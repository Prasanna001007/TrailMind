package com.example.trailmind.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.data.repository.BudgetRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {

    private val repo = BudgetRepository()

    private val _uiState = MutableStateFlow<Resource<BudgetResponse>?>(null)
    val uiState: StateFlow<Resource<BudgetResponse>?> = _uiState

    fun predict(request: BudgetRequest) {
        _uiState.value = Resource.Loading
        viewModelScope.launch {
            _uiState.value = repo.predict(request)
        }
    }
}