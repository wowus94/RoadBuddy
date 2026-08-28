package ru.vlyashuk.roadbuddy.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import kotlin.coroutines.cancellation.CancellationException

data class DetailsUiState(
    val request: RoadRequest? = null,
    val isLoading: Boolean = false,
    val isOwner: Boolean = false,
    val error: String? = null
)

class DetailsViewModel(
    private val getRequestByIdUseCase: GetRequestByIdUseCase,
    private val authService: AuthService,
    private val requestId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    getRequestByIdUseCase(requestId),
                    authService.currentUser
                ) { request, user ->
                    DetailsUiState(
                        request = request,
                        isLoading = false,
                        isOwner = request != null && user != null && request.authorId == user.uid,
                        error = if (request == null) "Request not found" else null
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}