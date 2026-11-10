package ru.vlyashuk.roadbuddy.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ru.vlyashuk.roadbuddy.presentation.components.initMapKit
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

private const val LOCATION_TIMEOUT_MS = 10_000L

internal object LocationContext {
    var appContext: Context? = null
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberLocationPermissionRequester(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    LocationContext.appContext = context.applicationContext
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) onGranted() else onDenied()
    }
    return remember {
        {
            if (hasLocationPermission(context)) {
                onGranted()
            } else {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}

actual suspend fun requestCurrentLocation(): Pair<Double, Double>? {
    val context = LocationContext.appContext ?: return null
    if (!hasLocationPermission(context)) return null

    initMapKit(context)
    val locationManager = MapKitFactory.getInstance().createLocationManager()

    return withTimeoutOrNull(LOCATION_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
            val listener = object : LocationListener {
                override fun onLocationUpdated(location: Location) {
                    if (cont.isActive) {
                        cont.resume(location.position.latitude to location.position.longitude)
                    }
                }

                override fun onLocationStatusUpdated(status: LocationStatus) {
                    if (status != LocationStatus.AVAILABLE && cont.isActive) {
                        cont.resume(null)
                    }
                }
            }
            locationManager.requestSingleUpdate(WeakReference(listener))
        }
    }
}
