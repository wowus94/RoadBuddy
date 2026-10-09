package ru.vlyashuk.roadbuddy.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class FakeRoadRequestRepository : RoadRequestRepository {

    private val requestsFlow =
        MutableStateFlow<AppResult<List<RoadRequest>>>(AppResult.Success(emptyList()))

    var createResult: AppResult<Unit> = AppResult.Success(Unit)
    var updateResult: AppResult<Unit> = AppResult.Success(Unit)

    var createdRequest: RoadRequest? = null
        private set
    var updatedRequest: RoadRequest? = null
        private set
    var createCallCount = 0
        private set
    var updateCallCount = 0
        private set

    fun emitSuccess(requests: List<RoadRequest>) {
        requestsFlow.value = AppResult.Success(requests)
    }

    fun emitFailure(error: AppError) {
        requestsFlow.value = AppResult.Failure(error)
    }

    override fun getRequests(): Flow<AppResult<List<RoadRequest>>> = requestsFlow

    override suspend fun createRequest(request: RoadRequest): AppResult<Unit> {
        createCallCount++
        createdRequest = request
        return createResult
    }

    override fun getRequest(id: String): Flow<AppResult<RoadRequest?>> =
        requestsFlow.map { result ->
            when (result) {
                is AppResult.Success ->
                    AppResult.Success(result.value.firstOrNull { it.id == id })
                is AppResult.Failure -> result
            }
        }

    override suspend fun updateRequest(request: RoadRequest): AppResult<Unit> {
        updateCallCount++
        updatedRequest = request
        return updateResult
    }
}