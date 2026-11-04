package ru.vlyashuk.roadbuddy.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.auth_create_account
import roadbuddy.sharedui.generated.resources.auth_email
import roadbuddy.sharedui.generated.resources.auth_forgot_password
import roadbuddy.sharedui.generated.resources.auth_password
import roadbuddy.sharedui.generated.resources.auth_sign_in
import roadbuddy.sharedui.generated.resources.ic_email
import roadbuddy.sharedui.generated.resources.ic_lock
import roadbuddy.sharedui.generated.resources.ic_logo
import roadbuddy.sharedui.generated.resources.ic_visibility
import roadbuddy.sharedui.generated.resources.ic_visibility_off
import roadbuddy.sharedui.generated.resources.login_welcome
import ru.vlyashuk.roadbuddy.presentation.components.RoadBuddyButton
import ru.vlyashuk.roadbuddy.presentation.components.RoadBuddyCard
import ru.vlyashuk.roadbuddy.presentation.components.RoadBuddySecondaryButton
import ru.vlyashuk.roadbuddy.presentation.components.RoadBuddyTextField
import ru.vlyashuk.roadbuddy.presentation.util.toLocalizedString
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) onAuthSuccess()
    }

    Scaffold(
        containerColor = RoadBuddyTheme.colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = RoadBuddyTheme.spacing.large)
                    .padding(vertical = RoadBuddyTheme.spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.medium)
            ) {
                Spacer(modifier = Modifier.height(RoadBuddyTheme.spacing.medium))

                // Logo and Welcome text
                Image(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = "Logo",
                    colorFilter = ColorFilter.tint(RoadBuddyTheme.colors.primary),
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = stringResource(Res.string.login_welcome),
                    style = RoadBuddyTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RoadBuddyTheme.colors.onBackground
                    )
                )

                Text(
                    text = stringResource(Res.string.auth_sign_in),
                    style = RoadBuddyTheme.typography.bodyMedium.copy(
                        color = RoadBuddyTheme.colors.onBackground.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(RoadBuddyTheme.spacing.small))

                // Form card container
                RoadBuddyCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RoadBuddyTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.medium)
                    ) {
                        // Email TextField
                        RoadBuddyTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = stringResource(Res.string.auth_email),
                            isError = state.emailError != null,
                            supportingText = state.emailError?.toLocalizedString(),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_email),
                                    contentDescription = null,
                                    tint = RoadBuddyTheme.colors.primary.copy(alpha = 0.6f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        // Password TextField with Toggle visibility
                        RoadBuddyTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = stringResource(Res.string.auth_password),
                            isError = state.passwordError != null,
                            supportingText = state.passwordError?.toLocalizedString(),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = RoadBuddyTheme.colors.primary.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        painter = if (isPasswordVisible) painterResource(Res.drawable.ic_visibility) else painterResource(
                                            Res.drawable.ic_visibility_off
                                        ),
                                        contentDescription = "Toggle password visibility",
                                        tint = RoadBuddyTheme.colors.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.signIn()
                                }
                            )
                        )

                        // Forgot Password Link
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = viewModel::resetPassword,
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = RoadBuddyTheme.colors.primary
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.auth_forgot_password),
                                    style = RoadBuddyTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // Error block
                        if (state.error != null) {
                            Text(
                                text = state.error.orEmpty(),
                                color = RoadBuddyTheme.colors.error,
                                style = RoadBuddyTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        // Action Button
                        RoadBuddyButton(
                            text = stringResource(Res.string.auth_sign_in),
                            onClick = viewModel::signIn,
                            isLoading = state.isLoading,
                            enabled = !state.isLoading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RoadBuddyTheme.spacing.small))

                // Bottom register call-to-action
                RoadBuddySecondaryButton(
                    text = stringResource(Res.string.auth_create_account),
                    onClick = onNavigateToRegister,
                    enabled = !state.isLoading
                )
            }
        }
    }
}