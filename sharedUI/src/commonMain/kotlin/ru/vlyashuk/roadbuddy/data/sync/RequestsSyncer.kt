package ru.vlyashuk.roadbuddy.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.data.local.dao.RoadRequestDao
import ru.vlyashuk.roadbuddy.data.local.mapper.RoadRequestMapper
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps the local Room cache in sync with Firestore for the whole app session.
 *
 * Subscribes to the remote snapshot listener while a user is signed in and
 * atomically replaces the cache on every emission, so Room flows (and therefore
 * the UI) update live. On sign-out the cache is cleared. Network failures are
 * retried with a delay; Firestore itself serves cached snapshots while offline.
 */
class RequestsSyncer(
    private val dao: RoadRequestDao,
    private val remote: RoadRequestRemoteDataSource,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                try {
                    getCurrentUserUseCase()
                        .flatMapLatest { user ->
                            if (user == null) {
                                flowOf(emptyList())
                            } else {
                                remote.getRequests()
                            }
                        }
                        .collectLatest { requests ->
                            dao.replaceAll(requests.map(RoadRequestMapper::toEntity))
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    delay(RETRY_DELAY)
                }
            }
        }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
