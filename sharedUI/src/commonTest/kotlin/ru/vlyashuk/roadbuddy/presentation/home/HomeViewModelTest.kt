package ru.vlyashuk.roadbuddy.presentation.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.fakes.FakeAuthService
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : ViewModelTest() {

    private fun createViewModel(
        repository: FakeRoadRequestRepository,
        authService: FakeAuthService = FakeAuthService()
    ) = HomeViewModel(GetRequestsUseCase(repository), authService)

    @Test
    fun loadRequests_success_showsRequests() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(listOf(RoadRequest(id = "1", title = "Tow")))

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.requests.size)
        assertNull(state.error)
    }

    @Test
    fun loadRequests_failure_showsError() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitFailure(AppError.Network)

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No internet connection", state.error)
    }

    @Test
    fun signOut_failure_showsError() = runTest {
        val repository = FakeRoadRequestRepository()
        val auth = FakeAuthService().apply {
            signOutResult = AppResult.Failure(AppError.Unknown)
        }
        val viewModel = createViewModel(repository, auth)
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(
            "Something went wrong. Please try again",
            viewModel.uiState.value.error
        )
    }
}
