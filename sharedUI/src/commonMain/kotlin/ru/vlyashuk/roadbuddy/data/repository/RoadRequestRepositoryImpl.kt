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
        dao.insert(RoadRequestMapper.toEntity(toSave))
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
        dao.insert(RoadRequestMapper.toEntity(toSave))
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Network)
    }
}