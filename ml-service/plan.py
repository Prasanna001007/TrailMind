import json
import time
from typing import List

import requests
from fastapi import APIRouter, HTTPException
from fastapi.encoders import jsonable_encoder
from pydantic import BaseModel

from chat import BASE, FAST_MODEL, SMART_MODEL, Msg, _api_key
from routing.destinations import DESTINATIONS

router = APIRouter()

CITIES = ["Kathmandu", "Chitwan", "Pokhara", "Lumbini", "Itahari", "Surkhet", "Nepalgunj"]
MONTHS = ["January", "February", "March", "April", "May", "June",
          "July", "August", "September", "October", "November", "December"]
NATIONALITIES = ["Domestic", "SAARC", "Foreign"]
TIERS = ["Budget", "Standard", "Luxury"]
TRANSPORTS = ["Public Bus", "Private Jeep", "Flight"]


class TurnRequest(BaseModel):
    messages: List[Msg]


def _gemini(prompt, model, as_json, max_tokens, temperature):
    key = _api_key()
    if not key:
        raise HTTPException(500, "GEMINI_API_KEY not found")
    cfg = {"temperature": temperature, "maxOutputTokens": max_tokens}
    if as_json:
        cfg["responseMimeType"] = "application/json"
    body = {"contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "generationConfig": cfg}
    # try the chosen model first, then the other one if it stays busy
    models = [model, FAST_MODEL if model == SMART_MODEL else SMART_MODEL]
    last = "unknown"
    for attempt in range(5):
        m = models[0] if attempt < 3 else models[1]
        try:
            r = requests.post(f"{BASE}/{m}:generateContent", json=body,
                              headers={"x-goog-api-key": key}, timeout=60)
        except requests.RequestException as e:
            last = str(e)
            time.sleep(3)
            continue
        if r.status_code == 200:
            try:
                return r.json()["candidates"][0]["content"]["parts"][0]["text"]
            except (KeyError, IndexError):
                return ""
        last = f"Gemini error {r.status_code}"
        if r.status_code in (429, 500, 503):
            time.sleep(4)
            continue
        break
    raise HTTPException(502, last)


def _extract(messages):
    transcript = "\n".join(
        f"{'User' if m.role == 'user' else 'Assistant'}: {m.text}" for m in messages
    )
    prompt = (
        "Read this conversation about a trip in Nepal and return ONLY a JSON object with "
        "these keys. Use null when the user has not said it. Never guess.\n"
        f'"destination": exactly one of {list(DESTINATIONS.keys())}, or null\n'
        '"other_place": a place the user wants that is NOT in that list, or null\n'
        f'"start_city": exactly one of {CITIES}, or null\n'
        '"days": integer or null\n'
        '"group_size": integer or null\n'
        f'"month": exactly one of {MONTHS}, or null\n'
        '"nationality": "Domestic" for a Nepali citizen, "SAARC" for India, Bangladesh, Bhutan, '
        'Sri Lanka, Pakistan, Maldives or Afghanistan, "Foreign" for anyone else, or null\n'
        '"tier": "Budget" for cheap or backpacker, "Standard" for mid-range, "Luxury", or null\n'
        '"transport": "Public Bus", "Private Jeep", "Flight", or null\n'
        '"intent": "budget" if the user describes a trip they want to take (destination, days, '
        'people or dates), even without saying plan or cost; '
        '"route" only if they ask for directions or a route; '
        '"hotels" only if they ask for hotels or lodging; '
        '"chat" for general questions such as weather, packing or advice\n\n'
        f"Conversation:\n{transcript}"
    )
    try:
        data = json.loads(_gemini(prompt, FAST_MODEL, True, 400, 0))
        return data if isinstance(data, dict) else {}
    except ValueError:
        return {}


def _pick(value, options):
    for o in options:
        if isinstance(value, str) and value.strip().lower() == o.lower():
            return o
    return None


def _num(value, lo, hi):
    try:
        n = int(value)
    except (TypeError, ValueError):
        return None
    return n if lo <= n <= hi else None


QUESTIONS = {
    "destination": "Where would you like to go? (for example Poon Hill, Everest Base Camp or Chitwan National Park)",
    "start_city": "Which city will you start from? (Kathmandu, Pokhara, Chitwan, Lumbini, Itahari, Surkhet or Nepalgunj)",
    "days": "How many days do you have?",
    "group_size": "How many people are travelling?",
    "month": "Which month are you travelling?",
    "nationality": "Are you a Nepali citizen, from a SAARC country, or from another country? (permit fees depend on this)",
}

# Details each feature needs. A new feature (for example weather) is one new entry here.
REQUIRED = {
    "budget": ["destination", "start_city", "days", "group_size", "month", "nationality"],
    "route": ["destination", "start_city"],
    "hotels": ["destination"],
}

@router.post("/chat/turn")
def chat_turn(req: TurnRequest):
    d = _extract(req.messages)
    f = {
        "destination": _pick(d.get("destination"), list(DESTINATIONS.keys())),
        "start_city": _pick(d.get("start_city"), CITIES),
        "days": _num(d.get("days"), 1, 30),
        "group_size": _num(d.get("group_size"), 1, 50),
        "month": _pick(d.get("month"), MONTHS),
        "nationality": _pick(d.get("nationality"), NATIONALITIES),
        "tier": _pick(d.get("tier"), TIERS),
        "transport": _pick(d.get("transport"), TRANSPORTS),
    }

    # Not about a trip feature: the app falls back to the normal chat
    intent = d.get("intent")
    if intent not in REQUIRED:
        return {"type": "chat"}

    # A place outside our 27: say so (written here, not by Gemini)
    if d.get("other_place") and not f["destination"]:
        return {"type": "limit", "message": (
            f"Sorry, I can only plan trips for these {len(DESTINATIONS)} places: "
            + ", ".join(DESTINATIONS.keys()) + ". Which of them would you like?")}

    missing = [k for k in REQUIRED[intent] if not f[k]]
    if missing:
        ask = missing[:3]
        if len(ask) == 1:
            text = QUESTIONS[ask[0]]
        else:
            text = "To plan this, I need a few details:\n" + "\n".join("• " + QUESTIONS[k] for k in ask)
        return {"type": "question", "message": text, "have": {k: v for k, v in f.items() if v}}

    # Route and hotels only need a few details, so they answer right away
    if intent in ("route", "hotels"):
        return {"type": intent, "inputs": f}

    # Everything known: the numbers come from the budget model
    from app import BudgetRequest, DEST_META, predict
    tier = f["tier"] or "Standard"
    transport = f["transport"] or "Public Bus"
    result = predict(BudgetRequest(
        starting_city=f["start_city"], destination=f["destination"],
        duration_days=f["days"], group_size=f["group_size"],
        travel_month=f["month"], nationality_category=f["nationality"],
        traveler_type="Solo" if f["group_size"] == 1 else "Group",
        accommodation_tier=tier, transport_mode=transport,
        use_guide=True, use_porter=True,
    ))
    altitude = DEST_META[f["destination"]][0]

    # Gemini writes the day-by-day text only: no prices, distances or names
    itinerary = _gemini(
        f"Write a short day-by-day itinerary for {f['days']} days in {f['destination']}, Nepal, "
        f"starting from {f['start_city']}, travelling in {f['month']}. "
        f"The highest point is about {altitude} m. "
        "Use one or two simple lines per day, labelled Day 1, Day 2 and so on. "
        "Do NOT mention prices, costs, distances, hotel names, flight times or weather. "
        + ("Include gradual acclimatization days because the altitude is high. " if altitude >= 3000 else "")
        + "Plain text only, no markdown symbols.",
        SMART_MODEL, False, 1500, 0.4)

    assumed = [f"{tier} accommodation", f"{transport} from {f['start_city']}", "a guide and a porter (where available)"]
    return {
        "type": "plan",
        "inputs": f,
        "assumed": assumed,
        "budget": jsonable_encoder(result),
        "altitude_m": altitude,
        "altitude_warning": "Altitude sickness is possible above 3,000 m. Climb gradually." if altitude >= 3000 else "",
        "itinerary": itinerary.replace("**", ""),
    }