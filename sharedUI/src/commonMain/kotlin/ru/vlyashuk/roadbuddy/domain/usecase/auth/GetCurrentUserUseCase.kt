package ru.vlyashuk.roadbuddy.domain.usecase.auth

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.repository.AuthRepository

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = authRepository.currentUser
}