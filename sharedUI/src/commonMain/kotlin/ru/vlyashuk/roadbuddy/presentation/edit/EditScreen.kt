package ru.vlyashuk.roadbuddy.presentation.edit

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.back
import roadbuddy.sharedui.generated.resources.edit_request
import roadbuddy.sharedui.generated.resources.ic_arrow_back
import roadbuddy.sharedui.generated.resources.request_not_found
import roadbuddy.sharedui.generated.resources.save
import ru.vlyashuk.roadbuddy.presentation.components.RequestForm
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

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
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.edit_request),
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
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.padding(padding),
                color = RoadBuddyTheme.colors.primary
            )
            state.error != null -> Text(
                text = state.error ?: "Unknown error",
                color = RoadBuddyTheme.colors.error,
                style = RoadBuddyTheme.typography.bodyMedium,
                modifier = Modifier.padding(padding)
            )
            state.request == null -> Text(
                text = stringResource(Res.string.request_not_found),
                style = RoadBuddyTheme.typography.bodyMedium,
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
                buttonText = stringResource(Res.string.save),
                latitude = state.latitude,
                longitude = state.longitude,
                onLocationSelected = viewModel::onLocationSelected,
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