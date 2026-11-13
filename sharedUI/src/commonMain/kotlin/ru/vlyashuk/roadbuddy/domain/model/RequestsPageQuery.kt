package ru.vlyashuk.roadbuddy.domain.model

data class RequestsPageQuery(
    val type: RequestType? = null,
    val status: RequestStatus? = null,
    val sortDescending: Boolean = true
)
