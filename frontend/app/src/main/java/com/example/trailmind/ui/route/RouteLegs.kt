package com.example.trailmind.ui.route

import com.example.trailmind.data.remote.dto.RoutePoint
import org.osmdroid.util.GeoPoint

enum class LegType { ROAD, TRAIL, FLIGHT }

data class Leg(
    val type: LegType,
    val points: List<GeoPoint>,
    val from: String? = null,   // airport names, used only for flights
    val to: String? = null
)

data class PlannedRoute(val legs: List<Leg>, val note: String)

// Total length in km of all legs (or of one type)
fun legKm(legs: List<Leg>, type: LegType? = null): Double =
    legs.filter { type == null || it.type == type }
        .sumOf { l -> l.points.zipWithNext().sumOf { (p, q) -> p.distanceToAsDouble(q) } } / 1000.0



object LegPlanner {
    // Checked by the team on Google Maps
    private val TRIBHUVAN = GeoPoint(27.7011926, 85.3540267)
    private val LUKLA = GeoPoint(27.6874162, 86.7318510)

    // Destinations that trekkers reach by flying to Lukla
    private val luklaSet = setOf("Everest Base Camp", "Sagarmatha National Park", "Gokyo Lakes")

    // Blocking network calls: run this on Dispatchers.IO
    fun plan(start: RoutePoint, end: RoutePoint): PlannedRoute {
        val a = GeoPoint(start.lat, start.lon)
        val b = GeoPoint(end.lat, end.lon)
        val legs = ArrayList<Leg>()
        var gaps = 0
        val trailhead = TrekWaypoints.trailhead(end.name)

        if (end.name in luklaSet && start.name !in luklaSet) {
            // 1. Road to the airport
            val road = RoadRouter.leg(a, TRIBHUVAN, "car")
            if (road != null) legs += Leg(LegType.ROAD, eraseLoops(road)) else gaps++
            // 2. Flight, airport to airport
            legs += Leg(LegType.FLIGHT, listOf(TRIBHUVAN, LUKLA), "Kathmandu Airport", "Lukla Airport")
            // 3. Trail from Lukla through the waypoints to the destination
            val chain = TrekWaypoints.before(end.name).ifEmpty { listOf(LUKLA) } + b
            chain.zipWithNext().forEach { (f, t) ->
                val p = RoadRouter.leg(f, t, "foot")
                if (p != null) legs += Leg(LegType.TRAIL, p) else gaps++
            }
        } else if (trailhead != null) {
            // Road to the trailhead, then trail. No foot fallback through the city.
            val road = RoadRouter.leg(a, trailhead, "car")
            if (road != null) legs += Leg(LegType.ROAD, eraseLoops(road)) else gaps++
            val chain = TrekWaypoints.before(end.name) + b
            chain.zipWithNext().forEach { (f, t) ->
                val p = RoadRouter.leg(f, t, "foot")
                if (p != null) legs += Leg(LegType.TRAIL, p) else gaps++
            }
        } else {
            // Everything else: a road if one exists, otherwise a short walking path
            val road = RoadRouter.leg(a, b, "car")
            if (road != null) {
                legs += Leg(LegType.ROAD, eraseLoops(road))
            } else {
                val km = a.distanceToAsDouble(b) / 1000
                val foot = if (km < 40) RoadRouter.leg(a, b, "foot") else null
                if (foot != null) legs += Leg(LegType.TRAIL, foot) else gaps++
            }
        }

        val note = when {
            legs.none { it.type != LegType.FLIGHT } -> "No mapped route found for this pair."
            gaps > 0 -> "Some sections have no mapped road or trail and are left blank."
            else -> ""
        }
        return PlannedRoute(legs, note)
    }
}