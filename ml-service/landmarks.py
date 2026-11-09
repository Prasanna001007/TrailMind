import json
import os
import time

import requests
from fastapi import APIRouter

from hotels import ENDPOINTS, HEADERS
from routing.destinations import DESTINATIONS

router = APIRouter()

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
CACHE_FILE = os.path.join(DATA_DIR, "landmarks_cache.json")

RADIUS_M = 20000
NEPAL_BBOX = "26.3,80.0,30.5,88.3"

# Trails longer than the radius around the destination need extra centres
EXTRA_CENTERS = {
    "Everest Base Camp": [(27.6874, 86.7319), (27.8070, 86.7138)],   # Lukla, Namche
}

# Kinds are listed in the order they win when a place has several tags
CAPS = {"airport": 60, "town": 80, "peak": 60, "pass": 40, "viewpoint": 40,
        "monastery": 40, "village": 120, "temple": 40}


def _load():
    try:
        with open(CACHE_FILE, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {}


def _save(cache):
    os.makedirs(DATA_DIR, exist_ok=True)
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False)


def _kind(t):
    if t.get("aeroway") == "aerodrome":
        return "airport"
    if t.get("place") in ("city", "town"):
        return "town"
    if t.get("place") in ("village", "hamlet"):
        return "village"
    if t.get("natural") == "peak":
        return "peak"
    if t.get("mountain_pass") == "yes":
        return "pass"
    if t.get("tourism") == "viewpoint":
        return "viewpoint"
    if "monastery" in (t.get("amenity"), t.get("historic"), t.get("building")):
        return "monastery"
    if t.get("amenity") == "place_of_worship":
        return "temple"
    return None


def _convert(elements):
    seen, out, counts = set(), [], {}
    for e in elements:
        t = e.get("tags", {})
        name = t.get("name:en") or t.get("name")
        kind = _kind(t)
        la = e.get("lat") or e.get("center", {}).get("lat")
        lo = e.get("lon") or e.get("center", {}).get("lon")
        if not name or not kind or la is None or lo is None:
            continue
        key = (name.lower(), kind)
        if key in seen:
            continue
        seen.add(key)
        if counts.get(kind, 0) >= CAPS[kind]:
            continue
        counts[kind] = counts.get(kind, 0) + 1
        out.append({"name": name, "kind": kind, "lat": la, "lon": lo})
    return out


def _run(query):
    last = "unknown"
    for wait in (0, 5, 15):
        time.sleep(wait)
        for url in ENDPOINTS:
            try:
                r = requests.post(url, data={"data": query}, headers=HEADERS, timeout=90)
                if r.status_code == 200:
                    return r.json().get("elements", [])
                last = f"HTTP {r.status_code}"
            except requests.RequestException as e:
                last = str(e)
    raise RuntimeError(last)


def _towns_query():
    b = NEPAL_BBOX
    return (f'[out:json][timeout:80];('
            f'nwr["place"~"^(city|town)$"]["name"]({b});'
            f'nwr["aeroway"="aerodrome"]["name"]({b});'
            f');out center 1000;')


def _detail_query(lat, lon):
    a = f"(around:{RADIUS_M},{lat},{lon})"
    return ('[out:json][timeout:80];('
            f'nwr["place"~"^(village|hamlet|town|city)$"]["name"]{a};'
            f'nwr["natural"="peak"]["name"]{a};'
            f'nwr["mountain_pass"="yes"]["name"]{a};'
            f'nwr["tourism"="viewpoint"]["name"]{a};'
            f'nwr["amenity"~"^(place_of_worship|monastery)$"]["name"]{a};'
            f'nwr["historic"="monastery"]["name"]{a};'
            f'nwr["aeroway"="aerodrome"]["name"]{a};'
            ');out center 1500;')


@router.get("/landmarks")
def landmarks(names: str = "", refresh: bool = False):
    """names = comma separated destination names (the route's two ends)."""
    cache = _load()
    areas = [("nepal_towns", _towns_query())]
    for n in [x.strip() for x in names.split(",") if x.strip()]:
        if n not in DESTINATIONS:
            continue
        areas.append((f"dest|{n}", _detail_query(DESTINATIONS[n][0], DESTINATIONS[n][1])))
        for i, (la, lo) in enumerate(EXTRA_CENTERS.get(n, [])):
            areas.append((f"dest|{n}|extra{i}", _detail_query(la, lo)))

    state, missing, merged, seen = "cache", [], [], set()
    for key, query in areas:
        if refresh or key not in cache:
            try:
                cache[key] = _convert(_run(query))
                _save(cache)
                state = "live"
            except RuntimeError as err:
                print(f"[landmarks] {key} failed: {err}")
                missing.append(key)
                continue
        for item in cache[key]:
            k = (item["name"].lower(), item["kind"])
            if k not in seen:
                seen.add(k)
                merged.append(item)

    if missing:
        state = "partial"
    return {"state": state, "missing": missing, "count": len(merged), "landmarks": merged}