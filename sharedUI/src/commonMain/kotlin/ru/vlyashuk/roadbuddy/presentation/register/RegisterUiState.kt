package ru.vlyashuk.roadbuddy.presentation.register

import ru.vlyashuk.roadbuddy.domain.validation.AuthFieldError

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val confirmPasswordError: AuthFieldError? = null
)