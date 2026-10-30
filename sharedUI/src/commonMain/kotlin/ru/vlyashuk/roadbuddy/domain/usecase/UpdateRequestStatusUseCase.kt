package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class UpdateRequestStatusUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(request: RoadRequest) = repository.updateRequestStatus(request)
}
