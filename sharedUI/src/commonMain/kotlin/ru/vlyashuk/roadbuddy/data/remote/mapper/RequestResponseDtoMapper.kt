package ru.vlyashuk.roadbuddy.data.remote.mapper

import ru.vlyashuk.roadbuddy.data.remote.firestore.RequestResponseDto
import ru.vlyashuk.roadbuddy.domain.model.RequestResponse

object RequestResponseDtoMapper {

    fun toDomain(dto: RequestResponseDto): RequestResponse = RequestResponse(
        id = dto.id,
        requestId = dto.requestId,
        helperId = dto.helperId,
        helperName = dto.helperName,
        message = dto.message,
        createdAt = dto.createdAt
    )

    fun fromDomain(response: RequestResponse): RequestResponseDto = RequestResponseDto(
        id = response.id,
        requestId = response.requestId,
        helperId = response.helperId,
        helperName = response.helperName,
        message = response.message,
        createdAt = response.createdAt
    )
}
