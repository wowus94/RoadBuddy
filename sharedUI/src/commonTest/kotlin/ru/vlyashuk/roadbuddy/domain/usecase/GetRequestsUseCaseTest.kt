package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetRequestsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryFlow() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(listOf(RoadRequest(id = "1", title = "Tow")))
        val useCase = GetRequestsUseCase(repository)

        val result = useCase().first()

        assertIs<AppResult.Success<List<RoadRequest>>>(result)
        assertEquals(1, result.value.size)
    }

    @Test
    fun invoke_failure_propagatesFailure() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitFailure(AppError.Network)
        val useCase = GetRequestsUseCase(repository)

        val result = useCase().first()

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Network, result.error)
    }
}
