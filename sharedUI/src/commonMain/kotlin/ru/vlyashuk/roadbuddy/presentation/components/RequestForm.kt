package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roadbuddy.sharedui.generated.resources.*
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import ru.vlyashuk.roadbuddy.presentation.util.toLocalizedName
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

@Composable
fun RequestForm(
    title: String,
    description: String,
    type: RequestType,
    authorName: String,
    contact: String,
    isSaving: Boolean,
    isValid: Boolean,
    error: String?,
    buttonText: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (RequestType) -> Unit,
    onAuthorNameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onSubmit: () -> Unit,
    latitude: Double? = null,
    longitude: Double? = null,
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isMapOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(RoadBuddyTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.medium)
        ) {
            RoadBuddyTextField(
                value = title,
                onValueChange = onTitleChange,
                label = stringResource(Res.string.form_title),
                singleLine = true
            )

            RoadBuddyTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = stringResource(Res.string.form_description),
                singleLine = false,
                minLines = 3
            )

            Text(
                text = stringResource(Res.string.form_type),
                style = RoadBuddyTheme.typography.titleMedium,
                modifier = Modifier.padding(top = RoadBuddyTheme.spacing.extraSmall)
            )
            
            RequestType.entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = entry == type,
                        onClick = { onTypeChange(entry) }
                    )
                    Text(
                        text = entry.toLocalizedName(),
                        style = RoadBuddyTheme.typography.bodyLarge
                    )
                }
            }

            RoadBuddyTextField(
                value = authorName,
                onValueChange = onAuthorNameChange,
                label = stringResource(Res.string.form_author),
                singleLine = true
            )

            RoadBuddyTextField(
                value = contact,
                onValueChange = onContactChange,
                label = stringResource(Res.string.form_contact),
                singleLine = true
            )

            if (error != null) {
                Text(
                    text = error,
                    color = RoadBuddyTheme.colors.error,
                    style = RoadBuddyTheme.typography.bodyMedium
                )
            }

            Text(
                text = stringResource(Res.string.form_coordinates),
                style = RoadBuddyTheme.typography.titleMedium,
                modifier = Modifier.padding(top = RoadBuddyTheme.spacing.extraSmall)
            )

            val locationText = if (latitude != null && longitude != null) {
                stringResource(
                    Res.string.form_coordinates_format,
                    latitude.toString().take(8),
                    longitude.toString().take(8)
                )
            } else {
                stringResource(Res.string.form_coordinates_placeholder)
            }

            RoadBuddyTextField(
                value = locationText,
                onValueChange = {},
                singleLine = true,
                readOnly = true,
                label = stringResource(Res.string.form_location),
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_location_on),
                        contentDescription = "Location",
                        tint = RoadBuddyTheme.colors.primary.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    isMapOpen = true
                                }
                            }
                        }
                    }
            )

            Spacer(modifier = Modifier.height(RoadBuddyTheme.spacing.extraSmall))

            RoadBuddyButton(
                text = buttonText,
                onClick = onSubmit,
                enabled = isValid && !isSaving,
                isLoading = isSaving
            )
        }

        if (isMapOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                MapPicker(
                    latitude = latitude,
                    longitude = longitude,
                    onLocationSelected = onLocationSelected,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { isMapOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.form_location_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { isMapOpen = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(stringResource(Res.string.form_location_confirm), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}