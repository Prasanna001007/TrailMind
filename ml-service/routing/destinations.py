"""
TrailMind destination coordinate registry.
Each node carries lat/lon, a tourism archetype (drives the macro curvature
factor gamma), and an approximate max altitude in metres.

NOTE: coordinates are representative points for each area/trailhead. Verify
key ones against a map before final submission.
"""

# archetype -> gamma (Archetype-Conditioned Curvature Factor), from report 4.2.1
GAMMA = {
    "Trekking": 1.45,
    "Homestay": 1.40,
    "Cultural": 1.38,
    "Hiking":   1.35,
    "Wildlife": 1.25,
}

DESTINATIONS = {
    # Cultural / Religious
    "Pashupatinath":                  (27.7104, 85.3488, "Cultural", 1400),
    "Boudhanath":                     (27.7215, 85.3620, "Cultural", 1400),
    "Swayambhunath":                  (27.7149, 85.2903, "Cultural", 1400),
    "Bhaktapur Durbar Square":        (27.6722, 85.4278, "Cultural", 1401),
    "Lumbini":                        (27.4833, 83.2760, "Cultural", 150),
    "Janakpur (Janaki Mandir)":       (26.7288, 85.9256, "Cultural", 70),
    # Trekking
    "Everest Base Camp":              (28.0026, 86.8528, "Trekking", 5364),
    "Annapurna Circuit":              (28.7960, 83.8210, "Trekking", 5416),
    "Langtang Valley":                (28.2131, 85.5216, "Trekking", 3870),
    "Mardi Himal":                    (28.4033, 83.9333, "Trekking", 4500),
    "Manaslu Circuit":                (28.5500, 84.5600, "Trekking", 5106),
    "Gokyo Lakes":                    (27.9500, 86.6944, "Trekking", 4790),
    "Upper Mustang":                  (29.1892, 83.9631, "Trekking", 3840),
    "Muktinath":                      (28.8167, 83.8719, "Trekking", 3710),
    # Hiking
    "Poon Hill (Ghorepani)":          (28.4000, 83.6858, "Hiking", 3210),
    "Nagarkot":                       (27.7172, 85.5208, "Hiking", 2175),
    "Shivapuri National Park":        (27.8110, 85.3920, "Hiking", 2732),
    "Bandipur Trails":                (27.9333, 84.4167, "Hiking", 1030),
    "Dhampus / Australian Camp":      (28.2833, 83.8333, "Hiking", 2060),
    "Champadevi / Chandragiri Trail": (27.6333, 85.2333, "Hiking", 2552),
    # Community Homestays
    "Sirubari":                       (27.9333, 83.7500, "Homestay", 1700),
    "Ghalegaun":                      (28.3167, 84.3333, "Homestay", 2100),
    "Bhujung Village":                (28.2833, 84.2333, "Homestay", 1600),
    "Amaltari Community Homestay":    (27.5667, 84.1333, "Homestay", 180),
    # Wildlife / Nature
    "Chitwan National Park":          (27.5291, 84.3542, "Wildlife", 415),
    "Bardia National Park":           (28.3833, 81.5000, "Wildlife", 415),
    "Sagarmatha National Park":       (27.8069, 86.7017, "Wildlife", 3440),
}