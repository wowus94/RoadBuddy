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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
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
import ru.vlyashuk.roadbuddy.presentation.activehelp.ActiveHelpScreen
import ru.vlyashuk.roadbuddy.presentation.components.AppTab
import ru.vlyashuk.roadbuddy.presentation.components.OfflineBanner
import ru.vlyashuk.roadbuddy.presentation.components.RoadBuddyBottomBar
import ru.vlyashuk.roadbuddy.presentation.create.CreateScreen
import ru.vlyashuk.roadbuddy.presentation.details.DetailsScreen
import ru.vlyashuk.roadbuddy.presentation.edit.EditScreen
import ru.vlyashuk.roadbuddy.presentation.history.HistoryScreen
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
            val sessionViewModel: SessionViewModel = koinViewModel()
            val sessionState by sessionViewModel.state.collectAsState()

            val connectivityObserver = koinInject<ConnectivityObserver>()
            val isOnline by connectivityObserver.isOnline.collectAsState()
            val requestsSyncer = koinInject<RequestsSyncer>()

            LaunchedEffect(Unit) {
                requestsSyncer.start()
            }

            val feedBackStack = rememberNavBackStack(navConfig, Route.Home)
            val myRequestsBackStack = rememberNavBackStack(navConfig, Route.MyRequests)
            val activeHelpBackStack = rememberNavBackStack(navConfig, Route.ActiveHelp)
            val historyBackStack = rememberNavBackStack(navConfig, Route.History)
            val authBackStack = rememberNavBackStack(navConfig, Route.Login)

            var selectedTabName by rememberSaveable { mutableStateOf(AppTab.FEED.name) }
            val selectedTab = AppTab.valueOf(selectedTabName)
            val activeBackStack = when (selectedTab) {
                AppTab.FEED -> feedBackStack
                AppTab.MY_REQUESTS -> myRequestsBackStack
                AppTab.ACTIVE_HELP -> activeHelpBackStack
                AppTab.HISTORY -> historyBackStack
            }

            val entryDecorators: List<NavEntryDecorator<NavKey>> = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>()
            )

            LaunchedEffect(sessionState) {
                if (sessionState == SessionState.Unauthenticated) {
                    authBackStack.clear()
                    authBackStack.add(Route.Login)
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
                    when (sessionState) {
                        SessionState.Loading -> SplashScreen()
                        SessionState.Unauthenticated -> NavDisplay(
                            backStack = authBackStack,
                            onBack = { authBackStack.removeLastOrNull() },
                            entryDecorators = entryDecorators,
                            entryProvider = entryProvider {
                                entry<Route.Login> {
                                    LoginScreen(
                                        onAuthSuccess = {},
                                        onNavigateToRegister = {
                                            authBackStack.add(Route.Register)
                                        }
                                    )
                                }
                                entry<Route.Register> {
                                    RegisterScreen(
                                        onAuthSuccess = {},
                                        onNavigateToLogin = { authBackStack.removeLastOrNull() }
                                    )
                                }
                            }
                        )
                        is SessionState.Authenticated -> Column(modifier = Modifier.fillMaxSize()) {
                            NavDisplay(
                                modifier = Modifier.weight(1f),
                                backStack = activeBackStack,
                                onBack = { activeBackStack.popToRoot() },
                                entryDecorators = entryDecorators,
                                entryProvider = appEntryProvider(activeBackStack)
                            )
                            RoadBuddyBottomBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTabName = it.name }
                            )
                        }
                    }
                }
            }
        }
    })
}

private fun appEntryProvider(backStack: NavBackStack<NavKey>) = entryProvider<NavKey> {
    entry<Route.Home> {
        HomeScreen(
            onNavigateToDetails = { id -> backStack.add(Route.Details(id)) },
            onNavigateToCreate = { backStack.add(Route.Create) },
            onNavigateToMap = { lat, lon, radius ->
                backStack.add(Route.RequestsMap(lat, lon, radius))
            }
        )
    }
    entry<Route.MyRequests> {
        MyRequestsScreen(
            onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
        )
    }
    entry<Route.ActiveHelp> {
        ActiveHelpScreen(
            onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
        )
    }
    entry<Route.History> {
        HistoryScreen(
            onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
        )
    }
    entry<Route.Details> { key ->
        DetailsScreen(
            requestId = key.requestId,
            onBack = { backStack.popToRoot() },
            onNavigateToEdit = { id -> backStack.add(Route.Edit(id)) }
        )
    }
    entry<Route.Edit> { key ->
        EditScreen(
            requestId = key.requestId,
            onBack = { backStack.popToRoot() }
        )
    }
    entry<Route.Create> {
        CreateScreen(
            onBack = { backStack.popToRoot() }
        )
    }
    entry<Route.RequestsMap> { key ->
        RequestsMapScreen(
            centerLat = key.latitude,
            centerLon = key.longitude,
            radiusKm = key.radiusKm,
            onBack = { backStack.popToRoot() },
            onNavigateToDetails = { id -> backStack.add(Route.Details(id)) }
        )
    }
}

private fun NavBackStack<NavKey>.popToRoot() {
    while (size > 1) {
        removeAt(lastIndex)
    }
}
