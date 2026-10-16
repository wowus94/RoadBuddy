package ru.vlyashuk.roadbuddy.presentation.create

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.dispatcher.ViewModelTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateViewModelTest : ViewModelTest() {

    private fun createViewModel(
        repository: FakeRoadRequestRepository
    ) = CreateViewModel(CreateRequestUseCase(repository))

    private fun CreateViewModel.fillValidForm() {
        onTitleChanged("Flat tire")
        onAuthorNameChanged("John")
        onContactChanged("+1234567890")
    }

    @Test
    fun createRequest_invalidForm_showsValidationErrorAndSkipsRepository() = runTest {
        val repository = FakeRoadRequestRepository()
        val viewModel = createViewModel(repository)

        viewModel.createRequest()
        advanceUntilIdle()

        assertEquals("Check the entered data", viewModel.uiState.value.error)
        assertEquals(0, repository.createCallCount)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun createRequest_success_setsSaved() = runTest {
        val repository = FakeRoadRequestRepository()
        val viewModel = createViewModel(repository)

        viewModel.fillValidForm()
        viewModel.createRequest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals(1, repository.createCallCount)
        assertEquals("Flat tire", repository.createdRequest?.title)
    }

    @Test
    fun createRequest_networkFailure_showsError() = runTest {
        val repository = FakeRoadRequestRepository().apply {
            createResult = AppResult.Failure(AppError.Network)
        }
        val viewModel = createViewModel(repository)

        viewModel.fillValidForm()
        viewModel.createRequest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
        assertEquals("No internet connection", state.error)
    }

    @Test
    fun onLocationSelected_savesCoordinates() = runTest {
        val viewModel = createViewModel(FakeRoadRequestRepository())

        viewModel.onLocationSelected(55.75, 37.61)

        assertEquals(55.75, viewModel.uiState.value.latitude)
        assertEquals(37.61, viewModel.uiState.value.longitude)
    }
}
