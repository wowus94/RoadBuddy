package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier
)