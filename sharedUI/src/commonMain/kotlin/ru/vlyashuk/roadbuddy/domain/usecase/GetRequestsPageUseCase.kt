package ru.vlyashuk.roadbuddy.domain.usecase

import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository

class GetRequestsPageUseCase(
    private val repository: RoadRequestRepository
) {
    suspend operator fun invoke(
        pageSize: Int,
        offset: Int,
        query: RequestsPageQuery
    ): AppResult<RequestsPage> = repository.getRequestsPage(pageSize, offset, query)
}
