package ru.vlyashuk.roadbuddy.presentation.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.vlyashuk.roadbuddy.presentation.components.RequestForm

@Composable
fun EditScreen(
    requestId: String,
    onBack: () -> Unit = {},
    viewModel: EditViewModel = koinViewModel(parameters = { parametersOf(requestId) })
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isSaved) {
        LaunchedEffect(Unit) { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit request") },
                navigationIcon = {
                    Text(
                        "Back",
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(horizontal = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(padding))
            state.error != null -> Text(
                text = state.error ?: "Unknown error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding)
            )
            state.request == null -> Text(
                text = "Request not found",
                modifier = Modifier.padding(padding)
            )
            else -> RequestForm(
                title = state.title,
                description = state.description,
                type = state.type,
                authorName = state.authorName,
                contact = state.contact,
                isSaving = state.isSaving,
                isValid = state.isValid,
                error = state.error,
                buttonText = "Save",
                onTitleChange = viewModel::onTitleChanged,
                onDescriptionChange = viewModel::onDescriptionChanged,
                onTypeChange = viewModel::onTypeChanged,
                onAuthorNameChange = viewModel::onAuthorNameChanged,
                onContactChange = viewModel::onContactChanged,
                onSubmit = viewModel::updateRequest,
                modifier = Modifier.padding(padding)
            )
        }
    }
}