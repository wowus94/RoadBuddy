package ru.vlyashuk.roadbuddy.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.AuthUser
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestsPage
import ru.vlyashuk.roadbuddy.domain.model.RequestsPageQuery
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsPageUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.ObserveRequestsPageUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.RefreshRequestsUseCase
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
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = false,
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getRequestsPageUseCase: GetRequestsPageUseCase,
    private val observeRequestsPageUseCase: ObserveRequestsPageUseCase,
    private val refreshRequestsUseCase: RefreshRequestsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _rawRequests = MutableStateFlow<List<RoadRequest>>(emptyList())
    private val _olderRequests = MutableStateFlow<List<RoadRequest>>(emptyList())
    private val _selectedType = MutableStateFlow<RequestType?>(null)
    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    private val _sortOption = MutableStateFlow<SortOption>(SortOption.NEWEST_FIRST)
    private val _nearby = MutableStateFlow(NearbyState())
    private val _isLoading = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _hasMore = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private var liveRequests: List<RoadRequest> = emptyList()
    private var liveLastId: String? = null
    private var liveNextOffset: Int? = null
    private var olderNextOffset: Int? = null
    private val pageSize = 20

    private val queryParams = combine(
        _selectedType, _selectedStatus, _sortOption
    ) { type, status, sort ->
        RequestsPageQuery(
            type = type,
            status = status,
            sortDescending = sort == SortOption.NEWEST_FIRST
        )
    }.distinctUntilChanged()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        _rawRequests,
        _selectedType,
        _selectedStatus,
        _sortOption,
        _nearby,
        _isLoading,
        _isLoadingMore,
        _isRefreshing,
        _hasMore,
        _error
    ) { array ->
        val raw = array[0] as List<RoadRequest>
        val type = array[1] as RequestType?
        val status = array[2] as RequestStatus?
        val sort = array[3] as SortOption
        val nearby = array[4] as NearbyState
        val loading = array[5] as Boolean
        val loadingMore = array[6] as Boolean
        val refreshing = array[7] as Boolean
        val hasMore = array[8] as Boolean
        val err = array[9] as String?

        val geoFiltered = if (
            nearby.enabled && nearby.centerLat != null && nearby.centerLon != null
        ) {
            val ctx = geoRadiusContext(nearby.centerLat, nearby.centerLon, nearby.radiusKm)
            raw.filter { ctx.contains(it) }
        } else {
            raw
        }
        val sorted = when (sort) {
            SortOption.NEWEST_FIRST -> geoFiltered.sortedByDescending { it.createdAt }
            SortOption.OLDEST_FIRST -> geoFiltered.sortedBy { it.createdAt }
        }
        HomeUiState(
            requests = sorted,
            isLoading = loading,
            isLoadingMore = loadingMore,
            isRefreshing = refreshing,
            hasMore = hasMore,
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
        viewModelScope.launch {
            refreshRequestsUseCase()
        }
        viewModelScope.launch {
            queryParams
                .onEach {
                    liveRequests = emptyList()
                    liveLastId = null
                    liveNextOffset = null
                    _olderRequests.value = emptyList()
                    olderNextOffset = null
                    _hasMore.value = false
                    _isLoading.value = true
                    _error.value = null
                }
                .flatMapLatest { params -> observeRequestsPageUseCase(pageSize, 0, params) }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> onLivePage(result.value)
                        is AppResult.Failure -> {
                            _error.value = result.error.toMessage()
                            _isLoading.value = false
                        }
                    }
                }
        }
    }

    private fun onLivePage(page: RequestsPage) {
        val lastId = page.requests.lastOrNull()?.id
        val contentShifted = lastId != liveLastId
        liveRequests = page.requests
        liveLastId = lastId
        liveNextOffset = page.nextOffset
        if (contentShifted) {
            _olderRequests.value = emptyList()
            olderNextOffset = null
            _hasMore.value = page.hasMore
        }
        _rawRequests.value = (page.requests + _olderRequests.value)
            .distinctBy { it.id }
        _isLoading.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshRequestsUseCase()
            _isRefreshing.value = false
        }
    }

    fun loadNextPage() {
        if (!_hasMore.value || _isLoadingMore.value) return
        val offset = olderNextOffset ?: liveNextOffset ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val query = RequestsPageQuery(
                type = _selectedType.value,
                status = _selectedStatus.value,
                sortDescending = _sortOption.value == SortOption.NEWEST_FIRST
            )
            when (val result = getRequestsPageUseCase(pageSize, offset, query)) {
                is AppResult.Success -> {
                    _olderRequests.update { it + result.value.requests }
                    olderNextOffset = result.value.nextOffset
                    _hasMore.value = result.value.hasMore
                    _rawRequests.value = (liveRequests + _olderRequests.value)
                        .distinctBy { it.id }
                }
                is AppResult.Failure -> {
                    _error.value = result.error.toMessage()
                }
            }
            _isLoadingMore.value = false
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