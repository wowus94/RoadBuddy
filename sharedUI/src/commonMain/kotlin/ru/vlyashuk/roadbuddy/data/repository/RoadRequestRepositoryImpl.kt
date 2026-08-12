package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import kotlin.random.Random
import kotlin.time.Clock

class RoadRequestRepositoryImpl : RoadRequestRepository {

    private val _requests = MutableStateFlow<List<RoadRequest>>(emptyList())
    override fun getRequests(): Flow<List<RoadRequest>> = _requests.asStateFlow()

    override suspend fun createRequest(request: RoadRequest) {
        _requests.update { current ->
            val newId = request.id.ifBlank { Random.nextInt(10000, 99999).toString() }
            val now = Clock.System.now()
            current + request.copy(
                id = newId,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    override suspend fun getRequest(id: String): RoadRequest? = _requests.value.find { it.id == id }
}