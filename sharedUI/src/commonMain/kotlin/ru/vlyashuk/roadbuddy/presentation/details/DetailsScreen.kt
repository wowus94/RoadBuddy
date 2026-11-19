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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.action_cancel_request
import roadbuddy.sharedui.generated.resources.action_decline_request
import roadbuddy.sharedui.generated.resources.action_delete_request
import roadbuddy.sharedui.generated.resources.action_mark_completed
import roadbuddy.sharedui.generated.resources.action_reopen_request
import roadbuddy.sharedui.generated.resources.action_respond
import roadbuddy.sharedui.generated.resources.back
import roadbuddy.sharedui.generated.resources.cancel
import roadbuddy.sharedui.generated.resources.delete
import roadbuddy.sharedui.generated.resources.delete_request_desc
import roadbuddy.sharedui.generated.resources.delete_request_title
import roadbuddy.sharedui.generated.resources.edit
import roadbuddy.sharedui.generated.resources.ic_arrow_back
import roadbuddy.sharedui.generated.resources.label_author
import roadbuddy.sharedui.generated.resources.label_contact
import roadbuddy.sharedui.generated.resources.label_created
import roadbuddy.sharedui.generated.resources.label_description
import roadbuddy.sharedui.generated.resources.label_helper
import roadbuddy.sharedui.generated.resources.label_location
import roadbuddy.sharedui.generated.resources.label_type
import roadbuddy.sharedui.generated.resources.label_updated
import roadbuddy.sharedui.generated.resources.no_responses_yet
import roadbuddy.sharedui.generated.resources.request_details
import roadbuddy.sharedui.generated.resources.request_not_found
import roadbuddy.sharedui.generated.resources.respond_message_hint
import roadbuddy.sharedui.generated.resources.responses_title
import roadbuddy.sharedui.generated.resources.select_helper
import roadbuddy.sharedui.generated.resources.submit_response
import roadbuddy.sharedui.generated.resources.withdraw_response
import roadbuddy.sharedui.generated.resources.you_responded
import ru.vlyashuk.roadbuddy.domain.model.RequestResponse
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.presentation.util.toLocalizedName
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme
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
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.request_details),
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
                },
                actions = {
                    if (state.isOwner) {
                        Text(
                            text = stringResource(Res.string.edit),
                            modifier = Modifier
                                .padding(horizontal = RoadBuddyTheme.spacing.medium)
                                .clickable { onNavigateToEdit(requestId) },
                            style = RoadBuddyTheme.typography.labelLarge.copy(
                                color = RoadBuddyTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
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
                    text = state.error.toString(),
                    color = RoadBuddyTheme.colors.error,
                    style = RoadBuddyTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )

                state.request == null -> Text(
                    text = stringResource(Res.string.request_not_found),
                    style = RoadBuddyTheme.typography.bodyMedium,
                    color = RoadBuddyTheme.colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> RequestDetails(
                    request = state.request!!,
                    responses = state.responses,
                    isOwner = state.isOwner,
                    isHelper = state.isHelper,
                    hasResponded = state.hasResponded,
                    isDeleting = state.isDeleting,
                    isCancelling = state.isCancelling,
                    isStatusUpdating = state.isStatusUpdating,
                    isResponding = state.isResponding,
                    isWithdrawing = state.isWithdrawing,
                    isSelectingHelper = state.isSelectingHelper,
                    onCancelClick = viewModel::cancelRequest,
                    onDeleteClick = viewModel::deleteRequest,
                    onStatusUpdateClick = viewModel::updateRequestStatus,
                    onRespondSubmit = viewModel::submitResponse,
                    onWithdrawResponse = viewModel::withdrawResponse,
                    onSelectHelper = viewModel::selectHelper,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(RoadBuddyTheme.spacing.medium)
                )
            }
        }
    }
}

@Composable
private fun RequestDetails(
    request: RoadRequest,
    responses: List<RequestResponse>,
    isOwner: Boolean,
    isHelper: Boolean,
    hasResponded: Boolean,
    isDeleting: Boolean,
    isCancelling: Boolean,
    isStatusUpdating: Boolean,
    isResponding: Boolean,
    isWithdrawing: Boolean,
    isSelectingHelper: Boolean,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusUpdateClick: (RequestStatus) -> Unit,
    onRespondSubmit: (String) -> Unit,
    onWithdrawResponse: () -> Unit,
    onSelectHelper: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val openMap = rememberOpenMapAction()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.medium)
    ) {
        Text(
            text = request.title,
            style = RoadBuddyTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = RoadBuddyTheme.colors.onBackground
        )

        StatusBadge(status = request.status)

        DetailItem(label = stringResource(Res.string.label_description), value = request.description)
        DetailItem(label = stringResource(Res.string.label_type), value = request.type.toLocalizedName())
        DetailItem(label = stringResource(Res.string.label_author), value = request.authorName)
        DetailItem(label = stringResource(Res.string.label_contact), value = request.contact)

        request.latitude?.let { lat ->
            request.longitude?.let { lon ->
                DetailItem(
                    label = stringResource(Res.string.label_location),
                    value = "$lat, $lon",
                    modifier = Modifier.clickable { openMap(lat, lon, request.title) }
                )
            }
        }

        DetailItem(
            label = stringResource(Res.string.label_created),
            value = DateTimeConverter.format(request.createdAt)
        )
        DetailItem(
            label = stringResource(Res.string.label_updated),
            value = DateTimeConverter.format(request.updatedAt)
        )

        request.helperId.takeIf { it.isNotBlank() }?.let { helperId ->
            val helperName = responses
                .firstOrNull { it.helperId == helperId }
                ?.helperName
                ?.takeIf { it.isNotBlank() }
                ?: helperId
            DetailItem(
                label = stringResource(Res.string.label_helper),
                value = helperName
            )
        }

        if (isOwner) {
            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(Res.string.delete_request_title), style = RoadBuddyTheme.typography.titleMedium) },
                    text = { Text(stringResource(Res.string.delete_request_desc), style = RoadBuddyTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDeleteClick()
                            }
                        ) {
                            Text(stringResource(Res.string.delete), color = RoadBuddyTheme.colors.error, style = RoadBuddyTheme.typography.labelLarge)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(Res.string.cancel), style = RoadBuddyTheme.typography.labelLarge)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = RoadBuddyTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small)
            ) {
                if (request.status == RequestStatus.OPEN || responses.isNotEmpty()) {
                    ResponsesSection(
                        responses = responses,
                        canSelect = request.status == RequestStatus.OPEN,
                        isSelecting = isSelectingHelper,
                        onSelect = onSelectHelper
                    )
                }

                when (request.status) {
                    RequestStatus.OPEN -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.CANCELLED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoadBuddyTheme.colors.errorContainer,
                                contentColor = RoadBuddyTheme.colors.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = RoadBuddyTheme.spacing.small)
                                        .size(20.dp),
                                    color = RoadBuddyTheme.colors.onErrorContainer
                                )
                            }
                            Text(stringResource(Res.string.action_cancel_request))
                        }
                    }
                    RequestStatus.IN_PROGRESS -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.COMPLETED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoadBuddyTheme.colors.primary,
                                contentColor = RoadBuddyTheme.colors.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = RoadBuddyTheme.spacing.small)
                                        .size(20.dp),
                                    color = RoadBuddyTheme.colors.onPrimary
                                )
                            }
                            Text(stringResource(Res.string.action_mark_completed))
                        }

                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.CANCELLED) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoadBuddyTheme.colors.errorContainer,
                                contentColor = RoadBuddyTheme.colors.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = RoadBuddyTheme.spacing.small)
                                        .size(20.dp),
                                    color = RoadBuddyTheme.colors.onErrorContainer
                                )
                            }
                            Text(stringResource(Res.string.action_cancel_request))
                        }
                    }
                    RequestStatus.COMPLETED, RequestStatus.CANCELLED -> {
                        Button(
                            onClick = { onStatusUpdateClick(RequestStatus.OPEN) },
                            enabled = !isStatusUpdating && !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoadBuddyTheme.colors.primary,
                                contentColor = RoadBuddyTheme.colors.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isStatusUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = RoadBuddyTheme.spacing.small)
                                        .size(20.dp),
                                    color = RoadBuddyTheme.colors.onPrimary
                                )
                            }
                            Text(stringResource(Res.string.action_reopen_request))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isStatusUpdating && !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoadBuddyTheme.colors.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = RoadBuddyTheme.spacing.small)
                                .size(20.dp),
                            color = RoadBuddyTheme.colors.error
                        )
                    }
                    Text(stringResource(Res.string.action_delete_request))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = RoadBuddyTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small)
            ) {
                when (request.status) {
                    RequestStatus.OPEN -> {
                        if (hasResponded) {
                            Text(
                                text = stringResource(Res.string.you_responded),
                                style = RoadBuddyTheme.typography.bodyMedium,
                                color = RoadBuddyTheme.colors.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = onWithdrawResponse,
                                enabled = !isWithdrawing && !isStatusUpdating,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RoadBuddyTheme.colors.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isWithdrawing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = RoadBuddyTheme.spacing.small)
                                            .size(20.dp),
                                        color = RoadBuddyTheme.colors.error
                                    )
                                }
                                Text(stringResource(Res.string.withdraw_response))
                            }
                        } else {
                            var showRespondDialog by remember { mutableStateOf(false) }

                            Button(
                                onClick = { showRespondDialog = true },
                                enabled = !isStatusUpdating && !isResponding,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoadBuddyTheme.colors.primary,
                                    contentColor = RoadBuddyTheme.colors.onPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isResponding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = RoadBuddyTheme.spacing.small)
                                            .size(20.dp),
                                        color = RoadBuddyTheme.colors.onPrimary
                                    )
                                }
                                Text(stringResource(Res.string.action_respond))
                            }

                            if (showRespondDialog) {
                                RespondDialog(
                                    isResponding = isResponding,
                                    onDismiss = { showRespondDialog = false },
                                    onSubmit = { message ->
                                        showRespondDialog = false
                                        onRespondSubmit(message)
                                    }
                                )
                            }
                        }
                    }
                    RequestStatus.IN_PROGRESS -> {
                        if (isHelper) {
                            Button(
                                onClick = { onStatusUpdateClick(RequestStatus.COMPLETED) },
                                enabled = !isStatusUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoadBuddyTheme.colors.secondary,
                                    contentColor = RoadBuddyTheme.colors.onSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isStatusUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = RoadBuddyTheme.spacing.small)
                                            .size(20.dp),
                                        color = RoadBuddyTheme.colors.onSecondary
                                    )
                                }
                                Text(stringResource(Res.string.action_mark_completed))
                            }

                            OutlinedButton(
                                onClick = { onStatusUpdateClick(RequestStatus.OPEN) },
                                enabled = !isStatusUpdating,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RoadBuddyTheme.colors.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isStatusUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = RoadBuddyTheme.spacing.small)
                                            .size(20.dp),
                                        color = RoadBuddyTheme.colors.error
                                    )
                                }
                                Text(stringResource(Res.string.action_decline_request))
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
        RequestStatus.OPEN -> RoadBuddyTheme.colors.primary
        RequestStatus.IN_PROGRESS -> RoadBuddyTheme.colors.tertiary
        RequestStatus.COMPLETED -> RoadBuddyTheme.colors.secondary
        RequestStatus.CANCELLED -> RoadBuddyTheme.colors.error
    }
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = status.toLocalizedName(),
            color = color,
            style = RoadBuddyTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = RoadBuddyTheme.spacing.medium, vertical = RoadBuddyTheme.spacing.small)
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
            style = RoadBuddyTheme.typography.labelMedium,
            color = RoadBuddyTheme.colors.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = RoadBuddyTheme.typography.bodyLarge,
            color = RoadBuddyTheme.colors.onSurface
        )
    }
}

@Composable
private fun ResponsesSection(
    responses: List<RequestResponse>,
    canSelect: Boolean,
    isSelecting: Boolean,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.responses_title),
            style = RoadBuddyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RoadBuddyTheme.colors.onBackground
        )

        if (responses.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_responses_yet),
                style = RoadBuddyTheme.typography.bodyMedium,
                color = RoadBuddyTheme.colors.onSurfaceVariant
            )
        } else {
            responses.forEach { response ->
                ResponseCard(
                    response = response,
                    canSelect = canSelect,
                    isSelecting = isSelecting,
                    onSelect = { onSelect(response.helperId) }
                )
            }
        }
    }
}

@Composable
private fun ResponseCard(
    response: RequestResponse,
    canSelect: Boolean,
    isSelecting: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = RoadBuddyTheme.colors.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoadBuddyTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.extraSmall)
        ) {
            Text(
                text = response.helperName.ifBlank { response.helperId },
                style = RoadBuddyTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = RoadBuddyTheme.colors.onSurface
            )

            if (response.message.isNotBlank()) {
                Text(
                    text = response.message,
                    style = RoadBuddyTheme.typography.bodyMedium,
                    color = RoadBuddyTheme.colors.onSurface
                )
            }

            Text(
                text = DateTimeConverter.format(response.createdAt),
                style = RoadBuddyTheme.typography.labelSmall,
                color = RoadBuddyTheme.colors.onSurfaceVariant
            )

            if (canSelect) {
                Button(
                    onClick = onSelect,
                    enabled = !isSelecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoadBuddyTheme.colors.primary,
                        contentColor = RoadBuddyTheme.colors.onPrimary
                    )
                ) {
                    if (isSelecting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = RoadBuddyTheme.spacing.small)
                                .size(16.dp),
                            color = RoadBuddyTheme.colors.onPrimary
                        )
                    }
                    Text(stringResource(Res.string.select_helper))
                }
            }
        }
    }
}

@Composable
private fun RespondDialog(
    isResponding: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.action_respond),
                style = RoadBuddyTheme.typography.titleMedium
            )
        },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = {
                    Text(
                        text = stringResource(Res.string.respond_message_hint),
                        style = RoadBuddyTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(message) },
                enabled = !isResponding
            ) {
                Text(
                    text = stringResource(Res.string.submit_response),
                    style = RoadBuddyTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.cancel),
                    style = RoadBuddyTheme.typography.labelLarge
                )
            }
        }
    )
}