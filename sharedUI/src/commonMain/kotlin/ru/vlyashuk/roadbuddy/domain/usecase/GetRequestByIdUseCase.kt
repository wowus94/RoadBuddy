package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class GetRequestByIdUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(id: String) = repository.getRequest(id)
}