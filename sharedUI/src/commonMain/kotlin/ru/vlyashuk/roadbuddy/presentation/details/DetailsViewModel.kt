package ru.vlyashuk.roadbuddy.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.DeleteRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage
import kotlin.coroutines.cancellation.CancellationException

data class DetailsUiState(
    val request: RoadRequest? = null,
    val isLoading: Boolean = false,
    val isOwner: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isCancelling: Boolean = false,
    val error: String? = null
)

class DetailsViewModel(
    private val getRequestByIdUseCase: GetRequestByIdUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateRequestUseCase: UpdateRequestUseCase,
    private val deleteRequestUseCase: DeleteRequestUseCase,
    private val requestId: String
) : ViewModel() {

    private val _localState = MutableStateFlow(DetailsLocalState())
    private val _uiState = MutableStateFlow(DetailsUiState(isLoading = true))
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private data class DetailsLocalState(
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val isCancelling: Boolean = false,
        val localError: String? = null
    )

    init {
        viewModelScope.launch {
            try {
                combine(
                    getRequestByIdUseCase(requestId),
                    getCurrentUserUseCase(),
                    _localState
                ) { result, user, local ->
                    if (local.isDeleted) {
                        return@combine DetailsUiState(isDeleted = true)
                    }
                    when (result) {
                        is AppResult.Success -> {
                            val request = result.value
                            DetailsUiState(
                                request = request,
                                isLoading = false,
                                isOwner = request != null && user != null && request.authorId == user.uid,
                                isDeleting = local.isDeleting,
                                isDeleted = local.isDeleted,
                                isCancelling = local.isCancelling,
                                error = local.localError ?: if (request == null) "Request not found" else null
                            )
                        }
                        is AppResult.Failure -> {
                            DetailsUiState(
                                isLoading = false,
                                isDeleting = local.isDeleting,
                                isDeleted = local.isDeleted,
                                isCancelling = local.isCancelling,
                                error = local.localError ?: result.error.toMessage()
                            )
                        }
                    }
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun cancelRequest() {
        val currentRequest = _uiState.value.request ?: return
        if (!_uiState.value.isOwner) return

        viewModelScope.launch {
            _localState.update { it.copy(isCancelling = true, localError = null) }
            val cancelledRequest = currentRequest.copy(status = RequestStatus.CANCELLED)
            when (val result = updateRequestUseCase(cancelledRequest)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isCancelling = false) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isCancelling = false, localError = result.error.toMessage()) }
                }
            }
        }
    }

    fun deleteRequest() {
        if (!_uiState.value.isOwner) return

        viewModelScope.launch {
            _localState.update { it.copy(isDeleting = true, localError = null) }
            when (val result = deleteRequestUseCase(requestId)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isDeleting = false, localError = result.error.toMessage()) }
                }
            }
        }
    }
}