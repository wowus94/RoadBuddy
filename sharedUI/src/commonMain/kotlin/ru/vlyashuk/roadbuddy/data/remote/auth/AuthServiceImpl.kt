package ru.vlyashuk.roadbuddy.data.remote.auth

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.map

class AuthServiceImpl : AuthService {

    private val auth = Firebase.auth

    override val currentUser: Flow<AuthUser?> =
        auth.authStateChanged.map { user -> user?.toDomain() }

    override suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email = email, password = password)
        result.user?.toDomain() ?: error("No user returned")
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email = email, password = password)
        result.user?.toDomain() ?: error("No user returned")
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toDomain() = AuthUser(uid = uid, email = email)