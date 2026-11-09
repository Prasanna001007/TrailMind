package com.example.trailmind.data.remote.dto

// plan is set only for the trip plan card; it is never sent to the server
data class ChatMsg(val role: String, val text: String, val plan: TurnResponse? = null)
data class ChatRequest(val messages: List<ChatMsg>)
data class ChatResponse(val reply: String)

data class PlanInputs(
    val destination: String?,
    val start_city: String?,
    val days: Int?,
    val group_size: Int?,
    val month: String?,
    val nationality: String?,
    val tier: String?,
    val transport: String?
)

// Answer of POST /chat/turn. type is "chat", "question", "limit" or "plan".
data class TurnResponse(
    val type: String,
    val message: String?,
    val inputs: PlanInputs?,
    val assumed: List<String>?,
    val budget: BudgetResponse?,
    val altitude_m: Int?,
    val altitude_warning: String?,
    val itinerary: String?
)