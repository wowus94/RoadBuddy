package ru.vlyashuk.roadbuddy.presentation.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import org.jetbrains.compose.resources.painterResource
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.ic_add
import roadbuddy.sharedui.generated.resources.ic_remove
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.ln
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.graphics.createBitmap

private const val MAPKIT_META_KEY = "ru.vlyashuk.roadbuddy.MAPKIT_API_KEY"
private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173
private const val ZOOM = 15f
private const val MARKER_COLOR = 0xFFE53935.toInt()

private fun createMarkerBitmap(): Bitmap {
    val size = 48
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MARKER_COLOR }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, stroke)
    return bitmap
}

private const val CLUSTER_COLOR = 0xFF1E88E5.toInt()

private fun createClusterBitmap(count: Int): Bitmap {
    val size = 56
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CLUSTER_COLOR }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, stroke)
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val y = size / 2f - (text.descent() + text.ascent()) / 2f
    canvas.drawText(count.toString(), size / 2f, y, text)
    return bitmap
}
private var mapKitInitialized = false
internal fun initMapKit(context: Context) {
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
    modifier: Modifier,
    markers: List<MapMarker>,
    onMarkerClick: (id: String) -> Unit,
    focusPoints: List<Pair<Double, Double>>,
    onZoomChanged: (zoom: Float) -> Unit,
    initialCamera: MapCamera?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        initMapKit(context)
        MapView(context)
    }
    val map = mapView.mapWindow.map

    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)
    val cameraListener = remember(onLocationSelected) {
        CameraListener { _, cameraPosition, _, finished ->
            currentOnZoomChanged(cameraPosition.zoom)
            if (finished) {
                val target = cameraPosition.target
                onLocationSelected(target.latitude, target.longitude)
            }
        }
    }

    DisposableEffect(mapView, cameraListener) {
        val ref = WeakReference(cameraListener)
        map.addCameraListener(ref)
        onDispose { map.removeCameraListener(ref) }
    }

    val placemarks = remember { mutableMapOf<String, PlacemarkMapObject>() }
    val markersById = remember { mutableMapOf<String, MapMarker>() }
    val markerBitmap = remember { createMarkerBitmap() }
    val clusterBitmaps = remember { mutableMapOf<Int, Bitmap>() }
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    LaunchedEffect(markers) {
        markersById.clear()
        markers.forEach { markersById[it.id] = it }
        val wantedIds = markers.mapTo(HashSet()) { it.id }
        placemarks.keys.filter { it !in wantedIds }.forEach { id ->
            placemarks.remove(id)?.let { map.mapObjects.remove(it) }
        }
        markers.forEach { marker ->
            val placemark = placemarks[marker.id] ?: run {
                val created = map.mapObjects.addPlacemark()
                created.addTapListener(
                    WeakReference(
                        MapObjectTapListener { _, _ ->
                            val current = markersById[marker.id]
                            val cam = current?.takeIf { isClusterMarker(it) }
                                ?.let { cameraForPoints(it.clusterPoints) }
                            if (cam != null) {
                                val span = maxOf(cam.latitudeSpan, cam.longitudeSpan)
                                val zoom = (ln(360.0 / span) / ln(2.0)).toFloat()
                                    .coerceIn(3f, 20f)
                                map.move(
                                    CameraPosition(
                                        Point(cam.latitude, cam.longitude),
                                        zoom,
                                        0f,
                                        0f
                                    ),
                                    Animation(Animation.Type.SMOOTH, 0.25f),
                                    null
                                )
                            } else {
                                currentOnMarkerClick(marker.id)
                            }
                            true
                        }
                    )
                )
                placemarks[marker.id] = created
                created
            }
            placemark.geometry = Point(marker.latitude, marker.longitude)
            val icon = if (isClusterMarker(marker)) {
                clusterBitmaps.getOrPut(marker.clusterCount) {
                    createClusterBitmap(marker.clusterCount)
                }
            } else {
                markerBitmap
            }
            placemark.setIcon(ImageProvider.fromBitmap(icon))
        }
    }

    var cameraRestored by remember { mutableStateOf(false) }
    val currentInitialCamera by rememberUpdatedState(initialCamera)

    LaunchedEffect(focusPoints) {
        if (focusPoints.isEmpty()) return@LaunchedEffect
        if (!cameraRestored) {
            cameraRestored = true
            val cam = currentInitialCamera
            if (cam != null) {
                map.move(
                    CameraPosition(Point(cam.latitude, cam.longitude), cam.zoom, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.3f),
                    null
                )
                return@LaunchedEffect
            }
        }
        if (focusPoints.size == 1) {
            val (lat, lon) = focusPoints.first()
            map.move(
                CameraPosition(Point(lat, lon), ZOOM, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.3f),
                null
            )
            return@LaunchedEffect
        }
        val minLat = focusPoints.minOf { it.first }
        val maxLat = focusPoints.maxOf { it.first }
        val minLon = focusPoints.minOf { it.second }
        val maxLon = focusPoints.maxOf { it.second }
        val center = Point((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.01) * 1.4
        val zoom = (ln(360.0 / lonSpan) / ln(2.0)).toFloat().coerceIn(3f, 17f)
        map.move(
            CameraPosition(center, zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.3f),
            null
        )
    }

    DisposableEffect(lifecycleOwner, mapView) {
        fun start() {
            MapKitFactory.getInstance().onStart(); mapView.onStart()
        }

        fun stop() {
            mapView.onStop(); MapKitFactory.getInstance().onStop()
        }
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
                    map.move(
                        CameraPosition(defaultTarget, ZOOM, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 0.3f),
                        null
                    )
                }
            }
        }
    }

    LaunchedEffect(latitude, longitude, locationManager, locationListener, focusPoints.isEmpty()) {
        if (focusPoints.isNotEmpty()) return@LaunchedEffect
        if (latitude == null || longitude == null) {
            val defaultTarget = Point(DEFAULT_LAT, DEFAULT_LON)
            map.move(
                CameraPosition(defaultTarget, ZOOM, 0f, 0f)
            )
            locationManager.requestSingleUpdate(
                WeakReference<LocationListener>(locationListener)
            )
        } else {
            val target = Point(latitude, longitude)
            val currentCenter = map.cameraPosition.target
            val distance = hypot(
                currentCenter.latitude - target.latitude,
                currentCenter.longitude - target.longitude
            )
            if (distance > 0.0001) {
                map.move(
                    CameraPosition(target, ZOOM, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.3f),
                    null
                )
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Red),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ComposeColor.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.size(6.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = CardDefaults.cardColors(containerColor = ComposeColor.White)
                        ) {}
                    }
                }
                Card(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Red)
                ) {}
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        val currentPos = map.cameraPosition
                        map.move(
                            CameraPosition(
                                currentPos.target,
                                currentPos.zoom + 1f,
                                currentPos.azimuth,
                                currentPos.tilt
                            ),
                            Animation(Animation.Type.SMOOTH, 0.2f),
                            null
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = "Zoom In",
                        tint = ComposeColor.Black
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 1.dp,
                    color = ComposeColor.LightGray.copy(alpha = 0.5f)
                )

                IconButton(
                    onClick = {
                        val currentPos = map.cameraPosition
                        map.move(
                            CameraPosition(
                                currentPos.target,
                                currentPos.zoom - 1f,
                                currentPos.azimuth,
                                currentPos.tilt
                            ),
                            Animation(Animation.Type.SMOOTH, 0.2f),
                            null
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_remove),
                        contentDescription = "Zoom In",
                        tint = ComposeColor.Black
                    )
                }
            }
        }
    }
}