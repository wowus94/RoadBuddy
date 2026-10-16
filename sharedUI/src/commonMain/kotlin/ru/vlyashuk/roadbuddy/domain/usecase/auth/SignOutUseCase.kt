package ru.vlyashuk.roadbuddy.domain.usecase.auth

import ru.vlyashuk.roadbuddy.domain.repository.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
    }
}