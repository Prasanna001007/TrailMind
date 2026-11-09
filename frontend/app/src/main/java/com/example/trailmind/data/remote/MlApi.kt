package com.example.trailmind.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Streaming
import com.example.trailmind.data.remote.dto.RoutePoint
import com.example.trailmind.data.remote.dto.RouteRequest
import com.example.trailmind.data.remote.dto.RouteResponse
import retrofit2.http.GET
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import com.example.trailmind.data.remote.dto.ChatRequest
import com.example.trailmind.data.remote.dto.ChatResponse
import com.example.trailmind.data.remote.dto.HotelsResponse
import retrofit2.http.Query
import com.example.trailmind.data.remote.dto.TurnResponse

interface MlApi {
    @POST("predict")
    suspend fun predictBudget(@Body body: BudgetRequest): Response<BudgetResponse>

    @GET("destinations")
    suspend fun getDestinations(): Response<List<RoutePoint>>

    @POST("route")
    suspend fun findRoute(@Body body: RouteRequest): Response<RouteResponse>

    @POST("chat")
    suspend fun chat(@Body body: ChatRequest): Response<ChatResponse>

    @Streaming
    @POST("chat/stream")
    suspend fun chatStream(@Body body: ChatRequest): Response<ResponseBody>
    @POST("chat/turn")
    suspend fun chatTurn(@Body body: ChatRequest): Response<TurnResponse>

    @GET("hotels")
    suspend fun getHotels(
        @Query("name") name: String,
        @Query("radius_km") radiusKm: Int
    ): Response<HotelsResponse>


}

object MlClient {
    // 127.0.0.1 works with ADB Reverse for USB-connected physical phones
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val mlApi: MlApi = retrofit.create(MlApi::class.java)
}