package ru.vlyashuk.roadbuddy.presentation.edit

import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest

data class EditUiState(
    val request: RoadRequest? = null,
    val title: String = "",
    val description: String = "",
    val type: RequestType = RequestType.OTHER,
    val authorName: String = "",
    val contact: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val error: String? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() && authorName.isNotBlank() && contact.isNotBlank()
}
