"""
TrailMind — OSM map visualization (user-facing).
Renders cached walking graphs + destination markers + optimized route
onto a real Leaflet/OSM tile map, saved as a standalone HTML file.

Usage:
    from routing.mapview import render_route_map
    render_route_map(["Pashupatinath","Boudhanath","Nagarkot"], out="route.html")
"""
import re
from pathlib import Path

import folium
import networkx as nx
import osmnx as ox

from .destinations import DESTINATIONS
from .router import optimize_multi_stop, _slugify

_CACHE_DIR = Path(__file__).parent / "osm_cache"

# archetype -> marker colour (Folium's named colours)
_ARCH_COLOR = {
    "Cultural": "red",
    "Trekking": "darkgreen",
    "Hiking": "green",
    "Homestay": "orange",
    "Wildlife": "purple",
}


def _load_graph(name):
    path = _CACHE_DIR / f"{_slugify(name)}.graphml"
    if not path.exists():
        return None
    try:
        return ox.io.load_graphml(path)
    except Exception:
        return None


def _add_walking_graph(fmap, name):
    """Draw the cached OSM walking edges for one destination as thin lines."""
    G = _load_graph(name)
    if G is None or G.number_of_edges() == 0:
        return 0
    drawn = 0
    for u, v, data in G.edges(data=True):
        # each edge has a geometry (LineString) or just endpoints
        if "geometry" in data:
            coords = [(lat, lon) for lon, lat in data["geometry"].coords]
        else:
            coords = [
                (G.nodes[u]["y"], G.nodes[u]["x"]),
                (G.nodes[v]["y"], G.nodes[v]["x"]),
            ]
        folium.PolyLine(
            coords, color="#3388ff", weight=1.5, opacity=0.5
        ).add_to(fmap)
        drawn += 1
    return drawn


def render_route_map(stops, start=None, out="route_map.html", show_graphs=True):
    """Build an interactive OSM map for a set of destination stops.
    Returns the optimized order and the output path."""
    # 1. optimize the route order
    order, opt_km, nn_km = optimize_multi_stop(stops, start)

    # 2. center the map on the mean of the stops' coordinates
    lats = [DESTINATIONS[s][0] for s in order]
    lons = [DESTINATIONS[s][1] for s in order]
    center = (sum(lats) / len(lats), sum(lons) / len(lons))

    fmap = folium.Map(location=center, zoom_start=8, tiles="OpenStreetMap")

    # 3. draw each stop's cached walking network (the real OSM trail geometry)
    if show_graphs:
        for name in order:
            _add_walking_graph(fmap, name)

    # 4. plot markers in visit order, colour-coded by archetype
    for i, name in enumerate(order, 1):
        lat, lon, arch, alt = DESTINATIONS[name]
        folium.Marker(
            location=(lat, lon),
            tooltip=f"{i}. {name}",
            popup=folium.Popup(
                f"<b>{i}. {name}</b><br>{arch}<br>Alt: {alt} m", max_width=250
            ),
            icon=folium.Icon(color=_ARCH_COLOR.get(arch, "blue"), icon="info-sign"),
        ).add_to(fmap)

    # 5. draw the optimized route line between stops in order
    route_coords = [(DESTINATIONS[n][0], DESTINATIONS[n][1]) for n in order]
    folium.PolyLine(
        route_coords, color="crimson", weight=3, opacity=0.8,
        tooltip=f"Optimized route: {opt_km:.0f} km",
    ).add_to(fmap)

    # 6. simple legend
    legend = f"""
    <div style="position: fixed; bottom: 30px; left: 30px; z-index: 9999;
         background: white; padding: 10px 14px; border: 1px solid #999;
         border-radius: 6px; font-family: sans-serif; font-size: 13px;">
      <b>TrailMind Route</b><br>
      Stops: {len(order)} &nbsp;|&nbsp; Distance: {opt_km:.0f} km<br>
      <span style="color:crimson;">&#9644;</span> optimized route &nbsp;
      <span style="color:#3388ff;">&#9644;</span> walking paths
    </div>
    """
    fmap.get_root().html.add_child(folium.Element(legend))

    fmap.save(out)
    return order, out