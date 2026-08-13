package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class UpdateRequestUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(request: RoadRequest) = runCatching {
        repository.updateRequest(request)
    }
}