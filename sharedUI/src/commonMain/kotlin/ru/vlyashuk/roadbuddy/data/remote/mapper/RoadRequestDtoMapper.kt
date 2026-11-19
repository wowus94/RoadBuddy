package ru.vlyashuk.roadbuddy.data.remote.mapper

import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestDto
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

object RoadRequestDtoMapper {

    fun toDomain(dto: RoadRequestDto): RoadRequest = RoadRequest(
        id = dto.id,
        title = dto.title,
        description = dto.description,
        type = dto.type.toEnumOrNull<RequestType>() ?: RequestType.OTHER,
        latitude = dto.latitude,
        longitude = dto.longitude,
        geohash = dto.geohash,
        authorId = dto.authorId,
        authorName = dto.authorName,
        contact = dto.contact,
        helperId = dto.helperId,
        status = dto.status.toEnumOrNull<RequestStatus>() ?: RequestStatus.OPEN,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt
    )

    fun fromDomain(request: RoadRequest): RoadRequestDto = RoadRequestDto(
        id = request.id,
        title = request.title,
        description = request.description,
        type = request.type.name,
        latitude = request.latitude,
        longitude = request.longitude,
        geohash = request.geohash,
        authorId = request.authorId,
        authorName = request.authorName,
        contact = request.contact,
        helperId = request.helperId,
        status = request.status.name,
        createdAt = request.createdAt,
        updatedAt = request.updatedAt
    )

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { it.name == this }
}
