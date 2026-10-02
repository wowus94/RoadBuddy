package ru.vlyashuk.roadbuddy.presentation.session

import ru.vlyashuk.roadbuddy.domain.model.AuthUser

sealed interface SessionState {
    data object Loading : SessionState
    data class Authenticated(val user: AuthUser) : SessionState
    data object Unauthenticated : SessionState
}