import json
import os
import time
from typing import List

import requests
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

router = APIRouter()

BASE = "https://generativelanguage.googleapis.com/v1beta/models"
FAST_MODEL = os.environ.get("GEMINI_FAST_MODEL", "gemini-3.5-flash-lite")
SMART_MODEL = os.environ.get("GEMINI_SMART_MODEL", "gemini-3.8-flash")

# Words that mean the user wants a fuller answer
DETAIL_WORDS = ("itinerary", "plan", "day-by-day", "days", "budget",
                "compare", "schedule", "packing", "detailed", "route")

SYSTEM_PROMPT = (
    "You are TrailMind, a travel assistant for tourists in Nepal. "
    "Help with trip planning: destinations, treks, day-by-day itineraries, "
    "seasons, permits, altitude safety, transport and rough costs in NPR. "
    "Be concise and practical. Warn about altitude sickness above 3,000 m. "
    "Say clearly when you are unsure, and never present prices or trail "
    "conditions as certain. Treks need a licensed guide and permits. "
    "Keep replies under 120 words unless the user asks for a full itinerary. "
    "Use short bullet points, and ask one clarifying question if details are missing. "
    "For exact trip costs, ask the user to describe the trip (where, how many days and people, which month) so the app can estimate it. "
    "For routes or hotels, ask them to say which place. "
    "Never invent exact prices. Give only rough ranges and say they are rough."
)


def _api_key() -> str:
    key = os.environ.get("GEMINI_API_KEY", "")
    if key:
        return key
    env_path = os.path.join(os.path.dirname(__file__), "..", "backend", ".env")
    try:
        with open(env_path, encoding="utf-8") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY="):
                    return line.split("=", 1)[1].strip().strip('"').strip("'")
    except OSError:
        pass
    return ""


class Msg(BaseModel):
    role: str   # "user" or "assistant"
    text: str


class ChatRequest(BaseModel):
    messages: List[Msg]
    detailed: bool = False   # the app can force the stronger model


def _pick(req: ChatRequest):
    """Returns (model, max output tokens)."""
    last = req.messages[-1].text.lower() if req.messages else ""
    if req.detailed or len(last.split()) > 25 or any(w in last for w in DETAIL_WORDS):
        return SMART_MODEL, 3000
    return FAST_MODEL, 800


def _call(req: ChatRequest, stream: bool):
    key = _api_key()
    if not key:
        raise HTTPException(500, "GEMINI_API_KEY not found")

    model, max_tokens = _pick(req)
    body = {
        "system_instruction": {"parts": [{"text": SYSTEM_PROMPT}]},
        "contents": [
            {"role": "user" if m.role == "user" else "model", "parts": [{"text": m.text}]}
            for m in req.messages
        ],
        "generationConfig": {"maxOutputTokens": max_tokens, "temperature": 0.7},
    }

    method = "streamGenerateContent?alt=sse" if stream else "generateContent"
    url = f"{BASE}/{model}:{method}"

    r = None
    for attempt in range(3):
        try:
            r = requests.post(
                url,
                json=body,
                headers={"x-goog-api-key": key},
                timeout=60,
                stream=stream
            )
        except requests.RequestException as e:
            if attempt == 2:
                raise HTTPException(502, f"Could not reach Gemini: {e}")
            time.sleep(2)
            continue

        if r.status_code in (429, 500, 503) and attempt < 2:
            r.close()
            time.sleep(3)
            continue

        break

    if r.status_code != 200 and model == SMART_MODEL and r.status_code in (429, 500, 503):
        print("[chat] Smart model unavailable. Falling back to Flash-Lite.")
        r.close()

        fallback_model = FAST_MODEL
        fallback_url = f"{BASE}/{fallback_model}:{method}"

        try:
            r = requests.post(
                fallback_url,
                json=body,
                headers={"x-goog-api-key": key},
                timeout=60,
                stream=stream
            )
        except requests.RequestException as e:
            raise HTTPException(502, f"Flash-Lite fallback failed: {e}")

        model = fallback_model

    if r.status_code != 200:
        raise HTTPException(502, f"Gemini error {r.status_code}: {r.text[:300]}")

    print(f"[chat] model={model}")
    return r, model

@router.post("/chat")
def chat(req: ChatRequest):
    r, model = _call(req, stream=False)
    try:
        reply = r.json()["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError):
        raise HTTPException(502, "Gemini returned no text")
    finally:
        r.close()

    return {"reply": reply, "model": model}

@router.post("/chat/stream")
def chat_stream(req: ChatRequest):
    r, model = _call(req, stream=True)
    r.encoding = "utf-8"

    def generate():
        with r:
            for line in r.iter_lines(decode_unicode=True):
                if line and line.startswith("data:"):
                    try:
                        data = json.loads(line[5:].strip())
                        for p in data["candidates"][0]["content"]["parts"]:
                            if "text" in p:
                                yield p["text"]
                    except (ValueError, KeyError, IndexError):
                        continue

    return StreamingResponse(generate(), media_type="text/plain; charset=utf-8",
                             headers={"X-Model": model})


@router.get("/chat/models")
def list_models():
    r = requests.get(BASE, headers={"x-goog-api-key": _api_key()}, timeout=30)
    if r.status_code != 200:
        raise HTTPException(502, r.text[:300])
    return [m["name"] for m in r.json().get("models", [])
            if "generateContent" in m.get("supportedGenerationMethods", [])]