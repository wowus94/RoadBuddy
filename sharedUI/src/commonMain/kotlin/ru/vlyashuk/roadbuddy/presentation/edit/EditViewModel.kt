package ru.vlyashuk.roadbuddy.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase

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
    val error: String? = null
) {
    val isValid: Boolean
        get() = title.isNotBlank() && authorName.isNotBlank() && contact.isNotBlank()
}

class EditViewModel(
    private val getRequestByIdUseCase: GetRequestByIdUseCase,
    private val updateRequestUseCase: UpdateRequestUseCase,
    private val requestId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState(isLoading = true))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    init {
        loadRequest()
    }

    private fun loadRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = getRequestByIdUseCase(requestId).first()
                if (request == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Request not found") }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            request = request,
                            title = request.title,
                            description = request.description,
                            type = request.type,
                            authorName = request.authorName,
                            contact = request.contact
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onTypeChanged(value: RequestType) = _uiState.update { it.copy(type = value) }
    fun onAuthorNameChanged(value: String) = _uiState.update { it.copy(authorName = value) }
    fun onContactChanged(value: String) = _uiState.update { it.copy(contact = value) }

    fun updateRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            val original = state.request
            if (!state.isValid || original == null) {
                _uiState.update { it.copy(error = "Fill all required fields") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }

            val updated = original.copy(
                title = state.title,
                description = state.description,
                type = state.type,
                authorName = state.authorName,
                contact = state.contact
            )

            updateRequestUseCase(updated)
                .onSuccess {
                    _uiState.update { it.copy(isSaved = true, isSaving = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }
}