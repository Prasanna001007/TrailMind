"""
TrailMind — Graph-Based Route Optimization (report 4.2.1).
  1. A* point-to-point over a destination graph, Haversine heuristic
  2. Macro-transit distance = Haversine * gamma(archetype)
  3. Micro-exploration distance over cached OSM walking graphs (eq. 4.3)
  4. Multi-stop TSP: nearest-neighbour init + 2-opt local search

The micro layer loads per-node .graphml files from routing/osm_cache/
(built once by build_osm_cache.py) and falls back to a deterministic
1.20–1.50 km footprint when a graph is missing, empty, or disconnected.
"""
import heapq
import math
import random
import re
from itertools import combinations
from pathlib import Path

import networkx as nx
import osmnx as ox

from .destinations import DESTINATIONS, GAMMA

EARTH_R = 6371.0  # km

# ---------- micro-layer config ----------
_CACHE_DIR = Path(__file__).parent / "osm_cache"
_GRAPH_MEMO = {}                       # slug -> loaded graph (parse each file once)
_FALLBACK_MIN, _FALLBACK_MAX = 1.20, 1.50   # km, report's deterministic band


# ==========================================================
# 1. geodesic heuristic (report eq. 4.1)
# ==========================================================
def haversine(a, b):
    lat1, lon1 = math.radians(a[0]), math.radians(a[1])
    lat2, lon2 = math.radians(b[0]), math.radians(b[1])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 2 * EARTH_R * math.asin(math.sqrt(h))


# ==========================================================
# 2. macro-transit distance (report eq. 4.2)
# ==========================================================
def macro_distance(origin_name, target_name):
    o = DESTINATIONS[origin_name]
    t = DESTINATIONS[target_name]
    straight = haversine((o[0], o[1]), (t[0], t[1]))
    gamma = GAMMA[o[2]]          # curvature keyed on origin archetype
    return straight * gamma


# ==========================================================
# 3. micro-exploration distance (report eq. 4.3)
# ==========================================================
def _slugify(name):
    """'Poon Hill (Ghorepani)' -> 'poon_hill_ghorepani'"""
    s = re.sub(r"[^\w\s-]", "", name.lower())
    return re.sub(r"[\s/]+", "_", s).strip("_")


def _fallback(name):
    """Deterministic per-node footprint so runs are reproducible."""
    rng = random.Random(name)
    return round(rng.uniform(_FALLBACK_MIN, _FALLBACK_MAX), 2)


def micro_distance(node_name):
    """Localized pedestrian exploration distance over the cached OSM
    walking subgraph for this node. Falls back to the deterministic
    1.20–1.50 km footprint if the graph is missing/empty/disconnected."""
    slug = _slugify(node_name)

    if slug not in _GRAPH_MEMO:
        path = _CACHE_DIR / f"{slug}.graphml"
        if not path.exists():
            _GRAPH_MEMO[slug] = None
        else:
            try:
                _GRAPH_MEMO[slug] = ox.io.load_graphml(path)
            except Exception:
                _GRAPH_MEMO[slug] = None

    G = _GRAPH_MEMO[slug]
    if G is None or G.number_of_nodes() < 2:
        return _fallback(node_name)

    try:
        lat, lon = DESTINATIONS[node_name][0], DESTINATIONS[node_name][1]
        entry = ox.distance.nearest_nodes(G, lon, lat)
        # exit = farthest reachable node, modelling a real sightseeing loop
        lengths = nx.single_source_dijkstra_path_length(G, entry, weight="length")
        if len(lengths) < 2:
            return _fallback(node_name)
        exit_node = max(lengths, key=lengths.get)
        metres = nx.shortest_path_length(G, entry, exit_node, weight="length")
        km = metres / 1000.0
        return round(km, 2) if 0.05 <= km <= 15 else _fallback(node_name)
    except Exception:
        return _fallback(node_name)


# ==========================================================
# build a graph over destinations
# ==========================================================
def build_graph(nodes=None, k=6):
    """Connect each node to its k nearest neighbours by macro distance,
    then symmetrize and repair connectivity so A* always has a path."""
    names = nodes or list(DESTINATIONS.keys())
    graph = {n: {} for n in names}
    for a, b in combinations(names, 2):
        graph[a][b] = macro_distance(a, b)
        graph[b][a] = macro_distance(b, a)   # asymmetric: gamma differs by origin

    # prune to k nearest per node
    pruned = {n: {} for n in names}
    for n in names:
        nearest = sorted(graph[n].items(), key=lambda x: x[1])[:k]
        for nbr, d in nearest:
            pruned[n][nbr] = d
            pruned[nbr][n] = graph[nbr][n]   # symmetrize: keep it connected

    # safety net: bridge any disconnected components back into the main graph
    _ensure_connected(pruned, graph)
    return pruned


def _ensure_connected(pruned, full):
    def component(start):
        seen, stack = {start}, [start]
        while stack:
            cur = stack.pop()
            for nbr in pruned[cur]:
                if nbr not in seen:
                    seen.add(nbr)
                    stack.append(nbr)
        return seen

    names = list(pruned)
    main = component(names[0])
    while len(main) < len(names):
        outside = [n for n in names if n not in main]
        a, b, d = min(
            ((a, b, full[a][b]) for a in main for b in outside),
            key=lambda x: x[2],
        )
        pruned[a][b] = d
        pruned[b][a] = full[b][a]
        main = component(names[0])


# ==========================================================
# 1. A* search
# ==========================================================
def astar(graph, start, goal):
    coords = {n: (DESTINATIONS[n][0], DESTINATIONS[n][1]) for n in graph}
    open_set = [(0.0, start)]
    g = {start: 0.0}
    came = {}
    while open_set:
        _, cur = heapq.heappop(open_set)
        if cur == goal:
            path = [cur]
            while cur in came:
                cur = came[cur]
                path.append(cur)
            return path[::-1], g[goal]
        for nbr, w in graph[cur].items():
            tentative = g[cur] + w
            if tentative < g.get(nbr, math.inf):
                came[nbr] = cur
                g[nbr] = tentative
                h = haversine(coords[nbr], coords[goal])
                heapq.heappush(open_set, (tentative + h, nbr))
    return None, math.inf


# ==========================================================
# 4. multi-stop TSP
# ==========================================================
def _tour_length(order):
    return sum(macro_distance(order[i], order[i + 1]) for i in range(len(order) - 1))


def nearest_neighbour(stops, start=None):
    remaining = set(stops)
    cur = start or stops[0]
    remaining.discard(cur)
    tour = [cur]
    while remaining:
        nxt = min(remaining, key=lambda n: macro_distance(cur, n))
        tour.append(nxt)
        remaining.discard(nxt)
        cur = nxt
    return tour


def two_opt(tour):
    improved = True
    best = tour[:]
    while improved:
        improved = False
        for i in range(1, len(best) - 1):
            for j in range(i + 1, len(best)):
                if j - i == 1:
                    continue
                candidate = best[:i] + best[i:j + 1][::-1] + best[j + 1:]
                if _tour_length(candidate) < _tour_length(best):
                    best = candidate
                    improved = True
    return best


def optimize_multi_stop(stops, start=None):
    """Full pipeline: NN init -> 2-opt refine. Returns ordered stops,
    optimized length, and the pre-refinement NN length."""
    if len(stops) <= 2:
        return stops, _tour_length(stops), _tour_length(stops)
    nn = nearest_neighbour(stops, start)
    nn_len = _tour_length(nn)
    opt = two_opt(nn)
    return opt, _tour_length(opt), nn_len


def journey_distance(order):
    """Composite distance including micro footprints at each stop (eq. 4.4)."""
    macro = _tour_length(order)
    micro = sum(micro_distance(n) for n in order)
    return {"macro_km": round(macro, 1), "micro_km": round(micro, 1),
            "total_km": round(macro + micro, 1)}