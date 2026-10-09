package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateRequestUseCaseTest {

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val repository = FakeRoadRequestRepository()
        val useCase = CreateRequestUseCase(repository)
        val request = RoadRequest(id = "", title = "Tow")

        val result = useCase(request)

        assertIs<AppResult.Success<Unit>>(result)
        assertEquals(1, repository.createCallCount)
        assertEquals("Tow", repository.createdRequest?.title)
    }

    @Test
    fun invoke_repositoryFailure_propagatesFailure() = runTest {
        val repository = FakeRoadRequestRepository().apply {
            createResult = AppResult.Failure(AppError.Network)
        }
        val useCase = CreateRequestUseCase(repository)

        val result = useCase(RoadRequest(id = "", title = "Tow"))

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Network, result.error)
    }
}
