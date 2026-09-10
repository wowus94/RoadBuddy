package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173

@Composable
actual fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(latitude ?: DEFAULT_LAT, longitude ?: DEFAULT_LON),
            15f
        )
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(latitude, longitude),
                15f
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { onLocationSelected(it.latitude, it.longitude) }
    ) {
        if (latitude != null && longitude != null) {
            Marker(
                state = rememberUpdatedMarkerState(position = LatLng(latitude, longitude))
            )
        }
    }
}