package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.MlClient
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.util.Resource
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BudgetRepository {

    suspend fun predict(request: BudgetRequest): Resource<BudgetResponse> {
        return try {
            val res = MlClient.mlApi.predictBudget(request)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Resource.Success(body)
            } else {
                Resource.Error("Prediction failed (${res.code()})")
            }
        } catch (e: UnknownHostException) {
            Resource.Error("Cannot reach ML service. Is it running on port 8000?")
        } catch (e: ConnectException) {
            Resource.Error("Cannot reach ML service. Is it running on port 8000?")
        } catch (e: SocketTimeoutException) {
            Resource.Error("Prediction timed out.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
}