package ru.vlyashuk.roadbuddy.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberOpenMapAction(): (lat: Double, lon: Double, label: String?) -> Unit