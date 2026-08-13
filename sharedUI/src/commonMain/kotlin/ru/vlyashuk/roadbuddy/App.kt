package ru.vlyashuk.roadbuddy

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import ru.vlyashuk.roadbuddy.di.appModule
import ru.vlyashuk.roadbuddy.presentation.create.CreateScreen
import ru.vlyashuk.roadbuddy.presentation.details.DetailsScreen
import ru.vlyashuk.roadbuddy.presentation.edit.EditScreen
import ru.vlyashuk.roadbuddy.presentation.home.HomeScreen
import ru.vlyashuk.roadbuddy.presentation.navigation.Route
import ru.vlyashuk.roadbuddy.theme.AppTheme

@OptIn(ExperimentalSerializationApi::class)
private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<Route>()
        }
    }
}

@Composable
fun App(
    koinAppDeclaration: KoinAppDeclaration? = null,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    KoinApplication(configuration = koinConfiguration(declaration = {
        koinAppDeclaration?.invoke(this)
        modules(appModule)
    }), content = {
        AppTheme(
            onThemeChanged
        ) {
            val backStack = rememberNavBackStack(navConfig, Route.Home)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                color = MaterialTheme.colorScheme.background
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Route.Home> {
                            HomeScreen(
                                onNavigateToDetails = { id -> backStack.add(Route.Details(id)) },
                                onNavigateToCreate = { backStack.add(Route.Create) }
                            )
                        }
                        entry<Route.Details> { key ->
                            DetailsScreen(
                                requestId = key.requestId,
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToEdit = { id -> backStack.add(Route.Edit(id)) }
                            )
                        }
                        entry<Route.Edit> { key ->
                            EditScreen(
                                requestId = key.requestId,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<Route.Create> {
                            CreateScreen(
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    })
}
