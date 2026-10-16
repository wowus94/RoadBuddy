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

    override fun getAll(): Flow<List<RoadRequestEntity>> = entities

    override fun getById(id: String): Flow<RoadRequestEntity?> =
        entities.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insert(request: RoadRequestEntity) {
        insertCallCount++
        lastInserted = request
        entities.value = entities.value.filterNot { it.id == request.id } + request
    }

    override suspend fun delete(request: RoadRequestEntity) {
        entities.value = entities.value.filterNot { it.id == request.id }
    }

    override suspend fun deleteAll() {
        entities.value = emptyList()
    }
}
