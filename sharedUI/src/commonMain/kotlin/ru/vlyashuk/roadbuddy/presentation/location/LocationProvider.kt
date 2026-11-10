package ru.vlyashuk.roadbuddy.presentation.location

import androidx.compose.runtime.Composable

expect suspend fun requestCurrentLocation(): Pair<Double, Double>?

@Composable
expect fun rememberLocationPermissionRequester(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
): () -> Unit
