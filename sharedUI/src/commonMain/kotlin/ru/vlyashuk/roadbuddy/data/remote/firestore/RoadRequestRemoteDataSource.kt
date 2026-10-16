package ru.vlyashuk.roadbuddy.data.remote.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRemoteDataSource {
    fun getRequests(): Flow<List<RoadRequest>>
    fun getRequest(id: String): Flow<RoadRequest?>
    suspend fun createRequest(request: RoadRequest)
    suspend fun updateRequest(request: RoadRequest)
    suspend fun deleteRequest(id: String)
}

class FirestoreRequestRemoteDataSource : RoadRequestRemoteDataSource {

    private val collection = Firebase.firestore.collection("requests")

    override fun getRequests(): Flow<List<RoadRequest>> =
        collection.snapshots.map { snapshot ->
            snapshot.documents.map { it.data(RoadRequest.serializer()) }
        }

    override fun getRequest(id: String): Flow<RoadRequest?> =
        collection.document(id).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data(RoadRequest.serializer()) else null
        }

    override suspend fun createRequest(request: RoadRequest) {
        collection.document(request.id).set(RoadRequest.serializer(), request)
    }

    override suspend fun updateRequest(request: RoadRequest) {
        collection.document(request.id).set(RoadRequest.serializer(), request)
    }

    override suspend fun deleteRequest(id: String) {
        collection.document(id).delete()
    }
}