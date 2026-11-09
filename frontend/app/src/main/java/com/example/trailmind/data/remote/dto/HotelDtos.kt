package com.example.trailmind.data.remote.dto

data class Hotel(
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double,
    val distance_km: Double,
    val phone: String?,
    val website: String?,
    val stars: String?,
    val price_range: String?,
    val checked_on: String?,
    val source: String
)

data class HotelsResponse(
    val destination: String,
    val radius_km: Int,
    val state: String,
    val count: Int,
    val hotels: List<Hotel>
)