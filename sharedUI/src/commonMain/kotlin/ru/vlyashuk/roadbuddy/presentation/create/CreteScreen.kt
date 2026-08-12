package ru.vlyashuk.roadbuddy.presentation.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.vlyashuk.roadbuddy.domain.model.RequestType

@Composable
fun CreateScreen(
    viewModel: CreateViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isSaved) {
        LaunchedEffect(Unit) { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New request") },
                navigationIcon = {
                    Text(
                        "Back",
                        modifier = Modifier.clickable(onClick = onBack)
                    )
                }
            )
        }
    ) { padding ->
        CreateForm(
            state = state,
            onTitleChange = viewModel::onTitleChanged,
            onDescriptionChange = viewModel::onDescriptionChanged,
            onTypeChange = viewModel::onTypeChanged,
            onAuthorNameChange = viewModel::onAuthorNameChanged,
            onContactChange = viewModel::onContactChanged,
            onSave = viewModel::createRequest,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun CreateForm(
    state: CreateUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (RequestType) -> Unit,
    onAuthorNameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text("Title *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Text("Request type", style = MaterialTheme.typography.labelLarge)
        RequestType.entries.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = type == state.type,
                    onClick = { onTypeChange(type) }
                )
                Text(type.name)
            }
        }

        OutlinedTextField(
            value = state.authorName,
            onValueChange = onAuthorNameChange,
            label = { Text("Your name *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.contact,
            onValueChange = onContactChange,
            label = { Text("Contact *") },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSave,
            enabled = state.isValid && !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator()
            } else {
                Text("Publish")
            }
        }
    }
}

