package com.example.trailmind.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.MlClient
import com.example.trailmind.data.remote.dto.RoutePoint
import com.example.trailmind.data.remote.dto.RouteRequest
import com.example.trailmind.data.remote.dto.RouteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

// What the route part of the screen can show
sealed interface RouteUiState {
    data object Idle : RouteUiState
    data object Loading : RouteUiState
    data class Success(val data: RouteResponse) : RouteUiState
    data class Error(val message: String) : RouteUiState
}

class RouteViewModel : ViewModel() {

    // The list of places the user can pick from (GET /destinations)
    private val _destinations = MutableStateFlow<List<RoutePoint>>(emptyList())
    val destinations: StateFlow<List<RoutePoint>> = _destinations

    private val _destinationsError = MutableStateFlow<String?>(null)
    val destinationsError: StateFlow<String?> = _destinationsError

    // The result of the last route request (POST /route)
    private val _state = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val state: StateFlow<RouteUiState> = _state

    init {
        loadDestinations()
    }

    fun loadDestinations() {
        _destinationsError.value = null
        viewModelScope.launch {
            try {
                val res = MlClient.mlApi.getDestinations()
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    _destinations.value = body
                } else {
                    _destinationsError.value = "Server returned ${res.code()}"
                }
            } catch (e: Exception) {
                _destinationsError.value = e.message ?: "Could not reach the server"
            }
        }
    }

    fun findRoute(start: String, end: String) {
        _state.value = RouteUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val res = MlClient.mlApi.findRoute(RouteRequest(start, end))
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    RouteUiState.Success(body)
                } else {
                    RouteUiState.Error(res.errorBody()?.string() ?: "Server returned ${res.code()}")
                }
            } catch (e: Exception) {
                RouteUiState.Error(e.message ?: "Could not reach the server")
            }
        }
    }

    // Clears the last route (the ViewModel outlives the screen)
    fun reset() {
        _state.value = RouteUiState.Idle
    }

    // For city start points the server does not know: build the result here.
    // The map draws the real legs itself, so only the two endpoints are needed.
    fun planFrom(start: RoutePoint, end: RoutePoint) {
        val km = GeoPoint(start.lat, start.lon).distanceToAsDouble(GeoPoint(end.lat, end.lon)) / 1000
        _state.value = RouteUiState.Success(
            RouteResponse(path = listOf(start, end), macro_km = km, micro_km = 0.0, total_km = km)
        )
    }
}