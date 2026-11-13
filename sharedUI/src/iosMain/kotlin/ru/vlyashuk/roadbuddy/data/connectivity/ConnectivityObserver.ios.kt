package ru.vlyashuk.roadbuddy.data.connectivity

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.UIntVar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
actual fun createConnectivityObserver(): ConnectivityObserver =
    IosConnectivityObserver()

@OptIn(ExperimentalForeignApi::class)
private class IosConnectivityObserver : ConnectivityObserver {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            while (isActive) {
                _isOnline.value = isCurrentlyReachable()
                delay(POLL_INTERVAL)
            }
        }
    }

    private fun isCurrentlyReachable(): Boolean = memScoped {
        val reachability = SCNetworkReachabilityCreateWithName(null, "8.8.8.8")
            ?: return@memScoped false
        val flags = alloc<UIntVar>()
        val succeeded = SCNetworkReachabilityGetFlags(reachability, flags.ptr)
        succeeded &&
            (flags.value and kSCNetworkReachabilityFlagsReachable) != 0u &&
            (flags.value and kSCNetworkReachabilityFlagsConnectionRequired) == 0u
    }

    private companion object {
        val POLL_INTERVAL = 5.seconds
    }
}
