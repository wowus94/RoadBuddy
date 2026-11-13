package ru.vlyashuk.roadbuddy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import ru.vlyashuk.roadbuddy.data.connectivity.ConnectivityObserver
import ru.vlyashuk.roadbuddy.data.sync.RequestsSyncer
import ru.vlyashuk.roadbuddy.di.appModule
import ru.vlyashuk.roadbuddy.presentation.components.OfflineBanner
import ru.vlyashuk.roadbuddy.presentation.create.CreateScreen
import ru.vlyashuk.roadbuddy.presentation.details.DetailsScreen
import ru.vlyashuk.roadbuddy.presentation.edit.EditScreen
import ru.vlyashuk.roadbuddy.presentation.home.HomeScreen
import ru.vlyashuk.roadbuddy.presentation.login.LoginScreen
import ru.vlyashuk.roadbuddy.presentation.map.RequestsMapScreen
import ru.vlyashuk.roadbuddy.presentation.myrequests.MyRequestsScreen
import ru.vlyashuk.roadbuddy.presentation.navigation.Route
import ru.vlyashuk.roadbuddy.presentation.register.RegisterScreen
import ru.vlyashuk.roadbuddy.presentation.session.SessionState
import ru.vlyashuk.roadbuddy.presentation.session.SessionViewModel
import ru.vlyashuk.roadbuddy.presentation.session.SplashScreen
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
            val backStack = rememberNavBackStack(navConfig, Route.Splash)

            val sessionViewModel: SessionViewModel = koinViewModel()
            val sessionState by sessionViewModel.state.collectAsState()

            val connectivityObserver = koinInject<ConnectivityObserver>()
            val isOnline by connectivityObserver.isOnline.collectAsState()
            val requestsSyncer = koinInject<RequestsSyncer>()

            LaunchedEffect(Unit) {
                requestsSyncer.start()
            }

            LaunchedEffect(sessionState) {
                when (sessionState) {
                    SessionState.Unauthenticated -> {
                        backStack.clear()
                        backStack.add(Route.Login)
                    }

                    is SessionState.Authenticated -> {
                        backStack.clear()
                        backStack.add(Route.Home)
                    }

                    SessionState.Loading -> Unit
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isOnline) {
                        OfflineBanner()
                    }
                NavDisplay(
                    modifier = Modifier.weight(1f),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Route.Splash> {
                            SplashScreen()
                        }
                        entry<Route.Login> {
                            LoginScreen(
                                onAuthSuccess = {
                                    with(backStack) {
                                        clear()
                                        add(Route.Home)
                                    }
                                },
                                onNavigateToRegister = {
                                    backStack.add(Route.Register)
                                }
                            )
                        }
                        entry<Route.Register> {
                            RegisterScreen(
                                onAuthSuccess = {
                                    with(backStack) {
                                        clear()
                                        add(Route.Home)
                                    }
                                },
                                onNavigateToLogin = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<Route.Home> {
                            HomeScreen(
                                onNavigateToDetails = { id -> backStack.add(Route.Details(id)) },
                                onNavigateToCreate = { backStack.add(Route.Create) },
                                onNavigateToMyRequests = { backStack.add(Route.MyRequests) },
                                onNavigateToMap = { lat, lon, radius ->
                                    backStack.add(Route.RequestsMap(lat, lon, radius))
                                }
                            )
                        }
                        entry<Route.MyRequests> {
                            MyRequestsScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
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
                        entry<Route.RequestsMap> { key ->
                            RequestsMapScreen(
                                centerLat = key.latitude,
                                centerLon = key.longitude,
                                radiusKm = key.radiusKm,
                                onBack = { backStack.removeLastOrNull() },
                                onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
                            )
                        }
                    }
                )
                }
            }
        }
    })
}
