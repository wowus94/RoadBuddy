package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import kotlin.random.Random
import kotlin.time.Clock

class RoadRequestRepositoryImpl(
    private val dao: RoadRequestDao
) : RoadRequestRepository {

    override fun getRequests(): Flow<List<RoadRequest>> =
        dao.getAll().map { list ->
            list.map { RoadRequestMapper.toDomain(it) }
        }

    override suspend fun createRequest(request: RoadRequest) {
        val newId = request.id.ifBlank { Random.nextInt(10000, 99999).toString() }
        val now = Clock.System.now()
        val entity = RoadRequestMapper.toEntity(
            request.copy(id = newId, createdAt = now, updatedAt = now)
        )
        dao.insert(entity)
    }

    override fun getRequest(id: String): Flow<RoadRequest?> =
        dao.getById(id).map { entity ->
            entity?.let { RoadRequestMapper.toDomain(it) }
        }

    override suspend fun updateRequest(request: RoadRequest) {
        val entity = RoadRequestMapper.toEntity(
            request.copy(updatedAt = Clock.System.now())
        )
        dao.insert(entity)
    }
}