import json
import math
import os
import time

import requests
from fastapi import APIRouter, HTTPException

from routing.destinations import DESTINATIONS

router = APIRouter()

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
CACHE_FILE = os.path.join(DATA_DIR, "hotels_cache.json")
MANUAL_FILE = os.path.join(DATA_DIR, "hotels_manual.json")

ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
]
HEADERS = {"User-Agent": "TrailMind/1.0 (student project, Himalaya College of Engineering)"}
TYPES = "hotel|guest_house|hostel|motel|chalet|alpine_hut"


def _load(path, default):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


def _save_cache(cache):
    os.makedirs(DATA_DIR, exist_ok=True)
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False)


def _km(lat1, lon1, lat2, lon2):
    p = math.pi / 180
    a = (math.sin((lat2 - lat1) * p / 2) ** 2
         + math.cos(lat1 * p) * math.cos(lat2 * p) * math.sin((lon2 - lon1) * p / 2) ** 2)
    return 12742 * math.asin(math.sqrt(a))


def _overpass(lat, lon, radius_m):
    q = (f'[out:json][timeout:25];nwr["tourism"~"{TYPES}"]'
         f'(around:{radius_m},{lat},{lon});out center 200;')
    last = "unknown"
    for wait in (0, 5, 15):
        time.sleep(wait)
        for url in ENDPOINTS:
            try:
                r = requests.post(url, data={"data": q}, headers=HEADERS, timeout=40)
                if r.status_code == 200:
                    return r.json().get("elements", [])
                last = f"HTTP {r.status_code}"
            except requests.RequestException as e:
                last = str(e)
    raise RuntimeError(last)


def _convert(elements, lat, lon):
    out = []
    for e in elements:
        tags = e.get("tags", {})
        la = e.get("lat") or e.get("center", {}).get("lat")
        lo = e.get("lon") or e.get("center", {}).get("lon")
        if la is None or lo is None:
            continue
        out.append({
            "name": tags.get("name") or "Unnamed lodge",
            "type": tags.get("tourism", "lodging").replace("_", " "),
            "lat": la, "lon": lo,
            "distance_km": round(_km(lat, lon, la, lo), 2),
            "phone": tags.get("phone") or tags.get("contact:phone"),
            "website": tags.get("website") or tags.get("contact:website"),
            "stars": tags.get("stars"),
            "price_range": None, "checked_on": None,
            "source": "osm",
        })
    return out


def _merge_manual(hotels, name, lat, lon, radius_km):
    manual = [m for m in _load(MANUAL_FILE, []) if m.get("destination") == name]
    for m in manual:
        key = m["name"].strip().lower()
        hotels = [h for h in hotels if h["name"].strip().lower() != key]   # manual wins
        hotels.append({
            "name": m["name"], "type": m.get("type", "hotel"),
            "lat": m["lat"], "lon": m["lon"],
            "distance_km": round(_km(lat, lon, m["lat"], m["lon"]), 2),
            "phone": m.get("phone"), "website": m.get("website"), "stars": None,
            "price_range": m.get("price_range"), "checked_on": m.get("checked_on"),
            "source": "manual",
        })
    return [h for h in hotels if h["distance_km"] <= radius_km]


@router.get("/hotels")
def hotels(name: str, radius_km: int = 5, refresh: bool = False):
    if name not in DESTINATIONS:
        raise HTTPException(404, f"Unknown destination: {name}")
    lat, lon = DESTINATIONS[name][0], DESTINATIONS[name][1]
    key = f"{name}|{radius_km}"
    cache = _load(CACHE_FILE, {})
    state = "cache"

    if refresh or key not in cache:
        try:
            cache[key] = _convert(_overpass(lat, lon, radius_km * 1000), lat, lon)
            _save_cache(cache)
            state = "live"
        except RuntimeError as err:
            if key in cache:
                state = "cache_old"          # Overpass busy: use what we saved earlier
            else:
                cache[key] = []
                state = "unavailable"
                print(f"[hotels] Overpass failed for {name}: {err}")

    result = _merge_manual(cache[key], name, lat, lon, radius_km)
    def rank(h):
        if h["source"] == "manual":
            return 0                      # hand-checked by the group
        if h.get("phone") or h.get("website"):
            return 1                      # OSM, has contact details
        return 2

    result.sort(key=lambda h: (rank(h), h["distance_km"]))
    return {"destination": name, "radius_km": radius_km, "state": state,
            "count": len(result), "hotels": result[:80]}