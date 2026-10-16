package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.data.remote.auth.RemoteAuthDataSource
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource
) : AuthRepository {

    override val currentUser: Flow<AuthUser?>
        get() = remoteAuthDataSource.observeCurrentUser()

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<AuthUser> {
        return remoteAuthDataSource.signIn(email, password)
    }

    override suspend fun signUp(
        email: String,
        password: String
    ): Result<AuthUser> {
        return remoteAuthDataSource.signUp(email, password)
    }

    override suspend fun signOut() {
        remoteAuthDataSource.signOut()
    }
}