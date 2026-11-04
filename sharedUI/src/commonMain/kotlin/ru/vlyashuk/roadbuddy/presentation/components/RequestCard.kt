package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RoadRequest
import ru.vlyashuk.roadbuddy.presentation.util.toLocalizedName
import ru.vlyashuk.roadbuddy.theme.RoadBuddyTheme
import ru.vlyashuk.roadbuddy.utils.DateTimeConverter

@Composable
fun RequestCard(
    request: RoadRequest,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) RoadBuddyTheme.colors.primary else Color.LightGray.copy(alpha = 0.5f),
        label = "cardBorder"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 2.dp,
        label = "cardElevation"
    )

    RoadBuddyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = RoadBuddyTheme.spacing.medium,
                vertical = RoadBuddyTheme.spacing.extraSmall
            )
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.padding(RoadBuddyTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(RoadBuddyTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Text(
                    text = request.type.toLocalizedName(),
                    style = RoadBuddyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = RoadBuddyTheme.colors.primary,
                    modifier = Modifier
                        .background(
                            color = RoadBuddyTheme.colors.primary.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Creation Date
                Text(
                    text = DateTimeConverter.format(request.createdAt),
                    style = RoadBuddyTheme.typography.bodySmall,
                    color = RoadBuddyTheme.colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Title
            Text(
                text = request.title,
                style = RoadBuddyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RoadBuddyTheme.colors.onSurface
            )

            // Description Preview (limited to 2 lines with ellipsis)
            Text(
                text = request.description,
                style = RoadBuddyTheme.typography.bodyMedium,
                color = RoadBuddyTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RoadBuddyTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author info
                Text(
                    text = "👤  " + request.authorName,
                    style = RoadBuddyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = RoadBuddyTheme.colors.onSurfaceVariant
                )

                // Status Badge
                val statusColor = when (request.status) {
                    RequestStatus.OPEN -> RoadBuddyTheme.colors.primary
                    RequestStatus.IN_PROGRESS -> RoadBuddyTheme.colors.tertiary
                    RequestStatus.COMPLETED -> RoadBuddyTheme.colors.secondary
                    RequestStatus.CANCELLED -> RoadBuddyTheme.colors.error
                }

                Text(
                    text = request.status.toLocalizedName(),
                    style = RoadBuddyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
