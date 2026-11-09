package com.example.trailmind.ui.route

import org.osmdroid.util.GeoPoint

// Trail stops placed BEFORE a trek destination, in walking order.
// The app routes a foot path between each consecutive pair.
// Coordinates were checked by the team on Google Maps.
object TrekWaypoints {
    private val trails = mapOf(
        "Everest Base Camp" to listOf(
            GeoPoint(27.6874162, 86.7318510),  // Lukla
            GeoPoint(27.7402045, 86.7125552),  // Phakding
            GeoPoint(27.8069848, 86.7137565),  // Namche Bazaar
            GeoPoint(27.8360986, 86.7649626),  // Tengboche
            GeoPoint(27.8890009, 86.8268250),  // Dingboche
            GeoPoint(27.9476366, 86.8102946),  // Lobuche
            GeoPoint(27.9806119, 86.8285872)   // Gorak Shep
        ),
        "Poon Hill (Ghorepani)" to listOf(
            GeoPoint(28.3021732, 83.7697344),  // Nayapul (trailhead)
            GeoPoint(28.4026642, 83.6999098)   // Ghorepani bazaar
        )
    )

    // Names in the same order as the points above (shown on the map)
    private val names = mapOf(
        "Everest Base Camp" to listOf(
            "Lukla", "Phakding", "Namche Bazaar", "Tengboche", "Dingboche", "Lobuche", "Gorak Shep"
        ),
        "Poon Hill (Ghorepani)" to listOf("Nayapul", "Ghorepani")
    )

    // Where the road ends and the trail begins (destinations reached by road first)
    private val trailheads = mapOf(
        "Poon Hill (Ghorepani)" to GeoPoint(28.3021732, 83.7697344)
    )

    fun before(destination: String): List<GeoPoint> = trails[destination] ?: emptyList()
    fun names(destination: String): List<String> = names[destination] ?: emptyList()
    fun trailhead(destination: String): GeoPoint? = trailheads[destination]
}