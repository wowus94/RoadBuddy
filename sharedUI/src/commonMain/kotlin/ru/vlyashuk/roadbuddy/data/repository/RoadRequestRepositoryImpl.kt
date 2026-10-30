package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
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

    override fun getRequests(): Flow<AppResult<List<RoadRequest>>> = remote.getRequests()
        .map<List<RoadRequest>, AppResult<List<RoadRequest>>> { AppResult.Success(it) }
        .catch { emit(AppResult.Failure(AppError.Network)) }


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

    override fun getRequest(id: String): Flow<AppResult<RoadRequest?>> =
        remote.getRequest(id).map<RoadRequest?, AppResult<RoadRequest?>> { AppResult.Success(it) }
            .catch { emit(AppResult.Failure(AppError.Network)) }

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