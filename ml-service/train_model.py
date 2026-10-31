"""
TrailMind — Budget Prediction Model (Section 4.2.3)
Trains a Random Forest regressor on the Trekking Feature Matrix and
serializes the fitted pipeline + metrics into models/.
Also trains a second model for meal cost per day.
"""
import json
from pathlib import Path

import numpy as np
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.ensemble import (
    RandomForestRegressor,
    GradientBoostingRegressor,
)
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

BASE = Path(__file__).parent
DATA_PATH = BASE / "data" / "TrailMind_Budget_Dataset_Coherent.xlsx"
SHEET = "Budget_Dataset"
MODELS_DIR = BASE / "models"
MODELS_DIR.mkdir(exist_ok=True)

NUMERIC = ["Duration_Days", "Group_Size", "Max_Altitude_Meters", "Is_Restricted_Area"]
CATEGORICAL = [
    "Destination", "Travel_Month", "Nationality_Category",
    "Traveler_Type", "Accommodation_Tier", "Transport_Mode",
]
TARGET = "Total_Cost_NPR"
MEAL_TARGET = "Meal_Cost_Per_Day_USD"


def load_data() -> pd.DataFrame:
    df = pd.read_excel(DATA_PATH, sheet_name=SHEET)
    # median imputation for any numeric gaps (report step 2); data is clean but keep it robust
    for col in NUMERIC:
        df[col] = df[col].fillna(df[col].median())
    for col in CATEGORICAL:
        df[col] = df[col].fillna(df[col].mode()[0]).astype(str).str.strip()
    return df


def build_pipeline(model) -> Pipeline:
    pre = ColumnTransformer(
        transformers=[("cat", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL)],
        remainder="passthrough",   # numeric columns pass through as-is
    )
    return Pipeline([("preprocess", pre), ("model", model)])


def evaluate(name, pipe, X_tr, X_te, y_tr, y_te, X, y):
    pipe.fit(X_tr, y_tr)
    pred = pipe.predict(X_te)
    rmse = float(np.sqrt(mean_squared_error(y_te, pred)))
    mae = float(mean_absolute_error(y_te, pred))
    r2 = float(r2_score(y_te, pred))
    cv_rmse = float(-cross_val_score(
        pipe, X, y, cv=5, scoring="neg_root_mean_squared_error"
    ).mean())
    print(f"{name:16s} RMSE={rmse:9.0f}  MAE={mae:8.0f}  R2={r2:.3f}  CV_RMSE={cv_rmse:.0f}")
    return {"model": name, "rmse": rmse, "mae": mae, "r2": r2, "cv_rmse": cv_rmse}


def main():
    df = load_data()
    X = df[NUMERIC + CATEGORICAL]
    y = df[TARGET]

    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

    # --- baseline comparison (report claims RF wins on average error) ---
    print("Baseline comparison")
    print("-" * 60)
    candidates = {
        "LinearRegression": LinearRegression(),
        "Ridge": Ridge(alpha=1.0),
        "GradientBoosting": GradientBoostingRegressor(random_state=42),
        "RandomForest": RandomForestRegressor(
            n_estimators=300, random_state=42, n_jobs=-1
        ),
    }
    results = [
        evaluate(name, build_pipeline(m), X_tr, X_te, y_tr, y_te, X, y)
        for name, m in candidates.items()
    ]

    # --- final model: Random Forest, refit on full data for deployment ---
    print("-" * 60)
    final = build_pipeline(
        RandomForestRegressor(
            n_estimators=300, max_depth=None, min_samples_leaf=1,
            random_state=42, n_jobs=-1,
        )
    )
    final.fit(X, y)  # train on all 945 rows for the shipped artifact

    joblib.dump(final, MODELS_DIR / "budget_model.joblib")

    # --- second model: meal cost per day (USD) ---
    y_meal = df[MEAL_TARGET]
    meal_model = build_pipeline(
        RandomForestRegressor(n_estimators=300, random_state=42, n_jobs=-1)
    )
    meal_model.fit(X, y_meal)   # same feature frame X
    joblib.dump(meal_model, MODELS_DIR / "meal_model.joblib")

    metrics = {
        "final_model": "RandomForest",
        "test_rmse": next(r["rmse"] for r in results if r["model"] == "RandomForest"),
        "test_mae": next(r["mae"] for r in results if r["model"] == "RandomForest"),
        "test_r2": next(r["r2"] for r in results if r["model"] == "RandomForest"),
        "baseline_comparison": results,
        "feature_schema": {"numeric": NUMERIC, "categorical": CATEGORICAL},
        "categorical_values": {c: sorted(df[c].unique().tolist()) for c in CATEGORICAL},
    }
    with open(MODELS_DIR / "metrics.json", "w") as f:
        json.dump(metrics, f, indent=2)

    print(f"\nSaved model      -> {MODELS_DIR / 'budget_model.joblib'}")
    print(f"Saved meal model -> {MODELS_DIR / 'meal_model.joblib'}")
    print(f"Saved metrics    -> {MODELS_DIR / 'metrics.json'}")


if __name__ == "__main__":
    main()