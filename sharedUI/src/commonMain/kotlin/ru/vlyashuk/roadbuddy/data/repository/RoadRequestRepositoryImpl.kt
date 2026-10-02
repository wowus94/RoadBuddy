package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.vlyashuk.roadbuddy.data.error.ForbiddenException
import ru.vlyashuk.roadbuddy.data.error.UnauthorizedException
import ru.vlyashuk.roadbuddy.data.error.safeCall
import ru.vlyashuk.roadbuddy.data.error.toAppError
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import kotlin.time.Clock
import kotlin.uuid.Uuid

class RoadRequestRepositoryImpl(
    private val dao: RoadRequestDao,
    private val remote: RoadRequestRemoteDataSource,
    private val authService: AuthService
) : RoadRequestRepository {

    override fun getRequests(): Flow<AppResult<List<RoadRequest>>> =
        remote.getRequests()
            .map<List<RoadRequest>, AppResult<List<RoadRequest>>> { AppResult.Success(it) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(AppResult.Failure(exception.toAppError()))
            }

    override suspend fun createRequest(request: RoadRequest): AppResult<Unit> = safeCall {
        val currentUid = authService.currentUser.first()?.uid
            ?: throw UnauthorizedException()

        val newId = request.id.ifBlank {
            Uuid.random().toString()
        }
        val now = Clock.System.now()
        val toSave = request.copy(
            id = newId,
            authorId = currentUid,
            createdAt = now,
            updatedAt = now
        )
        remote.createRequest(toSave)
        dao.insert(RoadRequestMapper.toEntity(toSave))
    }

    override fun getRequest(id: String): Flow<AppResult<RoadRequest?>> =
        remote.getRequest(id)
            .map<RoadRequest?, AppResult<RoadRequest?>> { AppResult.Success(it) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(AppResult.Failure(exception.toAppError()))
            }

    override suspend fun updateRequest(request: RoadRequest): AppResult<Unit> = safeCall {
        val currentUid = authService.currentUser.first()?.uid
            ?: throw UnauthorizedException()

        if (request.authorId.isNotBlank() && request.authorId != currentUid) {
            throw ForbiddenException()
        }

        val toSave = request.copy(updatedAt = Clock.System.now())
        remote.updateRequest(toSave)
        dao.insert(RoadRequestMapper.toEntity(toSave))
    }
}