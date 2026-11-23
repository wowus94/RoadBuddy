package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.error.toAppError
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestActor
import ru.vlyashuk.roadbuddy.domain.model.RequestResponse
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestStatusTransitions
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import kotlin.random.Random
import kotlin.time.Clock

class RoadRequestRepositoryImpl(
    private val dao: RoadRequestDao,
    private val remote: RoadRequestRemoteDataSource,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : RoadRequestRepository {

    override fun getRequests(): Flow<AppResult<List<RoadRequest>>> = dao.getAll()
        .map<List<RoadRequestEntity>, AppResult<List<RoadRequest>>> {
            AppResult.Success(it.map(RoadRequestMapper::toDomain))
        }
        .catch { emit(AppResult.Failure(AppError.Unknown)) }

    override fun observeRequestsPage(
        pageSize: Int,
        offset: Int,
        query: RequestsPageQuery
    ): Flow<AppResult<RequestsPage>> {
        val type = query.type?.name
        val status = query.status?.name
        val pageFlow = if (query.sortDescending) {
            dao.observePageDesc(type, status, pageSize, offset)
        } else {
            dao.observePageAsc(type, status, pageSize, offset)
        }
        return combine(pageFlow, dao.observeCount(type, status)) { page, total ->
            val nextOffset = offset + page.size
            RequestsPage(
                requests = page.map(RoadRequestMapper::toDomain),
                nextOffset = nextOffset,
                hasMore = nextOffset < total
            )
        }.map<RequestsPage, AppResult<RequestsPage>> { AppResult.Success(it) }
            .catch { emit(AppResult.Failure(AppError.Unknown)) }
    }

    override suspend fun getRequestsPage(
        pageSize: Int,
        offset: Int,
        query: RequestsPageQuery
    ): AppResult<RequestsPage> = try {
        val type = query.type?.name
        val status = query.status?.name
        val pageFlow = if (query.sortDescending) {
            dao.observePageDesc(type, status, pageSize, offset)
        } else {
            dao.observePageAsc(type, status, pageSize, offset)
        }
        val page = pageFlow.first()
        val total = dao.countFiltered(type, status)
        val nextOffset = offset + page.size
        AppResult.Success(
            RequestsPage(
                requests = page.map(RoadRequestMapper::toDomain),
                nextOffset = nextOffset,
                hasMore = nextOffset < total
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMyRequests(): Flow<AppResult<List<RoadRequest>>> =
        getCurrentUserUseCase()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf<AppResult<List<RoadRequest>>>(AppResult.Success(emptyList()))
                } else {
                    dao.getAll()
                        .map<List<RoadRequestEntity>, AppResult<List<RoadRequest>>> { entities ->
                            AppResult.Success(
                                entities
                                    .filter { it.authorId == user.uid }
                                    .map(RoadRequestMapper::toDomain)
                            )
                        }
                }
            }
            .catch { emit(AppResult.Failure(AppError.Unknown)) }

    override fun getRequest(id: String): Flow<AppResult<RoadRequest?>> =
        dao.getById(id)
            .map<RoadRequestEntity?, AppResult<RoadRequest?>> {
                AppResult.Success(it?.let(RoadRequestMapper::toDomain))
            }
            .catch { emit(AppResult.Failure(AppError.Unknown)) }

    override suspend fun refreshRequests(): AppResult<Unit> = try {
        val remoteRequests = remote.getRequests().first()
        dao.replaceAll(remoteRequests.map(RoadRequestMapper::toEntity))
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun refreshRequest(id: String): AppResult<Unit> = try {
        val request = remote.getRequest(id).first()
        if (request == null) {
            dao.deleteById(id)
        } else {
            dao.insert(RoadRequestMapper.toEntity(request))
        }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun createRequest(request: RoadRequest): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)

        val newId = request.id.ifBlank { Random.nextInt(10000, 99999).toString() }
        val now = Clock.System.now()
        val toSave = request.copy(
            id = newId,
            authorId = currentUid,
            helperId = "",
            status = RequestStatus.OPEN,
            createdAt = now,
            updatedAt = now
        )
        remote.createRequest(toSave)
        cacheRequest(toSave)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun updateRequest(request: RoadRequest): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)

        val remoteRequest = remote.getRequest(request.id).first()
            ?: return AppResult.Failure(AppError.NotFound)

        if (remoteRequest.authorId != currentUid) {
            return AppResult.Failure(AppError.Forbidden)
        }

        val toSave = remoteRequest.copy(
            title = request.title,
            description = request.description,
            type = request.type,
            authorName = request.authorName,
            contact = request.contact,
            latitude = request.latitude,
            longitude = request.longitude,
            geohash = request.geohash,
            updatedAt = Clock.System.now()
        )
        remote.updateRequest(toSave)
        cacheRequest(toSave)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun updateRequestStatus(request: RoadRequest): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)

        val remoteRequest = remote.getRequest(request.id).first()
            ?: return AppResult.Failure(AppError.NotFound)

        val from = remoteRequest.status
        val to = request.status

        if (from == to) return AppResult.Success(Unit)

        val actor = when {
            remoteRequest.authorId == currentUid -> RequestActor.AUTHOR
            remoteRequest.helperId == currentUid -> RequestActor.HELPER
            else -> RequestActor.OTHER
        }

        if (!RequestStatusTransitions.isTransitionAllowed(from, to)) {
            return AppResult.Failure(AppError.Conflict)
        }
        if (!RequestStatusTransitions.isActorAllowed(from, to, actor)) {
            return AppResult.Failure(AppError.Forbidden)
        }

        val newHelperId = when {
            to == RequestStatus.OPEN -> ""
            from == RequestStatus.OPEN && to == RequestStatus.IN_PROGRESS &&
                actor == RequestActor.OTHER -> currentUid
            else -> remoteRequest.helperId
        }

        val toSave = remoteRequest.copy(
            status = to,
            helperId = newHelperId,
            updatedAt = Clock.System.now()
        )
        remote.updateRequest(toSave)
        cacheRequest(toSave)
        AppResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override suspend fun deleteRequest(id: String): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)

        val requestResult = remote.getRequest(id).first()
        if (requestResult != null && requestResult.authorId.isNotBlank() && requestResult.authorId != currentUid) {
            AppResult.Failure(AppError.Forbidden)
        } else {
            remote.deleteRequest(id)
            evictCachedRequest(id)
            AppResult.Success(Unit)
        }
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }

    override fun observeResponses(
        requestId: String
    ): Flow<AppResult<List<RequestResponse>>> =
        remote.getResponses(requestId)
            .map<List<RequestResponse>, AppResult<List<RequestResponse>>> {
                AppResult.Success(it)
            }
            .catch { emit(AppResult.Failure(it.toAppError())) }

    override suspend fun respondToRequest(
        requestId: String,
        message: String
    ): AppResult<Unit> = try {
        val user =
            getCurrentUserUseCase().first() ?: return AppResult.Failure(AppError.Unauthorized)
        val request = remote.getRequest(requestId).first()
            ?: return AppResult.Failure(AppError.NotFound)

        if (request.authorId == user.uid) return AppResult.Failure(AppError.Forbidden)
        if (request.status != RequestStatus.OPEN) return AppResult.Failure(AppError.Conflict)

        remote.createResponse(
            RequestResponse(
                id = user.uid,
                requestId = requestId,
                helperId = user.uid,
                helperName = user.email ?: user.uid,
                message = message.trim(),
                createdAt = Clock.System.now()
            )
        )
        AppResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toAppError())
    }

    override suspend fun selectHelper(
        requestId: String,
        helperId: String
    ): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)
        val request = remote.getRequest(requestId).first()
            ?: return AppResult.Failure(AppError.NotFound)

        if (request.authorId != currentUid) return AppResult.Failure(AppError.Forbidden)
        if (request.status != RequestStatus.OPEN && request.status != RequestStatus.IN_PROGRESS) {
            return AppResult.Failure(AppError.Conflict)
        }

        val toSave = request.copy(
            helperId = helperId,
            status = RequestStatus.IN_PROGRESS,
            updatedAt = Clock.System.now()
        )
        remote.updateRequest(toSave)
        cacheRequest(toSave)
        AppResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toAppError())
    }

    override suspend fun deleteResponse(
        requestId: String,
        responseId: String
    ): AppResult<Unit> = try {
        val currentUid =
            getCurrentUserUseCase().first()?.uid ?: return AppResult.Failure(AppError.Unauthorized)
        val request = remote.getRequest(requestId).first()
        val response = remote.getResponses(requestId).first()
            .firstOrNull { it.id == responseId }
            ?: return AppResult.Failure(AppError.NotFound)

        if (request?.authorId != currentUid && response.helperId != currentUid) {
            return AppResult.Failure(AppError.Forbidden)
        }

        remote.deleteResponse(requestId, responseId)
        AppResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toAppError())
    }

    /**
     * Local Room cache is write-through only; failures must not mask a
     * successful remote operation as a network error.
     */
    private suspend fun cacheRequest(request: RoadRequest) = try {
        dao.insert(RoadRequestMapper.toEntity(request))
    } catch (_: Exception) {
    }

    private suspend fun evictCachedRequest(id: String) = try {
        dao.deleteById(id)
    } catch (_: Exception) {
    }
}
