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
import kotlin.test.assertNull

class GetRequestByIdUseCaseTest {

    @Test
    fun invoke_existingId_returnsRequest() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(listOf(RoadRequest(id = "1", title = "Tow")))
        val useCase = GetRequestByIdUseCase(repository)

        val result = useCase("1").first()

        assertIs<AppResult.Success<RoadRequest?>>(result)
        assertEquals("Tow", result.value?.title)
    }

    @Test
    fun invoke_missingId_returnsNull() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitSuccess(emptyList())
        val useCase = GetRequestByIdUseCase(repository)

        val result = useCase("missing").first()

        assertIs<AppResult.Success<RoadRequest?>>(result)
        assertNull(result.value)
    }

    @Test
    fun invoke_failure_propagatesFailure() = runTest {
        val repository = FakeRoadRequestRepository()
        repository.emitFailure(AppError.Network)
        val useCase = GetRequestByIdUseCase(repository)

        val result = useCase("1").first()

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Network, result.error)
    }
}
