package ru.vlyashuk.roadbuddy.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity

class FakeRoadRequestDao : RoadRequestDao {

    private val entities = MutableStateFlow<List<RoadRequestEntity>>(emptyList())

    var insertCallCount = 0
        private set
    var lastInserted: RoadRequestEntity? = null
        private set

    private fun filtered(type: String?, status: String?) = entities.value.filter { e ->
        (type == null || e.type == type) && (status == null || e.status == status)
    }

    override fun getAll(): Flow<List<RoadRequestEntity>> = entities

    override fun getById(id: String): Flow<RoadRequestEntity?> =
        entities.map { list -> list.firstOrNull { it.id == id } }

    override fun observePageDesc(
        type: String?,
        status: String?,
        limit: Int,
        offset: Int
    ): Flow<List<RoadRequestEntity>> = entities.map { list ->
        list.filter { e -> (type == null || e.type == type) && (status == null || e.status == status) }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun observePageAsc(
        type: String?,
        status: String?,
        limit: Int,
        offset: Int
    ): Flow<List<RoadRequestEntity>> = entities.map { list ->
        list.filter { e -> (type == null || e.type == type) && (status == null || e.status == status) }
            .sortedBy { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun observeCount(type: String?, status: String?): Flow<Int> =
        entities.map { filtered(type, status).size }

    override suspend fun countFiltered(type: String?, status: String?): Int =
        filtered(type, status).size

    override suspend fun insert(request: RoadRequestEntity) {
        insertCallCount++
        lastInserted = request
        entities.value = entities.value.filterNot { it.id == request.id } + request
    }

    override suspend fun upsertAll(requests: List<RoadRequestEntity>) {
        entities.value = entities.value.filterNot { old -> requests.any { it.id == old.id } } + requests
    }

    override suspend fun delete(request: RoadRequestEntity) {
        entities.value = entities.value.filterNot { it.id == request.id }
    }

    override suspend fun deleteById(id: String) {
        entities.value = entities.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        entities.value = emptyList()
    }
}
