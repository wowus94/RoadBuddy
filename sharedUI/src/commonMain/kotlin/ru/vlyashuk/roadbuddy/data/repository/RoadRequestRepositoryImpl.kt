package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
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
            id = newId, authorId = currentUid, createdAt = now, updatedAt = now
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

        if (request.authorId.isNotBlank() && request.authorId != currentUid) {
            return AppResult.Failure(AppError.Forbidden)
        }

        val toSave = request.copy(updatedAt = Clock.System.now())
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

        val isAuthor = remoteRequest.authorId == currentUid
        val isHelper = remoteRequest.helperId == currentUid

        val newHelperId = when {
            isAuthor -> remoteRequest.helperId
            remoteRequest.status == RequestStatus.OPEN && request.status == RequestStatus.IN_PROGRESS ->
                currentUid
            isHelper -> remoteRequest.helperId
            else -> return AppResult.Failure(AppError.Forbidden)
        }

        val finalHelperId =
            if (request.status == RequestStatus.OPEN) "" else newHelperId

        val toSave = remoteRequest.copy(
            status = request.status,
            helperId = finalHelperId,
            updatedAt = Clock.System.now()
        )
        remote.updateRequest(toSave)
        cacheRequest(toSave)
        AppResult.Success(Unit)
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
