package ru.vlyashuk.roadbuddy.presentation.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.fakes.FakeAuthService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : ViewModelTest() {

    private fun createViewModel(
        authService: FakeAuthService
    ) = LoginViewModel(authService)

    @Test
    fun signIn_success_clearsLoadingAndError() = runTest {
        val auth = FakeAuthService()
        val viewModel = createViewModel(auth)

        viewModel.onEmailChange("test@mail.com")
        viewModel.onPasswordChange("password")
        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun signIn_invalidCredentials_showsError() = runTest {
        val auth = FakeAuthService().apply {
            signInResult = AppResult.Failure(AppError.InvalidCredentials)
        }
        val viewModel = createViewModel(auth)

        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Incorrect email or password", state.error)
    }

    @Test
    fun signIn_networkError_showsError() = runTest {
        val auth = FakeAuthService().apply {
            signInResult = AppResult.Failure(AppError.Network)
        }
        val viewModel = createViewModel(auth)

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals("No internet connection", viewModel.uiState.value.error)
    }

    @Test
    fun onEmailAndPasswordChange_updatesState() = runTest {
        val viewModel = createViewModel(FakeAuthService())

        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("secret")

        assertEquals("a@b.com", viewModel.uiState.value.email)
        assertEquals("secret", viewModel.uiState.value.password)
    }
}
