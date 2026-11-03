package ru.vlyashuk.roadbuddy.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import ru.vlyashuk.roadbuddy.presentation.components.RequestCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToMyRequests: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RoadBuddy") },
                actions = {
                    Text(
                        text = "My requests",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(onClick = onNavigateToMyRequests),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Logout",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { viewModel.signOut() },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Text("+")
            }
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
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.requests.isEmpty() -> Text(
                    text = "No requests yet",
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
}