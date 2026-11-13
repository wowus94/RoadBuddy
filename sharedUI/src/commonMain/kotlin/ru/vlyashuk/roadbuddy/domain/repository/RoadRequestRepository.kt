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
        query: RequestsPageQuery
    ): Flow<AppResult<RequestsPage>>
    suspend fun getRequestsPage(
        pageSize: Int,
        cursorId: String?,
        query: RequestsPageQuery
    ): AppResult<RequestsPage>
    fun getMyRequests(): Flow<AppResult<List<RoadRequest>>>
    suspend fun createRequest(request: RoadRequest): AppResult<Unit>
    fun getRequest(id: String): Flow<AppResult<RoadRequest?>>
    suspend fun updateRequest(request: RoadRequest): AppResult<Unit>
    suspend fun updateRequestStatus(request: RoadRequest): AppResult<Unit>
    suspend fun deleteRequest(id: String): AppResult<Unit>
}