package ru.vlyashuk.roadbuddy.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService

class SessionViewModel(
    authService: AuthService
) : ViewModel() {

    val state: StateFlow<SessionState> =
        authService.currentUser
            .map { user ->
                if (user == null) {
                    SessionState.Unauthenticated
                } else {
                    SessionState.Authenticated(user)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SessionState.Loading
            )
}