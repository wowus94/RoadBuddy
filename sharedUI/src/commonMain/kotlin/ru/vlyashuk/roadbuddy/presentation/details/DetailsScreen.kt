package ru.vlyashuk.roadbuddy.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.utils.DateTimeConverter

@Composable
fun DetailsScreen(
    requestId: String,
    onBack: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: DetailsViewModel = koinViewModel(parameters = { parametersOf(requestId) })
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = {
                    Text(
                        text = "←",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(onClick = onBack),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                actions = {
                    Text(
                        text = "Edit",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToEdit(requestId) },
                        style = MaterialTheme.typography.labelLarge
                    )
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
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = state.error.toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                state.request == null -> Text(
                    text = "Request not found",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> RequestDetails(
                    request = state.request!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RequestDetails(
    request: RoadRequest,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = request.title,
            style = MaterialTheme.typography.headlineMedium
        )

        StatusBadge(status = request.status)

        DetailItem(label = "Description", value = request.description)
        DetailItem(label = "Type", value = request.type.name)
        DetailItem(label = "Author", value = request.authorName)
        DetailItem(label = "Contact", value = request.contact)

        request.latitude?.let { lat ->
            request.longitude?.let { lon ->
                DetailItem(label = "Location", value = "$lat, $lon")
            }
        }

        DetailItem(
            label = "Created",
            value = DateTimeConverter.format(request.createdAt)
        )
        DetailItem(
            label = "Updated",
            value = DateTimeConverter.format(request.updatedAt)
        )
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val color = when (status) {
        RequestStatus.OPEN -> MaterialTheme.colorScheme.primary
        RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        RequestStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    Card {
        Text(
            text = status.name,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}