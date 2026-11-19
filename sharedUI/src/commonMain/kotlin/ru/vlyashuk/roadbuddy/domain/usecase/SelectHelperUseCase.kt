package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class SelectHelperUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(requestId: String, helperId: String): AppResult<Unit> =
        repository.selectHelper(requestId, helperId)
}
