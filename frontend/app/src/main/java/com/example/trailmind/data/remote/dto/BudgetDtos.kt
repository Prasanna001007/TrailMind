package com.example.trailmind.data.remote.dto

data class BudgetRequest(
    val starting_city: String,
    val destination: String,
    val duration_days: Int,
    val group_size: Int,
    val travel_month: String,
    val nationality_category: String,
    val traveler_type: String,
    val accommodation_tier: String,
    val transport_mode: String,
    val use_guide: Boolean,
    val use_porter: Boolean
)

data class BudgetBreakdown(
    val permit_npr: Double,
    val guide_npr: Double,
    val porter_npr: Double,
    val lodging_npr: Double,
    val meals_npr: Double,
    val transport_npr: Double
)

data class BudgetResponse(
    val breakdown: BudgetBreakdown,
    val itemized_total_npr: Double,
    val ml_estimate_npr: Double,
    val per_person_npr: Double,
    val start_to_dest_km: Double,
    val starting_city: String,
    val porter_available: Boolean,
    val currency: String
)