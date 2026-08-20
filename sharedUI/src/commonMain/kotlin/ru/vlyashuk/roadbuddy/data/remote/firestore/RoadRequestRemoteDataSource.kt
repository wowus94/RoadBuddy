package ru.vlyashuk.roadbuddy.data.remote.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

class RoadRequestRemoteDataSource {

    private val collection = Firebase.firestore.collection("requests")

    fun getRequests(): Flow<List<RoadRequest>> =
        collection.snapshots.map { snapshot ->
            snapshot.documents.map { it.data(RoadRequest.serializer()) }
        }

    fun getRequest(id: String): Flow<RoadRequest?> =
        collection.document(id).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data(RoadRequest.serializer()) else null
        }

    suspend fun createRequest(request: RoadRequest) {
        collection.document(request.id).set(RoadRequest.serializer(), request)
    }

    suspend fun updateRequest(request: RoadRequest) {
        collection.document(request.id).set(RoadRequest.serializer(), request)
    }

    suspend fun deleteRequest(id: String) {
        collection.document(id).delete()
    }
}