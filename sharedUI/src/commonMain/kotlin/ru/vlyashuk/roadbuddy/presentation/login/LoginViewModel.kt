package ru.vlyashuk.roadbuddy.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.ResetPasswordUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignInUseCase
import ru.vlyashuk.roadbuddy.domain.validation.AuthValidators

class LoginViewModel(
    private val signInUseCase: SignInUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<AuthUser?> =
        getCurrentUserUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (!validate(state)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            signInUseCase(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        val emailError = AuthValidators.emailError(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            resetPasswordUseCase(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, resetEmailSent = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun validate(state: LoginUiState): Boolean {
        val emailError = AuthValidators.emailError(state.email)
        val passwordError = AuthValidators.passwordError(state.password)

        _uiState.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError
            )
        }
        return emailError == null && passwordError == null
    }
}