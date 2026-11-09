package com.example.trailmind.ui.route

// Carries the wizard's answers to the map screen, then is cleared
object TripSelection {
    var start: String = ""
    var end: String = ""
    // Set by the chat's "See all hotels" button, read once by the Hotels screen
    var hotelsDestination: String = ""
    // Set by the plan card's "Change details" button, read once by the wizard
    var prefill: com.example.trailmind.data.remote.dto.PlanInputs? = null

    // Wizard city -> start name shown in the map's Start dropdown
    private val cityToStart = mapOf(
        "Kathmandu" to "Kathmandu (city centre)",
        "Pokhara" to "Pokhara (Lakeside)",
        "Chitwan" to "Chitwan (Bharatpur)",
        "Lumbini" to "Lumbini",
        "Itahari" to "Itahari",
        "Surkhet" to "Surkhet (Birendranagar)",
        "Nepalgunj" to "Nepalgunj"
    )

    fun set(city: String, destination: String) {
        val place = cityToStart[city] ?: ""
        start = if (place == destination) "" else place   // never start = destination
        end = destination
    }

    fun clear() {
        start = ""
        end = ""
    }
}