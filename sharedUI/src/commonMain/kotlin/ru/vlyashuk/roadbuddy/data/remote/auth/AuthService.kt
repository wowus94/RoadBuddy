package ru.vlyashuk.roadbuddy.data.remote.auth

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser

interface AuthService {
    val currentUser: Flow<AuthUser?>
    suspend fun signUp(email: String, password: String): AppResult<AuthUser>
    suspend fun signIn(email: String, password: String): AppResult<AuthUser>
    suspend fun signOut(): AppResult<Unit>
}