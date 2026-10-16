package ru.vlyashuk.roadbuddy.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.fakes.FakeAuthService
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestDao
import ru.vlyashuk.roadbuddy.fakes.FakeRoadRequestRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoadRequestRepositoryImplTest {

    private val request = RoadRequest(
        id = "req-1",
        title = "Tow",
        authorId = "uid"
    )

    private suspend fun signedInAuth(uid: String = "uid") = FakeAuthService().apply {
        signInResult = AppResult.Success(AuthUser(uid = uid, email = "a@b.com"))
        signIn("a@b.com", "password")
    }

    private fun createRepository(
        remote: FakeRoadRequestRemoteDataSource = FakeRoadRequestRemoteDataSource(),
        dao: FakeRoadRequestDao = FakeRoadRequestDao(),
        authService: FakeAuthService
    ) = RoadRequestRepositoryImpl(dao, remote, authService)

    // getRequests

    @Test
    fun getRequests_success_emitsSuccess() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        remote.emit(listOf(request))
        val repository = createRepository(remote = remote, authService = FakeAuthService())

        val result = repository.getRequests().first()

        assertIs<AppResult.Success<List<RoadRequest>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("req-1", result.value[0].id)
    }

    @Test
    fun getRequests_remoteError_emitsFailure() = runTest {
        val remote = FakeRoadRequestRemoteDataSource().apply {
            requestsError = RuntimeException("network down")
        }
        val repository = createRepository(remote = remote, authService = FakeAuthService())

        val result = repository.getRequests().first()

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Network, result.error)
    }

    // getRequest

    @Test
    fun getRequest_existingId_emitsRequest() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        remote.emit(listOf(request))
        val repository = createRepository(remote = remote, authService = FakeAuthService())

        val result = repository.getRequest("req-1").first()

        assertIs<AppResult.Success<RoadRequest?>>(result)
        assertEquals("Tow", result.value?.title)
    }

    @Test
    fun getRequest_missingId_emitsNull() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        remote.emit(emptyList())
        val repository = createRepository(remote = remote, authService = FakeAuthService())

        val result = repository.getRequest("missing").first()

        assertIs<AppResult.Success<RoadRequest?>>(result)
        assertNull(result.value)
    }

    // createRequest

    @Test
    fun createRequest_authorized_savesWithUidAndGeneratedId() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        val dao = FakeRoadRequestDao()
        val repository = createRepository(remote, dao, signedInAuth())

        val result = repository.createRequest(request.copy(id = ""))

        assertIs<AppResult.Success<Unit>>(result)
        val saved = remote.createdRequest!!
        assertNotEquals("", saved.id)
        assertEquals("uid", saved.authorId)
        assertEquals(1, dao.insertCallCount)
        assertEquals(saved.id, dao.lastInserted?.id)
    }

    @Test
    fun createRequest_notAuthorized_returnsUnauthorized() = runTest {
        val repository = createRepository(authService = FakeAuthService())

        val result = repository.createRequest(request)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Unauthorized, result.error)
    }

    @Test
    fun createRequest_remoteError_returnsFailure() = runTest {
        val remote = FakeRoadRequestRemoteDataSource().apply {
            createError = RuntimeException("network")
        }
        val repository = createRepository(remote = remote, authService = signedInAuth())

        val result = repository.createRequest(request)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Network, result.error)
    }

    // updateRequest

    @Test
    fun updateRequest_owner_updatesRemoteAndDao() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        val dao = FakeRoadRequestDao()
        val repository = createRepository(remote, dao, signedInAuth(uid = "uid"))

        val result = repository.updateRequest(request.copy(title = "New"))

        assertIs<AppResult.Success<Unit>>(result)
        assertEquals("New", remote.updatedRequest?.title)
        assertEquals(1, dao.insertCallCount)
    }

    @Test
    fun updateRequest_notOwner_returnsForbidden() = runTest {
        val remote = FakeRoadRequestRemoteDataSource()
        val repository = createRepository(remote = remote, authService = signedInAuth(uid = "other"))

        val result = repository.updateRequest(request)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Forbidden, result.error)
        assertNull(remote.updatedRequest)
    }

    @Test
    fun updateRequest_notAuthorized_returnsUnauthorized() = runTest {
        val repository = createRepository(authService = FakeAuthService())

        val result = repository.updateRequest(request)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Unauthorized, result.error)
    }

    @Test
    fun updateRequest_remoteError_returnsFailure() = runTest {
        val remote = FakeRoadRequestRemoteDataSource().apply {
            updateError = RuntimeException("permission denied")
        }
        val repository = createRepository(remote = remote, authService = signedInAuth())

        val result = repository.updateRequest(request)

        assertIs<AppResult.Failure>(result)
    }
}
