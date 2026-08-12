package ru.vlyashuk.roadbuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRepository {
    fun getRequests(): Flow<List<RoadRequest>>
    suspend fun createRequest(request: RoadRequest)
    suspend fun getRequest(id: String): RoadRequest?
}