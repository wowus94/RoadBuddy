package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import kotlin.random.Random
import kotlin.time.Clock

class RoadRequestRepositoryImpl(
    private val dao: RoadRequestDao,
    private val remote: RoadRequestRemoteDataSource
) : RoadRequestRepository {

    override fun getRequests(): Flow<List<RoadRequest>> = remote.getRequests()

    override suspend fun createRequest(request: RoadRequest) {
        val newId = request.id.ifBlank { Random.nextInt(10000, 99999).toString() }
        val now = Clock.System.now()
        val toSave = request.copy(id = newId, createdAt = now, updatedAt = now)
        remote.createRequest(toSave)
        dao.insert(RoadRequestMapper.toEntity(toSave))
    }

    override fun getRequest(id: String): Flow<RoadRequest?> = remote.getRequest(id)

    override suspend fun updateRequest(request: RoadRequest) {
        val toSave = request.copy(updatedAt = Clock.System.now())
        remote.updateRequest(toSave)
        dao.insert(RoadRequestMapper.toEntity(toSave))
    }
}