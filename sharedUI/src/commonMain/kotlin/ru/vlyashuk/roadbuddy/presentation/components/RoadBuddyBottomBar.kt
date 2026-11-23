package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.active_help
import roadbuddy.sharedui.generated.resources.feed
import roadbuddy.sharedui.generated.resources.ic_groups
import roadbuddy.sharedui.generated.resources.ic_home
import roadbuddy.sharedui.generated.resources.ic_view_list
import roadbuddy.sharedui.generated.resources.my_requests
import ru.vlyashuk.roadbuddy.presentation.navigation.Route
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

enum class AppTab(
    val route: Route,
    val labelRes: StringResource,
    val iconRes: DrawableResource
) {
    FEED(Route.Home, Res.string.feed, Res.drawable.ic_home),
    MY_REQUESTS(Route.MyRequests, Res.string.my_requests, Res.drawable.ic_view_list),
    ACTIVE_HELP(Route.ActiveHelp, Res.string.active_help, Res.drawable.ic_groups)
}

@Composable
fun RoadBuddyBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = RoadBuddyTheme.colors.surface,
        contentColor = RoadBuddyTheme.colors.onSurface
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = RoadBuddyTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}
