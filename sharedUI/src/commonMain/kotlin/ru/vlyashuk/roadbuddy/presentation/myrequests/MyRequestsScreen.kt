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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.vlyashuk.roadbuddy.presentation.components.FilterBottomSheet
import ru.vlyashuk.roadbuddy.presentation.components.RequestCard
import ru.vlyashuk.roadbuddy.presentation.home.SortOption

@Composable
fun MyRequestsScreen(
    viewModel: MyRequestsViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var isFilterSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My requests") },
                navigationIcon = {
                    Text(
                        text = "Back",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(onClick = onBack),
                        style = MaterialTheme.typography.labelLarge
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.requests.size} requests found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isSortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { isSortMenuExpanded = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.sortOption == SortOption.NEWEST_FIRST) "Sort: Newest" else "Sort: Oldest",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NEWEST_FIRST)
                                    isSortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.OLDEST_FIRST)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { isFilterSheetOpen = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        val filterCount = (if (state.selectedType != null) 1 else 0) + (if (state.selectedStatus != null) 1 else 0)
                        Text(
                            text = if (filterCount > 0) "Filters ($filterCount)" else "Filters",
                            style = MaterialTheme.typography.labelMedium
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
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        text = state.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    state.requests.isEmpty() -> Text(
                        text = if (state.selectedType != null || state.selectedStatus != null) {
                            "No requests match your filters"
                        } else {
                            "You have no requests yet"
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
