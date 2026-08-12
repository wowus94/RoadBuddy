package ru.vlyashuk.roadbuddy.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class RoadRequest(
    val id: String = "",
    val title: String,
    val description: String = "",
    val type: RequestType = RequestType.OTHER,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val authorName: String = "",
    val contact: String = "",
    val status: RequestStatus = RequestStatus.OPEN,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

@Serializable
enum class RequestType {
    TOWING, FUEL, BATTERY, TIRE, OTHER
}

@Serializable
enum class RequestStatus {
    OPEN, IN_PROGRESS, COMPLETED, CANCELLED
}