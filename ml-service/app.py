"""
TrailMind — Budget Microservice (itemized + ML cross-check).
Computes a transparent itemized budget from data-derived component rates
(permit, guide, porter, lodging, meals) with optional guide/porter, adds a
starting-city transport estimate, and returns the RF model prediction as an
independent market-based cross-check. Altitude/restricted are derived from
the destination, not user input.
"""
import math
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

BASE = Path(__file__).parent
MODEL_PATH = BASE / "models" / "budget_model.joblib"
MEAL_MODEL_PATH = BASE / "models" / "meal_model.joblib"

USD_TO_NPR = 152.0

app = FastAPI(title="TrailMind Budget Service", version="3.0.0")
model = None
meal_model = None

# ── destination -> (altitude_m, is_restricted) : constant per destination ──
DEST_META = {
    "Amaltari Community Homestay": (180, 0), "Annapurna Circuit": (5416, 0),
    "Bandipur Trails": (1030, 0), "Bardia National Park": (415, 0),
    "Bhaktapur Durbar Square": (1401, 0), "Bhujung Village": (1600, 0),
    "Boudhanath": (1400, 0), "Champadevi / Chandragiri Trail": (2552, 0),
    "Chitwan National Park": (415, 0), "Dhampus / Australian Camp": (2060, 0),
    "Everest Base Camp": (5364, 0), "Ghalegaun": (2100, 0),
    "Gokyo Lakes": (4790, 0), "Janakpur (Janaki Mandir)": (70, 0),
    "Langtang Valley": (3870, 0), "Lumbini": (150, 0),
    "Manaslu Circuit": (5106, 1), "Mardi Himal": (4500, 0),
    "Muktinath": (3710, 0), "Nagarkot": (2175, 0),
    "Pashupatinath": (1400, 0), "Poon Hill (Ghorepani)": (3210, 0),
    "Sagarmatha National Park": (3440, 0), "Shivapuri National Park": (2732, 0),
    "Sirubari": (1700, 0), "Swayambhunath": (1400, 0),
    "Upper Mustang": (3840, 1),
}

# ── component cost rates (USD), derived from the dataset ──
COMPONENTS = {
    'Amaltari Community Homestay': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 15.0, 'meal_per_day': 12.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Annapurna Circuit': {'guide_per_day': 30.0, 'porter_per_day': 20.0, 'lodging_per_night': 8.0, 'meal_per_day': 22.0, 'has_porter': True, 'permit_by_tier': {'Budget': 20.77, 'Standard': 13.33, 'Luxury': 13.85}},
    'Bandipur Trails': {'guide_per_day': 20.0, 'porter_per_day': 0.0, 'lodging_per_night': 25.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Bardia National Park': {'guide_per_day': 25.0, 'porter_per_day': 0.0, 'lodging_per_night': 25.0, 'meal_per_day': 18.0, 'has_porter': False, 'permit_by_tier': {'Budget': 4.82, 'Standard': 6.38, 'Luxury': 6.38}},
    'Bhaktapur Durbar Square': {'guide_per_day': 20.0, 'porter_per_day': 0.0, 'lodging_per_night': 25.0, 'meal_per_day': 18.0, 'has_porter': False, 'permit_by_tier': {'Budget': 3.33, 'Standard': 5.94, 'Luxury': 5.62}},
    'Bhujung Village': {'guide_per_day': 20.0, 'porter_per_day': 0.0, 'lodging_per_night': 20.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Boudhanath': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 20.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 1.5, 'Standard': 1.5, 'Luxury': 1.5}},
    'Champadevi / Chandragiri Trail': {'guide_per_day': 20.0, 'porter_per_day': 15.0, 'lodging_per_night': 15.0, 'meal_per_day': 15.0, 'has_porter': True, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Chitwan National Park': {'guide_per_day': 25.0, 'porter_per_day': 0.0, 'lodging_per_night': 30.0, 'meal_per_day': 20.0, 'has_porter': False, 'permit_by_tier': {'Budget': 9.2, 'Standard': 9.83, 'Luxury': 6.5}},
    'Dhampus / Australian Camp': {'guide_per_day': 25.0, 'porter_per_day': 18.0, 'lodging_per_night': 12.0, 'meal_per_day': 18.0, 'has_porter': True, 'permit_by_tier': {'Budget': 15.0, 'Standard': 15.0, 'Luxury': 15.0}},
    'Everest Base Camp': {'guide_per_day': 35.0, 'porter_per_day': 25.0, 'lodging_per_night': 12.0, 'meal_per_day': 30.0, 'has_porter': True, 'permit_by_tier': {'Budget': 24.0, 'Standard': 27.27, 'Luxury': 32.14}},
    'Ghalegaun': {'guide_per_day': 18.0, 'porter_per_day': 0.0, 'lodging_per_night': 18.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Gokyo Lakes': {'guide_per_day': 35.0, 'porter_per_day': 25.0, 'lodging_per_night': 12.0, 'meal_per_day': 30.0, 'has_porter': True, 'permit_by_tier': {'Budget': 24.0, 'Standard': 27.27, 'Luxury': 32.14}},
    'Janakpur (Janaki Mandir)': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 18.0, 'meal_per_day': 12.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Langtang Valley': {'guide_per_day': 28.0, 'porter_per_day': 20.0, 'lodging_per_night': 10.0, 'meal_per_day': 22.0, 'has_porter': True, 'permit_by_tier': {'Budget': 20.0, 'Standard': 20.0, 'Luxury': 20.0}},
    'Lumbini': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 20.0, 'meal_per_day': 12.0, 'has_porter': False, 'permit_by_tier': {'Budget': 1.31, 'Standard': 1.31, 'Luxury': 1.31}},
    'Manaslu Circuit': {'guide_per_day': 35.0, 'porter_per_day': 22.0, 'lodging_per_night': 10.0, 'meal_per_day': 25.0, 'has_porter': True, 'permit_by_tier': {'Budget': 100.0, 'Standard': 100.0, 'Luxury': 100.0}},
    'Mardi Himal': {'guide_per_day': 28.0, 'porter_per_day': 20.0, 'lodging_per_night': 10.0, 'meal_per_day': 20.0, 'has_porter': True, 'permit_by_tier': {'Budget': 15.0, 'Standard': 15.0, 'Luxury': 15.0}},
    'Muktinath': {'guide_per_day': 25.0, 'porter_per_day': 0.0, 'lodging_per_night': 15.0, 'meal_per_day': 18.0, 'has_porter': False, 'permit_by_tier': {'Budget': 13.33, 'Standard': 13.33, 'Luxury': 13.33}},
    'Nagarkot': {'guide_per_day': 18.0, 'porter_per_day': 0.0, 'lodging_per_night': 25.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Pashupatinath': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 20.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 5.19, 'Standard': 5.0, 'Luxury': 5.19}},
    'Poon Hill (Ghorepani)': {'guide_per_day': 25.0, 'porter_per_day': 18.0, 'lodging_per_night': 10.0, 'meal_per_day': 18.0, 'has_porter': True, 'permit_by_tier': {'Budget': 15.0, 'Standard': 15.0, 'Luxury': 15.0}},
    'Sagarmatha National Park': {'guide_per_day': 35.0, 'porter_per_day': 25.0, 'lodging_per_night': 12.0, 'meal_per_day': 25.0, 'has_porter': True, 'permit_by_tier': {'Budget': 22.73, 'Standard': 22.73, 'Luxury': 22.73}},
    'Shivapuri National Park': {'guide_per_day': 18.0, 'porter_per_day': 0.0, 'lodging_per_night': 15.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 1.0, 'Standard': 1.0, 'Luxury': 1.0}},
    'Sirubari': {'guide_per_day': 18.0, 'porter_per_day': 0.0, 'lodging_per_night': 18.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Swayambhunath': {'guide_per_day': 15.0, 'porter_per_day': 0.0, 'lodging_per_night': 20.0, 'meal_per_day': 15.0, 'has_porter': False, 'permit_by_tier': {'Budget': 0.0, 'Standard': 0.0, 'Luxury': 0.0}},
    'Upper Mustang': {'guide_per_day': 35.0, 'porter_per_day': 22.0, 'lodging_per_night': 12.0, 'meal_per_day': 25.0, 'has_porter': True, 'permit_by_tier': {'Budget': 500.0, 'Standard': 500.0, 'Luxury': 500.0}},
}

# destination -> (lat, lon)
DEST_COORD = {
    "Amaltari Community Homestay": (27.5667, 84.1333), "Annapurna Circuit": (28.7960, 83.8210),
    "Bandipur Trails": (27.9333, 84.4167), "Bardia National Park": (28.3833, 81.5000),
    "Bhaktapur Durbar Square": (27.6722, 85.4278), "Bhujung Village": (28.2833, 84.2333),
    "Boudhanath": (27.7215, 85.3620), "Champadevi / Chandragiri Trail": (27.6333, 85.2333),
    "Chitwan National Park": (27.5291, 84.3542), "Dhampus / Australian Camp": (28.2833, 83.8333),
    "Everest Base Camp": (28.0026, 86.8528), "Ghalegaun": (28.3167, 84.3333),
    "Gokyo Lakes": (27.9500, 86.6944), "Janakpur (Janaki Mandir)": (26.7288, 85.9256),
    "Langtang Valley": (28.2131, 85.5216), "Lumbini": (27.4833, 83.2760),
    "Manaslu Circuit": (28.5500, 84.5600), "Mardi Himal": (28.4033, 83.9333),
    "Muktinath": (28.8167, 83.8719), "Nagarkot": (27.7172, 85.5208),
    "Pashupatinath": (27.7104, 85.3488), "Poon Hill (Ghorepani)": (28.4000, 83.6858),
    "Sagarmatha National Park": (27.8069, 86.7017), "Shivapuri National Park": (27.8110, 85.3920),
    "Sirubari": (27.9333, 83.7500), "Swayambhunath": (27.7149, 85.2903),
    "Upper Mustang": (29.1892, 83.9631),
}

CITIES = {
    "Kathmandu": (27.7172, 85.3240), "Chitwan": (27.5291, 84.3542),
    "Pokhara": (28.2096, 83.9856), "Lumbini": (27.4833, 83.2760),
    "Itahari": (26.6646, 87.2718), "Surkhet": (28.6006, 81.6339),
    "Nepalgunj": (28.0500, 81.6167),
}

DEST_GAMMA = {
    d: g for d, g in {
        **{k: 1.45 for k in ["Annapurna Circuit","Everest Base Camp","Langtang Valley",
              "Mardi Himal","Manaslu Circuit","Gokyo Lakes","Upper Mustang",
              "Bhujung Village","Champadevi / Chandragiri Trail",
              "Dhampus / Australian Camp","Sagarmatha National Park"]},
        **{k: 1.40 for k in ["Amaltari Community Homestay","Ghalegaun","Sirubari"]},
        **{k: 1.38 for k in ["Bhaktapur Durbar Square","Boudhanath","Janakpur (Janaki Mandir)",
              "Lumbini","Muktinath","Pashupatinath","Swayambhunath"]},
        **{k: 1.35 for k in ["Bandipur Trails","Nagarkot","Poon Hill (Ghorepani)",
              "Shivapuri National Park"]},
        **{k: 1.25 for k in ["Bardia National Park","Chitwan National Park"]},
    }.items()
}
EARTH_R = 6371.0


def haversine(a, b):
    la1, lo1, la2, lo2 = map(math.radians, [a[0], a[1], b[0], b[1]])
    h = math.sin((la2-la1)/2)**2 + math.cos(la1)*math.cos(la2)*math.sin((lo2-lo1)/2)**2
    return 2 * EARTH_R * math.asin(math.sqrt(h))


def transport_fare(km):
    if km <= 200:
        return km * 5.0
    return 200 * 5.0 + (km - 200) * 18.0


@app.on_event("startup")
def load():
    global model, meal_model
    if MODEL_PATH.exists():
        model = joblib.load(MODEL_PATH)
    if MEAL_MODEL_PATH.exists():
        meal_model = joblib.load(MEAL_MODEL_PATH)


class BudgetRequest(BaseModel):
    starting_city: str = Field(..., example="Pokhara")
    destination: str = Field(..., example="Everest Base Camp")
    duration_days: int = Field(..., ge=1, le=30, example=12)
    group_size: int = Field(..., ge=1, le=20, example=4)
    travel_month: str = Field(..., example="October")
    nationality_category: str = Field(..., example="Foreign")
    traveler_type: str = Field(..., example="Group")
    accommodation_tier: str = Field(..., example="Standard")
    transport_mode: str = Field(..., example="Flight")
    use_guide: bool = Field(True, example=True)
    use_porter: bool = Field(True, example=True)


class BudgetBreakdown(BaseModel):
    permit_npr: float
    guide_npr: float
    porter_npr: float
    lodging_npr: float
    meals_npr: float
    transport_npr: float


class BudgetResponse(BaseModel):
    breakdown: BudgetBreakdown
    itemized_total_npr: float          # sum of the breakdown (primary number)
    ml_estimate_npr: float             # independent RF cross-check
    per_person_npr: float
    start_to_dest_km: float
    starting_city: str
    porter_available: bool             # false for non-trek destinations
    currency: str = "NPR"


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}


@app.post("/predict", response_model=BudgetResponse)
def predict(req: BudgetRequest):
    if req.destination not in COMPONENTS:
        raise HTTPException(400, f"Unknown destination: {req.destination}")
    if req.starting_city not in CITIES:
        raise HTTPException(400, f"Unknown starting city: {req.starting_city}")

    c = COMPONENTS[req.destination]
    days, group = req.duration_days, req.group_size
    tier = req.accommodation_tier

    # ── itemized components (USD -> NPR) ──
    # permit + lodging + meals scale per person; guide + porter are shared per group
    permit_usd = c["permit_by_tier"].get(tier, 0.0) * group
    guide_usd = (c["guide_per_day"] * days) if req.use_guide else 0.0
    porter_usd = (c["porter_per_day"] * days) if (req.use_porter and c["has_porter"]) else 0.0
    lodging_usd = c["lodging_per_night"] * days * group
    meals_usd = c["meal_per_day"] * days * group

    permit = round(permit_usd * USD_TO_NPR, 2)
    guide = round(guide_usd * USD_TO_NPR, 2)
    porter = round(porter_usd * USD_TO_NPR, 2)
    lodging = round(lodging_usd * USD_TO_NPR, 2)
    meals = round(meals_usd * USD_TO_NPR, 2)

    # ── transport from starting city ──
    gamma = DEST_GAMMA.get(req.destination, 1.35)
    km = round(haversine(CITIES[req.starting_city], DEST_COORD[req.destination]) * gamma, 1)
    transport = round(transport_fare(km) * group, 2)

    itemized_total = round(permit + guide + porter + lodging + meals + transport, 2)

    # ── ML cross-check (RF model, independent estimate) ──
    ml_estimate = 0.0
    if model is not None:
        altitude, restricted = DEST_META[req.destination]
        row = pd.DataFrame([{
            "Duration_Days": days, "Group_Size": group,
            "Max_Altitude_Meters": altitude, "Is_Restricted_Area": restricted,
            "Destination": req.destination, "Travel_Month": req.travel_month.strip(),
            "Nationality_Category": req.nationality_category.strip(),
            "Traveler_Type": req.traveler_type.strip(),
            "Accommodation_Tier": tier, "Transport_Mode": req.transport_mode.strip(),
        }])
        ml_estimate = round(float(model.predict(row)[0]) + transport, 2)

    return BudgetResponse(
        breakdown=BudgetBreakdown(
            permit_npr=permit, guide_npr=guide, porter_npr=porter,
            lodging_npr=lodging, meals_npr=meals, transport_npr=transport,
        ),
        itemized_total_npr=itemized_total,
        ml_estimate_npr=ml_estimate,
        per_person_npr=round(itemized_total / max(group, 1), 2),
        start_to_dest_km=km,
        starting_city=req.starting_city,
        porter_available=c["has_porter"],
    )