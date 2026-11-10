package ru.vlyashuk.roadbuddy.presentation.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

private const val LOCATION_TIMEOUT_MS = 10_000L

@OptIn(ExperimentalForeignApi::class)
private object IosLocationHolder {
    var manager: CLLocationManager? = null
}

@OptIn(ExperimentalForeignApi::class)
private fun isAuthorized(manager: CLLocationManager): Boolean {
    val status = manager.authorizationStatus()
    return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
        status == kCLAuthorizationStatusAuthorizedAlways
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberLocationPermissionRequester(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val requester = remember {
        val manager = CLLocationManager()
        IosLocationHolder.manager = manager
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                if (isAuthorized(manager)) onGranted() else onDenied()
            }
        }
        manager.delegate = delegate
        {
            if (isAuthorized(manager)) {
                onGranted()
            } else {
                manager.requestWhenInUseAuthorization()
            }
        }
    }
    return requester
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun requestCurrentLocation(): Pair<Double, Double>? {
    val manager = IosLocationHolder.manager ?: CLLocationManager().also {
        IosLocationHolder.manager = it
    }
    if (!isAuthorized(manager)) return null

    return withTimeoutOrNull(LOCATION_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    if (cont.isActive) {
                        val result: Pair<Double, Double>? = location?.coordinate?.useContents {
                            latitude to longitude
                        }
                        cont.resume(result)
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: platform.Foundation.NSError
                ) {
                    if (cont.isActive) cont.resume(null)
                }
            }
            manager.delegate = delegate
            manager.requestLocation()
        }
    }
}
