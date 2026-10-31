package com.example.trailmind.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val nationality: String = "NP"
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: UserDto?
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val nationality: String
)