package ru.vlyashuk.roadbuddy.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

class FakeRoadRequestRemoteDataSource : RoadRequestRemoteDataSource {

    private val requestsFlow = MutableStateFlow<List<RoadRequest>>(emptyList())

    var requestsError: Throwable? = null
    var createError: Throwable? = null
    var updateError: Throwable? = null

    var createdRequest: RoadRequest? = null
        private set
    var updatedRequest: RoadRequest? = null
        private set

    fun emit(requests: List<RoadRequest>) {
        requestsFlow.value = requests
    }

    override fun getRequests(): Flow<List<RoadRequest>> =
        requestsFlow.map { requests ->
            requestsError?.let { throw it }
            requests
        }

    override fun getRequest(id: String): Flow<RoadRequest?> =
        requestsFlow.map { requests ->
            requestsError?.let { throw it }
            requests.firstOrNull { it.id == id }
        }

    override suspend fun createRequest(request: RoadRequest) {
        createError?.let { throw it }
        createdRequest = request
    }

    override suspend fun updateRequest(request: RoadRequest) {
        updateError?.let { throw it }
        updatedRequest = request
    }

    override suspend fun deleteRequest(id: String) = Unit
}
