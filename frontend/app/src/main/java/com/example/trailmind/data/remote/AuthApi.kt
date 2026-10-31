package com.example.trailmind.data.remote

import com.example.trailmind.data.remote.dto.AuthResponse
import com.example.trailmind.data.remote.dto.LoginRequest
import com.example.trailmind.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<AuthResponse>
}