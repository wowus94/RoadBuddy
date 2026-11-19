package ru.vlyashuk.roadbuddy.data.remote.firestore

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class RequestResponseDto(
    val id: String = "",
    val requestId: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val message: String = "",
    @Serializable(with = InstantTimestampSerializer::class)
    val createdAt: Instant = kotlin.time.Clock.System.now()
)
