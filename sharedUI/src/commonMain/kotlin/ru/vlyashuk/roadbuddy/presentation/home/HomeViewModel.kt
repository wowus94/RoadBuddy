package ru.vlyashuk.roadbuddy.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignOutUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

data class HomeUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class User(val user: AuthUser?) : AuthUiState
}

@OptIn(InternalCoroutinesApi::class)
class HomeViewModel(
    private val getRequestsUseCase: GetRequestsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthUiState> =
        getCurrentUserUseCase()
            .map { AuthUiState.User(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AuthUiState.Loading
            )

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
                            _uiState.update { it.copy(isLoading = false, requests = result.value) }
                        }
                        is AppResult.Failure -> {
                            _uiState.update { it.copy(isLoading = false, error = result.error.toMessage()) }
                        }
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}