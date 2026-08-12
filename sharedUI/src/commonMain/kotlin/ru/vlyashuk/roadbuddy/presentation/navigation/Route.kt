package ru.vlyashuk.roadbuddy.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route
    @Serializable
    data class Details(val requestId: String) : Route
    @Serializable
    data object Create : Route
}