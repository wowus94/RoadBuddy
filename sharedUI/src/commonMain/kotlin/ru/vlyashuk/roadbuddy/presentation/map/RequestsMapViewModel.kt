package ru.vlyashuk.roadbuddy.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.vlyashuk.roadbuddy.domain.error.AppResult
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.RefreshRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.util.geoRadiusContext
import ru.vlyashuk.roadbuddy.domain.util.haversineKm
import ru.vlyashuk.roadbuddy.presentation.error.toMessage

data class RequestsMapUiState(
    val requests: List<RoadRequest> = emptyList(),
    val noLocationCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RequestsMapViewModel(
    private val getRequestsUseCase: GetRequestsUseCase,
    private val refreshRequestsUseCase: RefreshRequestsUseCase,
    private val centerLat: Double,
    private val centerLon: Double,
    private val radiusKm: Double
) : ViewModel() {

    init {
        viewModelScope.launch { refreshRequestsUseCase() }
    }

    private val hasCenter = centerLat != 0.0 || centerLon != 0.0
    private val hasRadius = radiusKm > 0.0

    val uiState: StateFlow<RequestsMapUiState> = getRequestsUseCase()
        .map { result ->
            when (result) {
                is AppResult.Success -> {
                    val withCoords = result.value.filter { it.latitude != null && it.longitude != null }
                    val noLocationCount = result.value.size - withCoords.size
                    var list = withCoords
                    if (hasCenter && hasRadius) {
                        val ctx = geoRadiusContext(centerLat, centerLon, radiusKm)
                        list = list.filter { ctx.contains(it) }
                    }
                    if (hasCenter) {
                        list = list.sortedBy {
                            haversineKm(
                                centerLat, centerLon,
                                it.latitude ?: 0.0, it.longitude ?: 0.0
                            )
                        }
                    }
                    RequestsMapUiState(
                        requests = list,
                        noLocationCount = noLocationCount
                    )
                }

                is AppResult.Failure -> RequestsMapUiState(error = result.error.toMessage())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RequestsMapUiState(isLoading = true)
        )
}
