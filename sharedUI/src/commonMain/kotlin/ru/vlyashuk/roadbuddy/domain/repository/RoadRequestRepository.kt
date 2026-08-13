package ru.vlyashuk.roadbuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRepository {
    fun getRequests(): Flow<List<RoadRequest>>
    suspend fun createRequest(request: RoadRequest)
    fun getRequest(id: String): Flow<RoadRequest?>
    suspend fun updateRequest(request: RoadRequest)
}