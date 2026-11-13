package ru.vlyashuk.roadbuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRepository {
    fun getRequests(): Flow<AppResult<List<RoadRequest>>>
    fun observeRequestsPage(
        pageSize: Int,
        offset: Int,
        query: RequestsPageQuery
    ): Flow<AppResult<RequestsPage>>
    suspend fun getRequestsPage(
        pageSize: Int,
        offset: Int,
        query: RequestsPageQuery
    ): AppResult<RequestsPage>
    fun getMyRequests(): Flow<AppResult<List<RoadRequest>>>
    fun getRequest(id: String): Flow<AppResult<RoadRequest?>>
    suspend fun refreshRequests(): AppResult<Unit>
    suspend fun refreshRequest(id: String): AppResult<Unit>
    suspend fun createRequest(request: RoadRequest): AppResult<Unit>
    suspend fun updateRequest(request: RoadRequest): AppResult<Unit>
    suspend fun updateRequestStatus(request: RoadRequest): AppResult<Unit>
    suspend fun deleteRequest(id: String): AppResult<Unit>
}
