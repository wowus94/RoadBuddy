package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class DeleteResponseUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(requestId: String, responseId: String): AppResult<Unit> =
        repository.deleteResponse(requestId, responseId)
}
