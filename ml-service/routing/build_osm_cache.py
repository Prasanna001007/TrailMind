"""
TrailMind — OSM walking-graph cache builder (report 4.2.1, micro layer).
Downloads a pedestrian street network around each destination and saves it
as <slug>.graphml in routing/osm_cache/. Run ONCE with internet, then commit
the .graphml files so the app works fully offline afterward.

Resumable: already-cached files are skipped, so re-running only retries the
ones that failed. A short pause between requests avoids Overpass rate-limits.

Usage:  python -m routing.build_osm_cache
Deps:   pip install osmnx>=2.0
"""
import re
import time
from pathlib import Path

import osmnx as ox

from .destinations import DESTINATIONS

# faster/more permissive Overpass mirror than the default de server
ox.settings.overpass_url = "https://overpass.kumi.systems/api/interpreter"

CACHE_DIR = Path(__file__).parent / "osm_cache"
CACHE_DIR.mkdir(exist_ok=True)

DIST_M = 2000          # radius (m) of walking network around each point
NETWORK_TYPE = "walk"  # pedestrian paths, footways, steps
PAUSE_S = 5            # politeness delay between live downloads


def slugify(name: str) -> str:
    """'Poon Hill (Ghorepani)' -> 'poon_hill_ghorepani'"""
    s = re.sub(r"[^\w\s-]", "", name.lower())
    return re.sub(r"[\s/]+", "_", s).strip("_")


def build_one(name, lat, lon):
    """Returns 'cached' if already present, True if newly saved, False on fail."""
    slug = slugify(name)
    out = CACHE_DIR / f"{slug}.graphml"
    if out.exists():
        print(f"  skip (cached): {name}")
        return "cached"
    try:
        G = ox.graph.graph_from_point(
            (lat, lon), dist=DIST_M, network_type=NETWORK_TYPE,
            simplify=True, retain_all=False,
        )
        if G.number_of_nodes() == 0:
            print(f"  EMPTY graph: {name} — no walking network in radius")
            return False
        ox.io.save_graphml(G, out)
        print(f"  saved: {name}  ({G.number_of_nodes()} nodes) -> {out.name}")
        return True
    except Exception as e:
        print(f"  FAILED: {name} -> {type(e).__name__}")
        return False


def main():
    saved, cached, failed = 0, 0, []
    total = len(DESTINATIONS)
    print(f"Building OSM walking cache for {total} destinations "
          f"(dist={DIST_M}m, type={NETWORK_TYPE})\n")

    for name, (lat, lon, _arch, _alt) in DESTINATIONS.items():
        result = build_one(name, lat, lon)
        if result == "cached":
            cached += 1
        elif result:
            saved += 1
            time.sleep(PAUSE_S)   # only pause after a real network hit
        else:
            failed.append(name)
            time.sleep(PAUSE_S)

    print(f"\nDone. {saved} newly saved, {cached} already cached, "
          f"{len(failed)} failed.")
    if failed:
        print("\nStill failed (re-run to retry just these):")
        for n in failed:
            print(f"  - {n}")
        print("\nAny node without a .graphml uses the deterministic "
              "1.20–1.50 km fallback at runtime — expected for sparse areas.")


if __name__ == "__main__":
    main()