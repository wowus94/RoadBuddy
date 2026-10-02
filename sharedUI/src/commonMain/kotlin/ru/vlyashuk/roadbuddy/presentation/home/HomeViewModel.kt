package ru.vlyashuk.roadbuddy.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

data class HomeUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val getRequestsUseCase: GetRequestsUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            getRequestsUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, requests = result.value, error = null)
                            }
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = result.error.toMessage())
                            }
                        }
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            when (val result = authService.signOut()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _uiState.update { it.copy(error = result.error.toMessage()) }
                }
            }
        }
    }
}