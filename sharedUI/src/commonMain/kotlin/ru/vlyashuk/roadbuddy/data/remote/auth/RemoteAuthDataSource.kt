package ru.vlyashuk.roadbuddy.data.remote.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.domain.model.AuthUser

class RemoteAuthDataSource {

    private val auth = Firebase.auth

    fun observeCurrentUser(): Flow<AuthUser?> = auth.authStateChanged.map { it?.toDomain() }

    suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password)
        result.user?.toDomain() ?: error("No user returned")
    }

    suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password)
        result.user?.toDomain() ?: error("No user returned")
    }

    suspend fun signOut() = auth.signOut()

}

private fun FirebaseUser.toDomain() = AuthUser(uid = uid, email = email)
