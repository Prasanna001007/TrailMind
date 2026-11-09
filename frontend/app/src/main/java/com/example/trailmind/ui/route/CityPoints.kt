package com.example.trailmind.ui.route

import com.example.trailmind.data.remote.dto.RoutePoint

// Start points for the wizard's cities. Checked by the team on Google Maps.
object CityPoints {
    val all = listOf(
        RoutePoint("Kathmandu (city centre)", 27.7013392, 85.3135398, "City", 1400),
        RoutePoint("Pokhara (Lakeside)", 28.2059061, 83.9605682, "City", 822),
        RoutePoint("Chitwan (Bharatpur)", 27.6705399, 84.4384245, "City", 200),
        RoutePoint("Itahari", 26.6629071, 87.2744626, "City", 100),
        RoutePoint("Surkhet (Birendranagar)", 28.5951529, 81.6095701, "City", 720),
        RoutePoint("Nepalgunj", 28.0537862, 81.6158736, "City", 150)
    )

    fun byName(name: String): RoutePoint? = all.firstOrNull { it.name == name }
}