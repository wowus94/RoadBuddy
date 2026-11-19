package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestResponse
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class GetResponsesUseCase(
    private val repository: RoadRequestRepository
) {
    operator fun invoke(requestId: String): Flow<AppResult<List<RequestResponse>>> =
        repository.observeResponses(requestId)
}
