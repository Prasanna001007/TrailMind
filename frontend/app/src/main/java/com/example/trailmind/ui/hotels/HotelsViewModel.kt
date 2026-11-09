package com.example.trailmind.ui.hotels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.MlClient
import com.example.trailmind.data.remote.dto.HotelsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HotelsUiState {
    data object Idle : HotelsUiState
    data object Loading : HotelsUiState
    data class Success(val data: HotelsResponse) : HotelsUiState
    data class Error(val message: String) : HotelsUiState
}

class HotelsViewModel : ViewModel() {
    private val _state = MutableStateFlow<HotelsUiState>(HotelsUiState.Idle)
    val state: StateFlow<HotelsUiState> = _state

    fun load(destination: String, radiusKm: Int) {
        _state.value = HotelsUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val res = MlClient.mlApi.getHotels(destination, radiusKm)
                val body = res.body()
                if (res.isSuccessful && body != null) HotelsUiState.Success(body)
                else HotelsUiState.Error("Server returned ${res.code()}")
            } catch (e: Exception) {
                HotelsUiState.Error(e.message ?: "Could not reach the server")
            }
        }
    }
}