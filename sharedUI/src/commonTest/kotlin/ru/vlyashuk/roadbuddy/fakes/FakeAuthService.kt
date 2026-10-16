package ru.vlyashuk.roadbuddy.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser

class FakeAuthService : AuthService {
    private val userFlow = MutableStateFlow<AuthUser?>(null)
    override val currentUser: Flow<AuthUser?> = userFlow

    override suspend fun signUp(
        email: String, password: String
    ): AppResult<AuthUser> = AppResult.Success(
        AuthUser(uid = "uid", email = "test@mail.com")
    )

    var signInResult: AppResult<AuthUser> =
        AppResult.Success(AuthUser(uid = "uid", email = "test@mail.com"))
    var signOutResult: AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun signIn(email: String, password: String) = signInResult.also {
        if (it is AppResult.Success) userFlow.value = it.value
    }

    override suspend fun signOut(): AppResult<Unit> = signOutResult.also {
        if (it is AppResult.Success) userFlow.value = null
    }
}