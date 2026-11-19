package ru.vlyashuk.roadbuddy.data.remote.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.remote.mapper.RoadRequestDtoMapper
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
            snapshot.documents.mapNotNull { document ->
                runCatching {
                    document.data(RoadRequestDto.serializer())
                }.map(RoadRequestDtoMapper::toDomain).getOrNull()
            }
        }

    override fun getRequest(id: String): Flow<RoadRequest?> =
        collection.document(id).snapshots.map { snapshot ->
            if (!snapshot.exists) return@map null
            runCatching {
                snapshot.data(RoadRequestDto.serializer())
            }.map(RoadRequestDtoMapper::toDomain).getOrNull()
        }

    override suspend fun createRequest(request: RoadRequest) {
        val dto = RoadRequestDtoMapper.fromDomain(request)
        collection.document(dto.id).set(RoadRequestDto.serializer(), dto)
    }

    override suspend fun updateRequest(request: RoadRequest) {
        val dto = RoadRequestDtoMapper.fromDomain(request)
        collection.document(dto.id).set(RoadRequestDto.serializer(), dto)
    }

    override suspend fun deleteRequest(id: String) {
        collection.document(id).delete()
    }
}
