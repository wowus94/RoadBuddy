package ru.vlyashuk.roadbuddy.presentation.myrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetMyRequestsUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

data class MyRequestsUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyRequestsViewModel(
    private val getMyRequestsUseCase: GetMyRequestsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRequestsUiState())
    val uiState: StateFlow<MyRequestsUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            getMyRequestsUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(isLoading = false, requests = result.value)
                            }
                        }

                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = result.error.toMessage())
                            }
                        }
                    }
                }
        }
    }
}
