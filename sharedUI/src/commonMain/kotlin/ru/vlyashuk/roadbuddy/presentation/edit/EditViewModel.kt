package ru.vlyashuk.roadbuddy.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

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
                when (val result = getRequestByIdUseCase(requestId).first()) {
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.error.toMessage())
                        }
                    }
                    is AppResult.Success -> {
                        val request = result.value
                        if (request == null) {
                            _uiState.update {
                                it.copy(isLoading = false, error = AppError.NotFound.toMessage())
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    request = request,
                                    title = request.title,
                                    description = request.description,
                                    type = request.type,
                                    authorName = request.authorName,
                                    contact = request.contact,
                                    latitude = request.latitude,
                                    longitude = request.longitude
                                )
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(isLoading = false, error = AppError.Unknown.toMessage())
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onTypeChanged(value: RequestType) = _uiState.update { it.copy(type = value) }
    fun onAuthorNameChanged(value: String) = _uiState.update { it.copy(authorName = value) }
    fun onContactChanged(value: String) = _uiState.update { it.copy(contact = value) }

    fun onLocationSelected(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(latitude = lat, longitude = lon)
        }
    }

    fun updateRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            val original = state.request
            if (!state.isValid || original == null) {
                _uiState.update { it.copy(error = AppError.Validation.toMessage()) }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }

            val updated = original.copy(
                title = state.title,
                description = state.description,
                type = state.type,
                authorName = state.authorName,
                contact = state.contact,
                latitude = state.latitude,
                longitude = state.longitude
            )

            when (val result = updateRequestUseCase(updated)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaved = true, isSaving = false) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(isSaving = false, error = result.error.toMessage())
                    }
                }
            }
        }
    }
}