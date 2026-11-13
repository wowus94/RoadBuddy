package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val clusterCount: Int = 1,
    val memberIds: List<String> = emptyList(),
    val clusterPoints: List<Pair<Double, Double>> = emptyList(),
    val clusterable: Boolean = true
)

@Composable
expect fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    onMarkerClick: (id: String) -> Unit = {},
    focusPoints: List<Pair<Double, Double>> = emptyList(),
    onZoomChanged: (zoom: Float) -> Unit = {},
    initialCamera: MapCamera? = null
)

data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float
)

data class BoundsCamera(
    val latitude: Double,
    val longitude: Double,
    val latitudeSpan: Double,
    val longitudeSpan: Double
)

fun cameraForPoints(points: List<Pair<Double, Double>>): BoundsCamera? {
    if (points.isEmpty()) return null
    val minLat = points.minOf { it.first }
    val maxLat = points.maxOf { it.first }
    val minLon = points.minOf { it.second }
    val maxLon = points.maxOf { it.second }
    return BoundsCamera(
        latitude = (minLat + maxLat) / 2.0,
        longitude = (minLon + maxLon) / 2.0,
        latitudeSpan = ((maxLat - minLat).coerceAtLeast(0.002) * 1.4).coerceAtMost(120.0),
        longitudeSpan = ((maxLon - minLon).coerceAtLeast(0.002) * 1.4).coerceAtMost(120.0)
    )
}
