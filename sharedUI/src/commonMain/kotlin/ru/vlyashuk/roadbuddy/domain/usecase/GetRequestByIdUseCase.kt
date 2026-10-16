package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.error.AppResult

class GetRequestByIdUseCase(
    private val repository: RoadRequestRepository
) {
    operator fun invoke(id: String): Flow<AppResult<RoadRequest?>> = repository.getRequest(id)
}