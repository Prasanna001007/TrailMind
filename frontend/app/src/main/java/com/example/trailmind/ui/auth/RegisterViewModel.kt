package com.example.trailmind.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.data.remote.dto.UserDto
import com.example.trailmind.data.repository.AuthRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(TokenStore(app.applicationContext))

    private val _uiState = MutableStateFlow<Resource<UserDto>?>(null)
    val uiState: StateFlow<Resource<UserDto>?> = _uiState

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = Resource.Error("All fields are required")
            return
        }
        if (password.length < 8) {
            _uiState.value = Resource.Error("Password must be at least 8 characters")
            return
        }
        _uiState.value = Resource.Loading
        viewModelScope.launch {
            _uiState.value = repo.register(name.trim(), email.trim(), password, "NP")
        }
    }

    fun clearError() { if (_uiState.value is Resource.Error) _uiState.value = null }
}