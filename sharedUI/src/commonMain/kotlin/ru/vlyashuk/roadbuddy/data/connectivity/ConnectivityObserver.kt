package ru.vlyashuk.roadbuddy.data.connectivity

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
}

expect fun createConnectivityObserver(): ConnectivityObserver
