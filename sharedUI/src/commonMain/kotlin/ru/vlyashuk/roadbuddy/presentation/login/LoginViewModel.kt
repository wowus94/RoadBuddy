package ru.vlyashuk.roadbuddy.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

class LoginViewModel(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun signIn() = auth { authService.signIn(it.email, it.password) }
    fun signUp() = auth { authService.signUp(it.email, it.password) }

    private fun auth(block: suspend (LoginUiState) -> AppResult<AuthUser>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = block(_uiState.value)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error.toMessage())
                }
            }
        }
    }
}