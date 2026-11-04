package ru.vlyashuk.roadbuddy.presentation.myrequests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.back
import roadbuddy.sharedui.generated.resources.filters
import roadbuddy.sharedui.generated.resources.ic_arrow_back
import roadbuddy.sharedui.generated.resources.my_requests
import roadbuddy.sharedui.generated.resources.newest_first
import roadbuddy.sharedui.generated.resources.no_requests_match
import roadbuddy.sharedui.generated.resources.oldest_first
import roadbuddy.sharedui.generated.resources.requests_found
import roadbuddy.sharedui.generated.resources.sort_newest
import roadbuddy.sharedui.generated.resources.sort_oldest
import roadbuddy.sharedui.generated.resources.you_have_no_requests_yet
import ru.vlyashuk.roadbuddy.presentation.components.FilterBottomSheet
import ru.vlyashuk.roadbuddy.presentation.components.RequestCard
import ru.vlyashuk.roadbuddy.presentation.home.SortOption
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

@Composable
fun MyRequestsScreen(
    viewModel: MyRequestsViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var isFilterSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.my_requests),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RoadBuddyTheme.spacing.medium, vertical = RoadBuddyTheme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.requests_found, state.requests.size.toString()),
                    style = RoadBuddyTheme.typography.bodyMedium,
                    color = RoadBuddyTheme.colors.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isSortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { isSortMenuExpanded = true },
                            modifier = Modifier.height(36.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoadBuddyTheme.colors.secondaryContainer,
                                contentColor = RoadBuddyTheme.colors.onSecondaryContainer
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = RoadBuddyTheme.spacing.medium, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.sortOption == SortOption.NEWEST_FIRST) {
                                    stringResource(Res.string.sort_newest)
                                } else {
                                    stringResource(Res.string.sort_oldest)
                                },
                                style = RoadBuddyTheme.typography.labelMedium
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.newest_first), style = RoadBuddyTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NEWEST_FIRST)
                                    isSortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.oldest_first), style = RoadBuddyTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.OLDEST_FIRST)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { isFilterSheetOpen = true },
                        modifier = Modifier.height(36.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoadBuddyTheme.colors.secondaryContainer,
                            contentColor = RoadBuddyTheme.colors.onSecondaryContainer
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = RoadBuddyTheme.spacing.medium, vertical = 4.dp)
                    ) {
                        val filterCount = (if (state.selectedType != null) 1 else 0) + (if (state.selectedStatus != null) 1 else 0)
                        Text(
                            text = if (filterCount > 0) {
                                "${stringResource(Res.string.filters)} ($filterCount)"
                            } else {
                                stringResource(Res.string.filters)
                            },
                            style = RoadBuddyTheme.typography.labelMedium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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

                    state.requests.isEmpty() -> Text(
                        text = if (state.selectedType != null || state.selectedStatus != null) {
                            stringResource(Res.string.no_requests_match)
                        } else {
                            stringResource(Res.string.you_have_no_requests_yet)
                        },
                        style = RoadBuddyTheme.typography.bodyMedium,
                        color = RoadBuddyTheme.colors.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = RoadBuddyTheme.spacing.large)
                    ) {
                        items(state.requests) { request ->
                            RequestCard(
                                request = request,
                                onClick = { onNavigateToDetails(request.id) }
                            )
                        }
                    }
                }
            }
        }

        if (isFilterSheetOpen) {
            FilterBottomSheet(
                onDismissRequest = { isFilterSheetOpen = false },
                selectedType = state.selectedType,
                onTypeSelected = { viewModel.setTypeFilter(it) },
                selectedStatus = state.selectedStatus,
                onStatusSelected = { viewModel.setStatusFilter(it) }
            )
        }
    }
}
