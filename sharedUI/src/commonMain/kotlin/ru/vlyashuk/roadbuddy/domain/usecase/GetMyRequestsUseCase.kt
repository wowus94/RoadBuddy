package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class GetMyRequestsUseCase(
    private val repository: RoadRequestRepository
) {
    operator fun invoke(): Flow<AppResult<List<RoadRequest>>> = repository.getMyRequests()
}
