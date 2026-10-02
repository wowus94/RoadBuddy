package ru.vlyashuk.roadbuddy.data.remote.auth

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.error.UnauthorizedException
import ru.vlyashuk.roadbuddy.data.error.safeCall
import ru.vlyashuk.roadbuddy.domain.error.AppResult

class AuthServiceImpl : AuthService {

    private val auth = Firebase.auth

    override val currentUser: Flow<AuthUser?> =
        auth.authStateChanged.map { user -> user?.toDomain() }

    override suspend fun signUp(email: String, password: String): AppResult<AuthUser> = safeCall {
        val result = auth.createUserWithEmailAndPassword(email = email, password = password)
        result.user?.toDomain() ?: throw UnauthorizedException()
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> = safeCall {
        val result = auth.signInWithEmailAndPassword(email = email, password = password)
        result.user?.toDomain() ?: throw UnauthorizedException()
    }

    override suspend fun signOut(): AppResult<Unit> = safeCall {
        auth.signOut()
    }
}

private fun FirebaseUser.toDomain() = AuthUser(uid = uid, email = email)