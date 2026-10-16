package ru.vlyashuk.roadbuddy.presentation.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.fakes.FakeAuthService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest : ViewModelTest() {

    @Test
    fun noUser_emitsUnauthenticated() = runTest {
        val viewModel = SessionViewModel(FakeAuthService())
        advanceUntilIdle()

        assertIs<SessionState.Unauthenticated>(viewModel.state.value)
    }

    @Test
    fun signedInUser_emitsAuthenticated() = runTest {
        val auth = FakeAuthService().apply {
            signInResult = AppResult.Success(
                AuthUser(uid = "uid", email = "a@b.com")
            )
        }
        val viewModel = SessionViewModel(auth)

        auth.signIn("a@b.com", "password")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<SessionState.Authenticated>(state)
        assertEquals("uid", state.user.uid)
    }
}
