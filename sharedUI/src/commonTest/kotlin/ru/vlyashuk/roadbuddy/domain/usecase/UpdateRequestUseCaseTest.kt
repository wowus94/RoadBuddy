package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateRequestUseCaseTest {

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val repository = FakeRoadRequestRepository()
        val useCase = UpdateRequestUseCase(repository)
        val request = RoadRequest(id = "1", title = "Updated")

        val result = useCase(request)

        assertIs<AppResult.Success<Unit>>(result)
        assertEquals(1, repository.updateCallCount)
        assertEquals("Updated", repository.updatedRequest?.title)
    }

    @Test
    fun invoke_repositoryFailure_propagatesFailure() = runTest {
        val repository = FakeRoadRequestRepository().apply {
            updateResult = AppResult.Failure(AppError.Forbidden)
        }
        val useCase = UpdateRequestUseCase(repository)

        val result = useCase(RoadRequest(id = "1", title = "Updated"))

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Forbidden, result.error)
    }
}
