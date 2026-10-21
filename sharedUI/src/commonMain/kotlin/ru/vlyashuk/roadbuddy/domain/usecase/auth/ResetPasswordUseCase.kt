package ru.vlyashuk.roadbuddy.domain.usecase.auth

import ru.vlyashuk.roadbuddy.domain.repository.AuthRepository

class ResetPasswordUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (!isValidEmail(trimmed)) return Result.failure(Throwable("Invalid email"))
        return authRepository.sendPasswordResetEmail(trimmed)
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() && email.contains("@")
}
