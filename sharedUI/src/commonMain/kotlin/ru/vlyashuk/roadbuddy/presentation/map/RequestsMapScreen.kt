package ru.vlyashuk.roadbuddy.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.back
import roadbuddy.sharedui.generated.resources.ic_arrow_back
import roadbuddy.sharedui.generated.resources.requests_map
import roadbuddy.sharedui.generated.resources.requests_without_location
import roadbuddy.sharedui.generated.resources.you_are_here
import ru.vlyashuk.roadbuddy.presentation.components.MapMarker
import ru.vlyashuk.roadbuddy.presentation.components.MapPicker
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

private const val USER_MARKER_ID = "__user__"

@Composable
fun RequestsMapScreen(
    centerLat: Double,
    centerLon: Double,
    radiusKm: Double,
    onBack: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {},
    viewModel: RequestsMapViewModel = koinViewModel(
        parameters = { parametersOf(centerLat, centerLon, radiusKm) }
    )
) {
    val state by viewModel.uiState.collectAsState()
    val hasCenter = centerLat != 0.0 || centerLon != 0.0
    val userTitle = stringResource(Res.string.you_are_here)

    val markers = remember(state.requests, hasCenter, userTitle) {
        buildList {
            if (hasCenter) {
                add(
                    MapMarker(
                        id = USER_MARKER_ID,
                        latitude = centerLat,
                        longitude = centerLon,
                        title = userTitle
                    )
                )
            }
            state.requests.forEach { request ->
                val lat = request.latitude ?: return@forEach
                val lon = request.longitude ?: return@forEach
                val h = request.id.hashCode()
                val jLat = ((h and 0xFF) / 255.0 - 0.5) * 0.0008
                val jLon = (((h shr 8) and 0xFF) / 255.0 - 0.5) * 0.0008
                add(
                    MapMarker(
                        id = request.id,
                        latitude = lat + jLat,
                        longitude = lon + jLon,
                        title = request.title
                    )
                )
            }
        }
    }

    val focusPoints = remember(markers) {
        markers.map { it.latitude to it.longitude }
    }

    Scaffold(
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.requests_map),
                        style = RoadBuddyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.back),
                            tint = RoadBuddyTheme.colors.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RoadBuddyTheme.colors.primary
                )

                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    color = RoadBuddyTheme.colors.error,
                    style = RoadBuddyTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    MapPicker(
                        latitude = if (hasCenter) centerLat else null,
                        longitude = if (hasCenter) centerLon else null,
                        onLocationSelected = { _, _ -> },
                        markers = markers,
                        onMarkerClick = { id ->
                            if (id != USER_MARKER_ID) onNavigateToDetails(id)
                        },
                        focusPoints = focusPoints,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (state.noLocationCount > 0) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = RoadBuddyTheme.colors.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.requests_without_location,
                                    state.noLocationCount
                                ),
                                style = RoadBuddyTheme.typography.bodySmall,
                                color = RoadBuddyTheme.colors.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = RoadBuddyTheme.spacing.medium,
                                    vertical = RoadBuddyTheme.spacing.small
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
