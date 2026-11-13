package ru.vlyashuk.roadbuddy.presentation.components

import ru.vlyashuk.roadbuddy.domain.util.GeoHash
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln

private val CLUSTER_CELL_KM = doubleArrayOf(
    5000.0, 1250.0, 156.0, 39.0, 4.9, 1.2, 0.153, 0.038, 0.0048, 0.0012
)

private const val CLUSTER_TARGET_PIXELS = 90.0
private const val MIN_ZOOM = 1.0
private const val MAX_ZOOM = 20.0
private const val CLUSTER_ID_PREFIX = "cluster:"

fun isClusterMarker(marker: MapMarker): Boolean = marker.clusterCount > 1

fun clusterMarkers(markers: List<MapMarker>, zoom: Float): List<MapMarker> {
    val clusterable = markers.filter { it.clusterable }
    if (clusterable.size <= 1) return markers

    val avgLat = clusterable.map { it.latitude }.average()
    val z = zoom.toDouble().coerceIn(MIN_ZOOM, MAX_ZOOM)
    val metersPerPixel = 156543.03392 * cos(avgLat * PI / 180.0) * exp(-z * ln(2.0))
    val cellKm = metersPerPixel * CLUSTER_TARGET_PIXELS / 1000.0

    var precision = 1
    var bestScore = Double.MAX_VALUE
    for (i in CLUSTER_CELL_KM.indices) {
        val score = kotlin.math.abs(ln(CLUSTER_CELL_KM[i] / cellKm))
        if (score < bestScore) {
            bestScore = score
            precision = i + 1
        }
    }

    val groups = LinkedHashMap<String, MutableList<MapMarker>>()
    clusterable.forEach { marker ->
        val key = GeoHash.encode(marker.latitude, marker.longitude, precision)
        groups.getOrPut(key) { mutableListOf() }.add(marker)
    }

    val result = markers.filter { !it.clusterable }.toMutableList()
    groups.forEach { (key, group) ->
        if (group.size == 1) {
            result.add(group.first())
        } else {
            result.add(
                MapMarker(
                    id = "$CLUSTER_ID_PREFIX$key",
                    latitude = group.map { it.latitude }.average(),
                    longitude = group.map { it.longitude }.average(),
                    title = "",
                    clusterCount = group.size,
                    memberIds = group.map { it.id },
                    clusterPoints = group.map { it.latitude to it.longitude },
                    clusterable = false
                )
            )
        }
    }
    return result
}
