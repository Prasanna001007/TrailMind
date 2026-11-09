package com.example.trailmind.data.remote.dto

// Sent to POST /route. Both names must be destinations known to the ml-service.
data class RouteRequest(
    val start: String,
    val end: String
)

// One stop on a route. GET /destinations returns a list of these too.
data class RoutePoint(
    val name: String,
    val lat: Double,
    val lon: Double,
    val type: String,
    val altitude_m: Int
)

// Answer of POST /route: the stops in order, plus the distances.
data class RouteResponse(
    val path: List<RoutePoint>,
    val macro_km: Double,   // travel distance between the stops
    val micro_km: Double,   // estimated walking while exploring at the stops
    val total_km: Double
)