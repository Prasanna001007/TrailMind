package com.example.trailmind.data.repository

import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.AuthResponse
import com.example.trailmind.data.remote.dto.LoginRequest
import com.example.trailmind.data.remote.dto.RegisterRequest
import com.example.trailmind.data.remote.dto.UserDto
import com.example.trailmind.util.Resource
import com.google.gson.JsonParser
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthRepository(private val tokenStore: TokenStore) {

    suspend fun login(email: String, password: String): Resource<UserDto> =
        safeCall { ApiClient.authApi.login(LoginRequest(email, password)) }

    suspend fun register(
        name: String, email: String, password: String, nationality: String
    ): Resource<UserDto> =
        safeCall {
            ApiClient.authApi.register(RegisterRequest(name, email, password, nationality))
        }

    private suspend fun safeCall(
        call: suspend () -> Response<AuthResponse>
    ): Resource<UserDto> {
        return try {
            val res = call()
            val body = res.body()
            if (res.isSuccessful && body?.token != null && body.user != null) {
                tokenStore.saveToken(body.token)
                Resource.Success(body.user)
            } else {
                Resource.Error(parseError(res.errorBody()?.string()))
            }
        } catch (e: UnknownHostException) {
            Resource.Error("Cannot reach server. Is the backend running?")
        } catch (e: SocketTimeoutException) {
            Resource.Error("Connection timed out.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    private fun parseError(json: String?): String {
        if (json.isNullOrBlank()) return "Request failed"
        return try {
            JsonParser.parseString(json).asJsonObject
                .get("message")?.asString ?: "Request failed"
        } catch (e: Exception) {
            "Request failed"
        }
    }
}