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

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.presentation.home.SortOption

data class MyRequestsUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: RequestType? = null,
    val selectedStatus: RequestStatus? = null,
    val sortOption: SortOption = SortOption.NEWEST_FIRST
)

class MyRequestsViewModel(
    private val getMyRequestsUseCase: GetMyRequestsUseCase
) : ViewModel() {

    private val _rawRequests = MutableStateFlow<List<RoadRequest>>(emptyList())
    private val _selectedType = MutableStateFlow<RequestType?>(null)
    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    private val _sortOption = MutableStateFlow<SortOption>(SortOption.NEWEST_FIRST)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MyRequestsUiState> = combine(
        _rawRequests,
        _selectedType,
        _selectedStatus,
        _sortOption,
        _isLoading,
        _error
    ) { array ->
        val raw = array[0] as List<RoadRequest>
        val type = array[1] as RequestType?
        val status = array[2] as RequestStatus?
        val sort = array[3] as SortOption
        val loading = array[4] as Boolean
        val err = array[5] as String?

        val filtered = raw.filter { request ->
            (type == null || request.type == type) &&
            (status == null || request.status == status)
        }
        val sorted = when (sort) {
            SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
            SortOption.OLDEST_FIRST -> filtered.sortedBy { it.createdAt }
        }
        MyRequestsUiState(
            requests = sorted,
            isLoading = loading,
            error = err,
            selectedType = type,
            selectedStatus = status,
            sortOption = sort
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MyRequestsUiState()
    )

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            getMyRequestsUseCase()
                .onStart { _isLoading.value = true; _error.value = null }
                .collect { result ->
                    _isLoading.value = false
                    when (result) {
                        is AppResult.Success -> {
                            _rawRequests.value = result.value
                        }

                        is AppResult.Failure -> {
                            _error.value = result.error.toMessage()
                        }
                    }
                }
        }
    }

    fun setTypeFilter(type: RequestType?) {
        _selectedType.value = type
    }

    fun setStatusFilter(status: RequestStatus?) {
        _selectedStatus.value = status
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
}
