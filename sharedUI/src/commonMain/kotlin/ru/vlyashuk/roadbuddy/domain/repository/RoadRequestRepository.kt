package ru.vlyashuk.roadbuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRepository {
    fun getRequests(): Flow<AppResult<List<RoadRequest>>>
    suspend fun createRequest(request: RoadRequest): AppResult<Unit>
    fun getRequest(id: String): Flow<AppResult<RoadRequest?>>
    suspend fun updateRequest(request: RoadRequest): AppResult<Unit>
}