package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.offline_indicator
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(
                horizontal = RoadBuddyTheme.spacing.medium,
                vertical = RoadBuddyTheme.spacing.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.offline_indicator),
            style = RoadBuddyTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
