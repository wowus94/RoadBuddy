package ru.vlyashuk.roadbuddy.domain.util

import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoHash {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    private val CELL_SIZE_KM = doubleArrayOf(
        5000.0, 1250.0, 156.0, 39.0, 4.9, 1.2, 0.153, 0.038, 0.0048, 0.0012
    )

    fun encode(latitude: Double, longitude: Double, precision: Int = 9): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        val builder = StringBuilder(precision)
        var bit = 0
        var ch = 0
        var isEvenBit = true // true -> longitude bit

        while (builder.length < precision) {
            if (isEvenBit) {
                val mid = (lonMin + lonMax) / 2.0
                if (longitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = mid
                } else {
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2.0
                if (latitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            isEvenBit = !isEvenBit
            if (bit < 4) {
                bit++
            } else {
                builder.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return builder.toString()
    }

    fun decodeBounds(hash: String): DoubleArray {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        var isEvenBit = true

        for (c in hash) {
            val idx = BASE32.indexOf(c)
            require(idx >= 0) { "Invalid geohash character: $c" }
            for (mask in intArrayOf(16, 8, 4, 2, 1)) {
                val bitSet = (idx and mask) != 0
                if (isEvenBit) {
                    val mid = (lonMin + lonMax) / 2.0
                    if (bitSet) lonMin = mid else lonMax = mid
                } else {
                    val mid = (latMin + latMax) / 2.0
                    if (bitSet) latMin = mid else latMax = mid
                }
                isEvenBit = !isEvenBit
            }
        }
        return doubleArrayOf(latMin, lonMin, latMax, lonMax)
    }

    fun withNeighbors(hash: String): Set<String> {
        val bounds = decodeBounds(hash)
        val lat = (bounds[0] + bounds[2]) / 2.0
        val lon = (bounds[1] + bounds[3]) / 2.0
        val latStep = bounds[2] - bounds[0]
        val lonStep = bounds[3] - bounds[1]
        val result = LinkedHashSet<String>(9)
        for (dLat in -1..1) {
            for (dLon in -1..1) {
                val nLat = lat + dLat * latStep
                val nLon = lon + dLon * lonStep
                if (nLat in -90.0..90.0 && nLon in -180.0..180.0) {
                    result.add(encode(nLat, nLon, hash.length))
                }
            }
        }
        return result
    }

    fun precisionForRadius(radiusKm: Double): Int {
        if (radiusKm <= 0) return 9
        for (i in CELL_SIZE_KM.indices) {
            if (CELL_SIZE_KM[i] >= radiusKm) return i + 1
        }
        return 9
    }
}

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
        sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(sqrt(a))
}

class GeoRadiusContext(
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val precision: Int,
    private val prefixes: Set<String>
) {
    fun isCandidate(request: RoadRequest): Boolean {
        val hash = request.geohash
        if (hash.isNullOrEmpty()) return true // no geohash -> verify by coordinates
        val p = minOf(precision, hash.length)
        val prefix = hash.take(p)
        return prefixes.any { it.startsWith(prefix) || prefix.startsWith(it.take(p)) }
    }

    fun distanceTo(request: RoadRequest): Double? {
        val lat = request.latitude ?: return null
        val lon = request.longitude ?: return null
        return haversineKm(centerLat, centerLon, lat, lon)
    }

    fun contains(request: RoadRequest): Boolean {
        if (!isCandidate(request)) return false
        val d = distanceTo(request) ?: return false
        return d <= radiusKm
    }
}

fun geoRadiusContext(centerLat: Double, centerLon: Double, radiusKm: Double): GeoRadiusContext {
    val precision = GeoHash.precisionForRadius(radiusKm)
    val centerHash = GeoHash.encode(centerLat, centerLon, precision)
    return GeoRadiusContext(
        centerLat = centerLat,
        centerLon = centerLon,
        radiusKm = radiusKm,
        precision = precision,
        prefixes = GeoHash.withNeighbors(centerHash)
    )
}
