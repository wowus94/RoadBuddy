package ru.vlyashuk.roadbuddy.presentation.history

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.history
import roadbuddy.sharedui.generated.resources.newest_first
import roadbuddy.sharedui.generated.resources.no_history_yet
import roadbuddy.sharedui.generated.resources.oldest_first
import roadbuddy.sharedui.generated.resources.requests_found
import roadbuddy.sharedui.generated.resources.retry
import roadbuddy.sharedui.generated.resources.sort_newest
import roadbuddy.sharedui.generated.resources.sort_oldest
import ru.vlyashuk.roadbuddy.presentation.components.RequestCard
import ru.vlyashuk.roadbuddy.presentation.home.SortOption
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.history),
                        style = RoadBuddyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = RoadBuddyTheme.colors.primary
                        )
                        state.error != null -> Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.error.orEmpty(),
                                color = RoadBuddyTheme.colors.error,
                                style = RoadBuddyTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(
                                    text = stringResource(Res.string.retry),
                                    color = RoadBuddyTheme.colors.primary
                                )
                            }
                        }

                        state.requests.isEmpty() -> Text(
                            text = stringResource(Res.string.no_history_yet),
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
        }
    }
}
