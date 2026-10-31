"""Quick demo / smoke test for the TrailMind route optimizer.
Run: python demo.py
"""
from .router import build_graph, astar, optimize_multi_stop, journey_distance

def main():
    graph = build_graph(k=6)

    print("=" * 55)
    print("1. A* POINT-TO-POINT")
    print("=" * 55)
    for a, b in [("Pashupatinath", "Everest Base Camp"),
                 ("Lumbini", "Gokyo Lakes"),
                 ("Chitwan National Park", "Nagarkot")]:
        path, dist = astar(graph, a, b)
        print(f"{a} -> {b}")
        print(f"  {' -> '.join(path)}")
        print(f"  {dist:.0f} km\n")

    print("=" * 55)
    print("2. MULTI-STOP TSP (nearest-neighbour + 2-opt)")
    print("=" * 55)
    stops = ["Pashupatinath", "Chitwan National Park", "Lumbini",
             "Nagarkot", "Bhaktapur Durbar Square", "Boudhanath"]
    order, opt_len, nn_len = optimize_multi_stop(stops, start="Pashupatinath")
    print("optimized order:")
    for i, s in enumerate(order, 1):
        print(f"  {i}. {s}")
    print(f"\nnearest-neighbour: {nn_len:.0f} km")
    print(f"after 2-opt:       {opt_len:.0f} km")
    if nn_len > 0:
        print(f"improvement:       {(nn_len - opt_len) / nn_len * 100:.1f}%")
    print(f"composite:         {journey_distance(order)}")

if __name__ == "__main__":
    main()