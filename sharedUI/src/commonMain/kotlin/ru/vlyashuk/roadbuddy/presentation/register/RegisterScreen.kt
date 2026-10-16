package ru.vlyashuk.roadbuddy.presentation.register

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(
    onAuthSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = koinViewModel()
) {


}
