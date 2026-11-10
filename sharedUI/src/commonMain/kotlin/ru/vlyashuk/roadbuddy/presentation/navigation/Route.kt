package ru.vlyashuk.roadbuddy.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Splash : Route
    @Serializable
    data object Login : Route
    @Serializable
    data object Register : Route
    @Serializable
    data object Home : Route
    @Serializable
    data class Details(val requestId: String) : Route
    @Serializable
    data object Create : Route
    @Serializable
    data object MyRequests : Route
    @Serializable
    data class Edit(val requestId: String) : Route
    @Serializable
    data class RequestsMap(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val radiusKm: Double = 0.0
    ) : Route
}