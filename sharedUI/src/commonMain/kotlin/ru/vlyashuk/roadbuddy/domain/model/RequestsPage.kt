package ru.vlyashuk.roadbuddy.domain.model

data class RequestsPage(
    val requests: List<RoadRequest>,
    val nextCursor: String?,
    val hasMore: Boolean
)
