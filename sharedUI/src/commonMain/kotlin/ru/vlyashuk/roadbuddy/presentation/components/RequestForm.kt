package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.ic_close
import roadbuddy.sharedui.generated.resources.ic_location_on
import ru.vlyashuk.roadbuddy.domain.model.RequestType

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Request type", style = MaterialTheme.typography.labelLarge)
            RequestType.entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = entry == type,
                        onClick = { onTypeChange(entry) }
                    )
                    Text(entry.name)
                }
            }

            OutlinedTextField(
                value = authorName,
                onValueChange = onAuthorNameChange,
                label = { Text("Your name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contact,
                onValueChange = onContactChange,
                label = { Text("Contact *") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("Coordinates *", style = MaterialTheme.typography.labelLarge)

            val locationText = if (latitude != null && longitude != null) {
                "Latitude: ${latitude.toString().take(8)}, Longitude: ${
                    longitude.toString().take(8)
                }"
            } else {
                "Not selected — click to select"
            }

            OutlinedTextField(
                value = locationText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_location_on),
                        contentDescription = "Location"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
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

            Button(
                onClick = onSubmit,
                enabled = isValid && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text(buttonText)
                }
            }
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
                        contentDescription = "Close Map",
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
                    Text("Confirm location", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}