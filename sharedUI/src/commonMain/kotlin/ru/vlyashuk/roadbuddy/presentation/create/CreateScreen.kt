package ru.vlyashuk.roadbuddy.presentation.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import ru.vlyashuk.roadbuddy.presentation.components.RequestForm

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
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(horizontal = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        RequestForm(
            title = state.title,
            description = state.description,
            type = state.type,
            authorName = state.authorName,
            contact = state.contact,
            isSaving = state.isSaving,
            isValid = state.isValid,
            error = state.error,
            buttonText = "Publish",
            onTitleChange = viewModel::onTitleChanged,
            onDescriptionChange = viewModel::onDescriptionChanged,
            onTypeChange = viewModel::onTypeChanged,
            onAuthorNameChange = viewModel::onAuthorNameChanged,
            onContactChange = viewModel::onContactChanged,
            onSubmit = viewModel::createRequest,
            modifier = Modifier.padding(padding)
        )
    }
}