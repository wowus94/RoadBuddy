package ru.vlyashuk.roadbuddy.domain.model

data class RequestsPage(
    val requests: List<RoadRequest>,
    val nextOffset: Int?,
    val hasMore: Boolean
)
