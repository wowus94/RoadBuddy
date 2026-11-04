package ru.vlyashuk.roadbuddy.presentation.login

import ru.vlyashuk.roadbuddy.domain.validation.AuthFieldError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val resetEmailSent: Boolean = false
)
