package ru.vlyashuk.roadbuddy.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class RequestResponse(
    val id: String = "",
    val requestId: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val message: String = "",
    val createdAt: Instant = Clock.System.now()
)
