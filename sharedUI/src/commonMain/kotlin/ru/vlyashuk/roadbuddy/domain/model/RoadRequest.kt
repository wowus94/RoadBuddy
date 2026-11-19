package ru.vlyashuk.roadbuddy.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class RoadRequest(
    val id: String = "",
    val title: String,
    val description: String = "",
    val type: RequestType = RequestType.OTHER,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val contact: String = "",
    val helperId: String = "",
    val status: RequestStatus = RequestStatus.OPEN,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

enum class RequestType {
    TOWING, FUEL, BATTERY, TIRE, OTHER
}

enum class RequestStatus {
    OPEN, IN_PROGRESS, COMPLETED, CANCELLED
}
