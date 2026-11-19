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
import ru.vlyashuk.roadbuddy.domain.model.RequestResponse
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.DeleteRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.DeleteResponseUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetResponsesUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.RefreshRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.RespondToRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.SelectHelperUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestStatusUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage
import kotlin.coroutines.cancellation.CancellationException

data class DetailsUiState(
    val request: RoadRequest? = null,
    val responses: List<RequestResponse> = emptyList(),
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    val isOwner: Boolean = false,
    val isHelper: Boolean = false,
    val hasResponded: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isCancelling: Boolean = false,
    val isStatusUpdating: Boolean = false,
    val isResponding: Boolean = false,
    val isWithdrawing: Boolean = false,
    val isSelectingHelper: Boolean = false,
    val error: String? = null
)

class DetailsViewModel(
    private val getRequestByIdUseCase: GetRequestByIdUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateRequestStatusUseCase: UpdateRequestStatusUseCase,
    private val deleteRequestUseCase: DeleteRequestUseCase,
    private val refreshRequestUseCase: RefreshRequestUseCase,
    private val getResponsesUseCase: GetResponsesUseCase,
    private val respondToRequestUseCase: RespondToRequestUseCase,
    private val selectHelperUseCase: SelectHelperUseCase,
    private val deleteResponseUseCase: DeleteResponseUseCase,
    private val requestId: String
) : ViewModel() {

    private val _localState = MutableStateFlow(DetailsLocalState())
    private val _uiState = MutableStateFlow(DetailsUiState(isLoading = true))
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private data class DetailsLocalState(
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val isCancelling: Boolean = false,
        val isStatusUpdating: Boolean = false,
        val isResponding: Boolean = false,
        val isWithdrawing: Boolean = false,
        val isSelectingHelper: Boolean = false,
        val localError: String? = null
    )

    init {
        viewModelScope.launch { refreshRequestUseCase(requestId) }
        viewModelScope.launch {
            try {
                combine(
                    getRequestByIdUseCase(requestId),
                    getCurrentUserUseCase(),
                    getResponsesUseCase(requestId),
                    _localState
                ) { result, user, responsesResult, local ->
                    if (local.isDeleted) {
                        return@combine DetailsUiState(isDeleted = true)
                    }
                    val responses = (responsesResult as? AppResult.Success)?.value.orEmpty()
                    when (result) {
                        is AppResult.Success -> {
                            val request = result.value
                            DetailsUiState(
                                request = request,
                                responses = responses,
                                currentUserId = user?.uid,
                                isLoading = false,
                                isOwner = request != null && user != null && request.authorId == user.uid,
                                isHelper = request != null && user != null && request.helperId == user.uid,
                                hasResponded = user != null && responses.any { it.helperId == user.uid },
                                isDeleting = local.isDeleting,
                                isDeleted = local.isDeleted,
                                isCancelling = local.isCancelling,
                                isStatusUpdating = local.isStatusUpdating,
                                isResponding = local.isResponding,
                                isWithdrawing = local.isWithdrawing,
                                isSelectingHelper = local.isSelectingHelper,
                                error = local.localError ?: if (request == null) "Request not found" else null
                            )
                        }
                        is AppResult.Failure -> {
                            DetailsUiState(
                                responses = responses,
                                currentUserId = user?.uid,
                                isLoading = false,
                                isDeleting = local.isDeleting,
                                isDeleted = local.isDeleted,
                                isCancelling = local.isCancelling,
                                isStatusUpdating = local.isStatusUpdating,
                                isResponding = local.isResponding,
                                isWithdrawing = local.isWithdrawing,
                                isSelectingHelper = local.isSelectingHelper,
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
        updateRequestStatus(RequestStatus.CANCELLED)
    }

    fun updateRequestStatus(newStatus: RequestStatus) {
        val currentRequest = _uiState.value.request ?: return

        viewModelScope.launch {
            _localState.update { it.copy(isStatusUpdating = true, localError = null) }
            val updatedRequest = currentRequest.copy(status = newStatus)
            when (val result = updateRequestStatusUseCase(updatedRequest)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isStatusUpdating = false) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isStatusUpdating = false, localError = result.error.toMessage()) }
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

    fun submitResponse(message: String) {
        viewModelScope.launch {
            _localState.update { it.copy(isResponding = true, localError = null) }
            when (val result = respondToRequestUseCase(requestId, message)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isResponding = false) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isResponding = false, localError = result.error.toMessage()) }
                }
            }
        }
    }

    fun withdrawResponse() {
        val uid = _uiState.value.currentUserId ?: return
        val response = _uiState.value.responses.firstOrNull { it.helperId == uid } ?: return

        viewModelScope.launch {
            _localState.update { it.copy(isWithdrawing = true, localError = null) }
            when (val result = deleteResponseUseCase(requestId, response.id)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isWithdrawing = false) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isWithdrawing = false, localError = result.error.toMessage()) }
                }
            }
        }
    }

    fun selectHelper(helperId: String) {
        viewModelScope.launch {
            _localState.update { it.copy(isSelectingHelper = true, localError = null) }
            when (val result = selectHelperUseCase(requestId, helperId)) {
                is AppResult.Success -> {
                    _localState.update { it.copy(isSelectingHelper = false) }
                }
                is AppResult.Failure -> {
                    _localState.update { it.copy(isSelectingHelper = false, localError = result.error.toMessage()) }
                }
            }
        }
    }
}
