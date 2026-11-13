package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class RefreshRequestUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.refreshRequest(id)
}
