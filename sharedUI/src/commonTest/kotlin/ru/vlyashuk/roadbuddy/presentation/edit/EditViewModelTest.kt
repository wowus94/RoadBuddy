package ru.vlyashuk.roadbuddy.presentation.edit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest : ViewModelTest() {

    private val request = RoadRequest(
        id = "req-1",
        title = "Need tow",
        description = "Engine dead",
        authorId = "uid",
        authorName = "John",
        contact = "+1234567890",
        latitude = 55.75,
        longitude = 37.61
    )

    private fun createViewModel(
        repository: FakeRoadRequestRepository,
        requestId: String = "req-1"
    ) = EditViewModel(
        getRequestByIdUseCase = GetRequestByIdUseCase(repository),
        updateRequestUseCase = UpdateRequestUseCase(repository),
        requestId = requestId
    )

    @Test
    fun loadRequest_existingRequest_fillsForm() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(listOf(request))

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Need tow", state.title)
        assertEquals("Engine dead", state.description)
        assertEquals(55.75, state.latitude)
        assertEquals(37.61, state.longitude)
    }

    @Test
    fun loadRequest_missingRequest_showsNotFound() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(emptyList())

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals("Request not found", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadRequest_failure_showsError() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitFailure(AppError.Network)

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals("No internet connection", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun updateRequest_success_savesChanges() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(listOf(request))
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onTitleChanged("Updated title")
        viewModel.onLocationSelected(59.0, 30.0)
        viewModel.updateRequest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isSaving)
        assertEquals(1, repository.updateCallCount)
        assertEquals("Updated title", repository.updatedRequest?.title)
        assertEquals(59.0, repository.updatedRequest?.latitude)
    }

    @Test
    fun updateRequest_failure_showsError() = runTest {
        val repository = FakeRoadRequestRepository().apply {
            updateResult = AppResult.Failure(AppError.Forbidden)
        }
        repository.emitSuccess(listOf(request))
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.updateRequest()
        advanceUntilIdle()

        assertEquals(
            "You don't have permission to perform this action",
            viewModel.uiState.value.error
        )
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
