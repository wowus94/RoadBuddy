package ru.vlyashuk.roadbuddy.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.*
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType

@Composable
fun RequestType.toLocalizedName(): String = when (this) {
    RequestType.TOWING -> stringResource(Res.string.type_towing)
    RequestType.FUEL -> stringResource(Res.string.type_fuel)
    RequestType.BATTERY -> stringResource(Res.string.type_battery)
    RequestType.TIRE -> stringResource(Res.string.type_tire)
    RequestType.OTHER -> stringResource(Res.string.type_other)
}

@Composable
fun RequestStatus.toLocalizedName(): String = when (this) {
    RequestStatus.OPEN -> stringResource(Res.string.status_open)
    RequestStatus.IN_PROGRESS -> stringResource(Res.string.status_in_progress)
    RequestStatus.COMPLETED -> stringResource(Res.string.status_completed)
    RequestStatus.CANCELLED -> stringResource(Res.string.status_cancelled)
}
