package ru.vlyashuk.roadbuddy.presentation.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.lang.ref.WeakReference

private const val MAPKIT_META_KEY = "ru.vlyashuk.roadbuddy.MAPKIT_API_KEY"
private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173
private const val ZOOM = 15f
private var mapKitInitialized = false
private fun initMapKit(context: Context) {
    if (mapKitInitialized) return
    val info = if (Build.VERSION.SDK_INT >= 33) {
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    }
    val apiKey = info.metaData?.getString(MAPKIT_META_KEY).orEmpty().trim('"', ' ')
    check(apiKey.isNotBlank()) {
        "API key is empty"
    }
    MapKitFactory.setApiKey(apiKey)
    MapKitFactory.initialize(context.applicationContext)
    mapKitInitialized = true
}

@Composable
actual fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        initMapKit(context)
        MapView(context)
    }
    val map = mapView.mapWindow.map

    val listener: InputListener = remember(onLocationSelected) {
        object : InputListener {
            override fun onMapTap(m: Map, p: Point) =
                onLocationSelected(p.latitude, p.longitude)
            override fun onMapLongTap(m: Map, p: Point) = Unit
        }
    }
    DisposableEffect(mapView, listener) {
        val ref: WeakReference<InputListener> = WeakReference(listener)
        map.addInputListener(ref)
        onDispose { map.removeInputListener(ref) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        fun start() { MapKitFactory.getInstance().onStart(); mapView.onStart() }
        fun stop() { mapView.onStop(); MapKitFactory.getInstance().onStop() }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationManager = remember {
        MapKitFactory.getInstance().createLocationManager()
    }

    val locationListener = remember(onLocationSelected) {
        object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                val position = location.position
                onLocationSelected(position.latitude, position.longitude)
            }

            override fun onLocationStatusUpdated(status: LocationStatus) {
                if (status != LocationStatus.AVAILABLE) {
                    val defaultTarget = Point(DEFAULT_LAT, DEFAULT_LON)
                    map.move(CameraPosition(defaultTarget, ZOOM, 0f, 0f), Animation(Animation.Type.SMOOTH, 0.3f), null)
                }
            }
        }
    }

    val markerImageProvider = remember {
        val size = 60
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
        paint.color = "#E53935".toColorInt() // Красный цвет
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, paint)
        ImageProvider.fromBitmap(bitmap)
    }

    val placemark = remember { mutableStateOf<PlacemarkMapObject?>(null) }
    LaunchedEffect(latitude, longitude, locationManager, locationListener, markerImageProvider) {
        if (latitude == null || longitude == null) {
            locationManager.requestSingleUpdate(
                WeakReference<LocationListener>(locationListener)
            )
        } else {
            val target = Point(latitude, longitude)
            map.move(CameraPosition(target, ZOOM, 0f, 0f), Animation(Animation.Type.SMOOTH, 0.3f), null)
            if (placemark.value == null) {
                placemark.value = map.mapObjects.addPlacemark().apply {
                    setIcon(markerImageProvider)
                }
            }
            placemark.value?.let {
                it.isVisible = true
                it.geometry = target
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}