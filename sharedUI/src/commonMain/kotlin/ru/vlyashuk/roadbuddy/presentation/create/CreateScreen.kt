package ru.vlyashuk.roadbuddy.presentation.create

import androidx.compose.foundation.layout.padding
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
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.back
import roadbuddy.sharedui.generated.resources.ic_arrow_back
import roadbuddy.sharedui.generated.resources.new_request
import roadbuddy.sharedui.generated.resources.publish
import ru.vlyashuk.roadbuddy.presentation.components.RequestForm
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

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
        containerColor = RoadBuddyTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.new_request),
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
        RequestForm(
            title = state.title,
            description = state.description,
            type = state.type,
            authorName = state.authorName,
            contact = state.contact,
            isSaving = state.isSaving,
            isValid = state.isValid,
            error = state.error,
            buttonText = stringResource(Res.string.publish),
            onTitleChange = viewModel::onTitleChanged,
            onDescriptionChange = viewModel::onDescriptionChanged,
            onTypeChange = viewModel::onTypeChanged,
            onAuthorNameChange = viewModel::onAuthorNameChanged,
            onContactChange = viewModel::onContactChanged,
            onSubmit = viewModel::createRequest,
            latitude = state.latitude,
            longitude = state.longitude,
            onLocationSelected = viewModel::onLocationSelected,
            modifier = Modifier.padding(padding)
        )
    }
}