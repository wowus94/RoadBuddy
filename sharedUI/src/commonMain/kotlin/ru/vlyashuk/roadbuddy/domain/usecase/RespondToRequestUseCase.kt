package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class RespondToRequestUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(requestId: String, message: String): AppResult<Unit> =
        repository.respondToRequest(requestId, message)
}
