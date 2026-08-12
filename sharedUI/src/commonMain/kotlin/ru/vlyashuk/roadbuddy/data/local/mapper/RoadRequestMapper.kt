package ru.vlyashuk.roadbuddy.data.local.mapper

import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import kotlin.time.Instant

object RoadRequestMapper {

    fun toEntity(request: RoadRequest): RoadRequestEntity = RoadRequestEntity(
        id = request.id,
        title = request.title,
        description = request.description,
        type = request.type.name,
        latitude = request.latitude,
        longitude = request.longitude,
        authorName = request.authorName,
        contact = request.contact,
        status = request.status.name,
        createdAt = request.createdAt.toEpochMilliseconds(),
        updatedAt = request.updatedAt.toEpochMilliseconds()
    )

    fun toDomain(entity: RoadRequestEntity): RoadRequest = RoadRequest(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        type = enumValueOf(entity.type),
        latitude = entity.latitude,
        longitude = entity.longitude,
        authorName = entity.authorName,
        contact = entity.contact,
        status = enumValueOf(entity.status),
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )
}