package ru.vlyashuk.roadbuddy.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase

data class DetailsUiState(
    val request: RoadRequest? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DetailsViewModel(
    private val getRequestByIdUseCase: GetRequestByIdUseCase,
    private val requestId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadRequest()
    }

    fun loadRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = getRequestByIdUseCase(requestId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        request = request,
                        error = if (request == null) "Request not found" else null
                    )
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}