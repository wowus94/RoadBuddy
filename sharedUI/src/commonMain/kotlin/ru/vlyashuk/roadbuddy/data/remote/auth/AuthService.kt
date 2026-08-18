package ru.vlyashuk.roadbuddy.data.remote.auth

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.AuthUser

interface AuthService {
    val currentUser: Flow<AuthUser?>
    suspend fun signUp(email: String, password: String): Result<AuthUser>
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signOut()
}