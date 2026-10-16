package ru.vlyashuk.roadbuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.AuthUser

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(email: String, password: String): Result<AuthUser>
    suspend fun signOut()
}