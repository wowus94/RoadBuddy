package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String = ""
)

@Composable
expect fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    onMarkerClick: (id: String) -> Unit = {},
    focusPoints: List<Pair<Double, Double>> = emptyList()
)
