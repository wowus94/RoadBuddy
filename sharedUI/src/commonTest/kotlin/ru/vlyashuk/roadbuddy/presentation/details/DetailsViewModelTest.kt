package ru.vlyashuk.roadbuddy.presentation.details

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.fakes.FakeAuthService
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest : ViewModelTest() {

    private fun createViewModel(
        repository: FakeRoadRequestRepository,
        authService: FakeAuthService,
        requestId: String = "req-1"
    ) = DetailsViewModel(
        getRequestByIdUseCase = GetRequestByIdUseCase(repository),
        authService = authService,
        requestId = requestId
    )

    private suspend fun signedInAuth(uid: String = "uid") = FakeAuthService().apply {
        signInResult = ru.vlyashuk.roadbuddy.domain.error.AppResult.Success(
            AuthUser(uid = uid, email = "a@b.com")
        )
        signIn("a@b.com", "password")
    }

    @Test
    fun request_byCurrentUser_setsIsOwner() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(
            listOf(RoadRequest(id = "req-1", title = "Tow", authorId = "uid"))
        )
        val auth = signedInAuth()

        val viewModel = createViewModel(repository, auth)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOwner)
        assertEquals("Tow", viewModel.uiState.value.request?.title)
    }

    @Test
    fun request_byOtherUser_isNotOwner() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(
            listOf(RoadRequest(id = "req-1", title = "Tow", authorId = "other"))
        )
        val auth = signedInAuth()

        val viewModel = createViewModel(repository, auth)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOwner)
    }

    @Test
    fun request_missing_showsNotFound() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(emptyList())

        val viewModel = createViewModel(repository, FakeAuthService())
        advanceUntilIdle()

        assertEquals("Request not found", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun request_failure_showsError() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitFailure(AppError.Forbidden)

        val viewModel = createViewModel(repository, FakeAuthService())
        advanceUntilIdle()

        assertEquals(
            "You don't have permission to perform this action",
            viewModel.uiState.value.error
        )
    }
}
