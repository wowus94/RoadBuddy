package ru.vlyashuk.roadbuddy.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase

data class CreateUiState(
    val title: String = "",
    val description: String = "",
    val type: RequestType = RequestType.OTHER,
    val authorName: String = "",
    val contact: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() && authorName.isNotBlank() && contact.isNotBlank()
}

class CreateViewModel(
    private val createRequestUseCase: CreateRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onTypeChanged(value: RequestType) = _uiState.update { it.copy(type = value) }
    fun onAuthorNameChanged(value: String) = _uiState.update { it.copy(authorName = value) }
    fun onContactChanged(value: String) = _uiState.update { it.copy(contact = value) }

    fun createRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isValid) {
                _uiState.update { it.copy(error = "Fill all required fields") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }

            val request = RoadRequest(
                title = state.title,
                description = state.description,
                type = state.type,
                authorName = state.authorName,
                contact = state.contact
            )

            createRequestUseCase(request)
                .onSuccess {
                    _uiState.update { it.copy(isSaved = true, isSaving = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }
}
