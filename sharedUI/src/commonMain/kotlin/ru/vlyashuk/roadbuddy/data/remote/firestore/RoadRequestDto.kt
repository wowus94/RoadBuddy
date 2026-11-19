package ru.vlyashuk.roadbuddy.data.remote.firestore

import kotlinx.serialization.Serializable
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import kotlin.time.Instant

@Serializable
data class RoadRequestDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = RequestType.OTHER.name,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val contact: String = "",
    val helperId: String = "",
    val status: String = RequestStatus.OPEN.name,
    @Serializable(with = InstantTimestampSerializer::class)
    val createdAt: Instant = kotlin.time.Clock.System.now(),
    @Serializable(with = InstantTimestampSerializer::class)
    val updatedAt: Instant = kotlin.time.Clock.System.now()
)
