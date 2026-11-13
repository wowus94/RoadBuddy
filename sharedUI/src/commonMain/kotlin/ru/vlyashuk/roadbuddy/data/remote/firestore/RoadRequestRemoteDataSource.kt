package ru.vlyashuk.roadbuddy.data.remote.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

interface RoadRequestRemoteDataSource {
    fun getRequests(): Flow<List<RoadRequest>>
    fun getRequestsByAuthor(authorId: String): Flow<List<RoadRequest>>
    fun getRequest(id: String): Flow<RoadRequest?>
    fun observeRequestsPage(
        pageSize: Int,
        query: RequestsPageQuery
    ): Flow<RequestsPage>
    suspend fun getRequestsPage(
        pageSize: Int,
        cursorId: String?,
        query: RequestsPageQuery
    ): RequestsPage
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

    override fun getRequestsByAuthor(authorId: String): Flow<List<RoadRequest>> =
        collection.where { "authorId" equalTo authorId }
            .snapshots.map { snapshot ->
                snapshot.documents.map { it.data(RoadRequest.serializer()) }
            }

    override fun observeRequestsPage(
        pageSize: Int,
        query: RequestsPageQuery
    ): Flow<RequestsPage> =
        baseQuery(query)
            .limit(pageSize)
            .snapshots
            .map { it.toRequestsPage(pageSize) }

    override suspend fun getRequestsPage(
        pageSize: Int,
        cursorId: String?,
        query: RequestsPageQuery
    ): RequestsPage {
        var firestoreQuery = baseQuery(query).limit(pageSize)

        if (cursorId != null) {
            val cursorSnapshot = collection.document(cursorId).get()
            firestoreQuery = firestoreQuery.startAfter(cursorSnapshot)
        }

        return firestoreQuery.get().toRequestsPage(pageSize)
    }

    private fun baseQuery(query: RequestsPageQuery): Query {
        var firestoreQuery: Query = collection
        if (query.type != null) {
            firestoreQuery = firestoreQuery.where { "type" equalTo query.type }
        }
        if (query.status != null) {
            firestoreQuery = firestoreQuery.where { "status" equalTo query.status }
        }
        return firestoreQuery.orderBy(
            "createdAt",
            if (query.sortDescending) Direction.DESCENDING else Direction.ASCENDING
        )
    }

    private fun QuerySnapshot.toRequestsPage(pageSize: Int): RequestsPage {
        val documents = this.documents
        return RequestsPage(
            requests = documents.map { it.data(RoadRequest.serializer()) },
            nextCursor = documents.lastOrNull()?.id,
            hasMore = documents.size == pageSize
        )
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