"""Generate a sample TrailMind route map. Run: python -m routing.map_demo"""
from .mapview import render_route_map


def main():
    stops = [
        "Pashupatinath", "Boudhanath", "Bhaktapur Durbar Square",
        "Nagarkot", "Chitwan National Park", "Lumbini",
    ]
    order, out = render_route_map(stops, start="Pashupatinath", out="route_map.html")
    print("Optimized order:")
    for i, s in enumerate(order, 1):
        print(f"  {i}. {s}")
    print(f"\nMap saved -> {out}")
    print("Open it in your browser to see the OSM map with trail geometry.")


if __name__ == "__main__":
    main()