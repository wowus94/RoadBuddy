package ru.vlyashuk.roadbuddy.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.utils.DateTimeConverter
import ru.vlyashuk.roadbuddy.utils.rememberOpenMapAction

@Composable
fun DetailsScreen(
    requestId: String,
    onBack: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: DetailsViewModel = koinViewModel(parameters = { parametersOf(requestId) })
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isDeleted) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }

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
                    if (state.isOwner) {
                        Text(
                            text = "Edit",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { onNavigateToEdit(requestId) },
                            style = MaterialTheme.typography.labelLarge
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
                    isOwner = state.isOwner,
                    isHelper = state.isHelper,
                    isDeleting = state.isDeleting,
                    isCancelling = state.isCancelling,
                    isStatusUpdating = state.isStatusUpdating,
                    onCancelClick = viewModel::cancelRequest,
                    onDeleteClick = viewModel::deleteRequest,
                    onStatusUpdateClick = viewModel::updateRequestStatus,
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
    isOwner: Boolean,
    isHelper: Boolean,
    isDeleting: Boolean,
    isCancelling: Boolean,
    isStatusUpdating: Boolean,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusUpdateClick: (RequestStatus) -> Unit,
    modifier: Modifier = Modifier
) {

    val openMap = rememberOpenMapAction()

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
                DetailItem(
                    label = "Location",
                    value = "$lat, $lon",
                    modifier = Modifier.clickable { openMap(lat, lon, request.title) }
                )
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

        if (isOwner) {
            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Request") },
                    text = { Text("Are you sure you want to delete this request? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDeleteClick()
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (request.status) {
                    RequestStatus.OPEN -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.CANCELLED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text("Cancel request")
                        }
                    }
                    RequestStatus.IN_PROGRESS -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.COMPLETED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text("Mark as completed")
                        }

                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.CANCELLED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text("Cancel request")
                        }
                    }
                    RequestStatus.COMPLETED, RequestStatus.CANCELLED -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.OPEN) },
                            enabled = !isStatusUpdating && !isDeleting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp)
                                )
                            }
                            Text("Reopen request")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isStatusUpdating && !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text("Delete request")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (request.status) {
                    RequestStatus.OPEN -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.IN_PROGRESS) },
                            enabled = !isStatusUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text("Accept request / Help")
                        }
                    }
                    RequestStatus.IN_PROGRESS -> {
                        if (isHelper) {
                            Button(
                                onClick = { onStatusUpdateClick(RequestStatus.COMPLETED) },
                                enabled = !isStatusUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isStatusUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(20.dp),
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                                Text("Mark as completed")
                            }

                            OutlinedButton(
                                onClick = { onStatusUpdateClick(RequestStatus.OPEN) },
                                enabled = !isStatusUpdating,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isStatusUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(20.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text("Decline / Stop helping")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
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
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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