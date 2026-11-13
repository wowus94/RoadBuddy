package ru.vlyashuk.roadbuddy.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class ObserveRequestsPageUseCase(
    private val repository: RoadRequestRepository
) {
    operator fun invoke(
        pageSize: Int,
        query: RequestsPageQuery
    ): Flow<AppResult<RequestsPage>> = repository.observeRequestsPage(pageSize, query)
}
