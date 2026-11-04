package ru.vlyashuk.roadbuddy.domain.validation

enum class AuthFieldError {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    CONFIRM_PASSWORD_REQUIRED,
    PASSWORDS_DO_NOT_MATCH
}

object AuthValidators {

    private const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(email: String): AuthFieldError? = when {
        email.isBlank() -> AuthFieldError.EMAIL_REQUIRED
        !EMAIL_REGEX.matches(email.trim()) -> AuthFieldError.EMAIL_INVALID
        else -> null
    }

    fun passwordError(password: String): AuthFieldError? = when {
        password.isBlank() -> AuthFieldError.PASSWORD_REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> AuthFieldError.PASSWORD_TOO_SHORT
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): AuthFieldError? = when {
        confirm.isBlank() -> AuthFieldError.CONFIRM_PASSWORD_REQUIRED
        confirm != password -> AuthFieldError.PASSWORDS_DO_NOT_MATCH
        else -> null
    }
}