package ru.vlyashuk.roadbuddy.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.RefreshRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.presentation.error.toMessage
import ru.vlyashuk.roadbuddy.presentation.home.SortOption

data class HistoryUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val sortOption: SortOption = SortOption.NEWEST_FIRST
)

class HistoryViewModel(
    private val getRequestsUseCase: GetRequestsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val refreshRequestsUseCase: RefreshRequestsUseCase
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.NEWEST_FIRST)
    private val _isLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HistoryUiState> = combine(
        getRequestsUseCase(),
        getCurrentUserUseCase(),
        _sortOption,
        _isLoading,
        _isRefreshing,
        _error
    ) { array ->
        val result = array[0] as AppResult<List<RoadRequest>>
        val user = array[1] as AuthUser?
        val sort = array[2] as SortOption
        val loading = array[3] as Boolean
        val refreshing = array[4] as Boolean
        val err = array[5] as String?

        val history = when (result) {
            is AppResult.Success -> result.value.filter {
                it.status in FINISHED_STATUSES &&
                    (it.authorId == user?.uid || it.helperId == user?.uid)
            }
            is AppResult.Failure -> emptyList()
        }
        val sorted = when (sort) {
            SortOption.NEWEST_FIRST -> history.sortedByDescending { it.updatedAt }
            SortOption.OLDEST_FIRST -> history.sortedBy { it.updatedAt }
        }
        HistoryUiState(
            requests = sorted,
            isLoading = loading,
            isRefreshing = refreshing,
            error = if (result is AppResult.Failure) result.error.toMessage() else err,
            sortOption = sort
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState(isLoading = true)
    )

    init {
        viewModelScope.launch { refreshRequestsUseCase() }
        viewModelScope.launch {
            getRequestsUseCase().collect {
                _isLoading.value = false
                if (it is AppResult.Failure) _error.value = it.error.toMessage()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshRequestsUseCase()
            _isRefreshing.value = false
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    private companion object {
        val FINISHED_STATUSES = setOf(RequestStatus.COMPLETED, RequestStatus.CANCELLED)
    }
}
