package ru.vlyashuk.roadbuddy.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignOutUseCase
import ru.vlyashuk.roadbuddy.domain.util.geoRadiusContext
import ru.vlyashuk.roadbuddy.presentation.error.toMessage
import ru.vlyashuk.roadbuddy.presentation.location.requestCurrentLocation

enum class SortOption {
    NEWEST_FIRST, OLDEST_FIRST
}

data class NearbyState(
    val enabled: Boolean = false,
    val radiusKm: Double = 10.0,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val isLocating: Boolean = false
)

data class HomeUiState(
    val requests: List<RoadRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: RequestType? = null,
    val selectedStatus: RequestStatus? = null,
    val sortOption: SortOption = SortOption.NEWEST_FIRST,
    val nearby: NearbyState = NearbyState()
)

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class User(val user: AuthUser?) : AuthUiState
}

@OptIn(InternalCoroutinesApi::class)
class HomeViewModel(
    private val getRequestsUseCase: GetRequestsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _rawRequests = MutableStateFlow<List<RoadRequest>>(emptyList())
    private val _selectedType = MutableStateFlow<RequestType?>(null)
    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    private val _sortOption = MutableStateFlow<SortOption>(SortOption.NEWEST_FIRST)
    private val _nearby = MutableStateFlow(NearbyState())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        _rawRequests,
        _selectedType,
        _selectedStatus,
        _sortOption,
        _nearby,
        _isLoading,
        _error
    ) { array ->
        val raw = array[0] as List<RoadRequest>
        val type = array[1] as RequestType?
        val status = array[2] as RequestStatus?
        val sort = array[3] as SortOption
        val nearby = array[4] as NearbyState
        val loading = array[5] as Boolean
        val err = array[6] as String?

        val filtered = raw.filter { request ->
            (type == null || request.type == type) &&
            (status == null || request.status == status)
        }
        val geoFiltered = if (
            nearby.enabled && nearby.centerLat != null && nearby.centerLon != null
        ) {
            val ctx = geoRadiusContext(nearby.centerLat, nearby.centerLon, nearby.radiusKm)
            filtered.filter { ctx.contains(it) }
        } else {
            filtered
        }
        val sorted = when (sort) {
            SortOption.NEWEST_FIRST -> geoFiltered.sortedByDescending { it.createdAt }
            SortOption.OLDEST_FIRST -> geoFiltered.sortedBy { it.createdAt }
        }
        HomeUiState(
            requests = sorted,
            isLoading = loading,
            error = err,
            selectedType = type,
            selectedStatus = status,
            sortOption = sort,
            nearby = nearby
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    val authState: StateFlow<AuthUiState> =
        getCurrentUserUseCase()
            .map { AuthUiState.User(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AuthUiState.Loading
            )

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            getRequestsUseCase()
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

    fun setRadiusKm(radiusKm: Double) {
        _nearby.update { it.copy(radiusKm = radiusKm) }
    }

    fun setNearbyEnabled(enabled: Boolean) {
        _nearby.update { it.copy(enabled = enabled) }
    }

    fun locateNearMe() {
        viewModelScope.launch {
            _nearby.update { it.copy(isLocating = true) }
            try {
                val location = requestCurrentLocation() ?: return@launch
                _nearby.update {
                    it.copy(
                        enabled = true,
                        centerLat = location.first,
                        centerLon = location.second
                    )
                }
            } finally {
                _nearby.update { it.copy(isLocating = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}