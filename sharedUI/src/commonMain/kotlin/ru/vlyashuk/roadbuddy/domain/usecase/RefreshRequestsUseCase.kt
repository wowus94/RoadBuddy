package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class RefreshRequestsUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshRequests()
}
