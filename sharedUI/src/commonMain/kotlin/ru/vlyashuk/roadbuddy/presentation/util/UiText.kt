package ru.vlyashuk.roadbuddy.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class ResourceString(val resource: StringResource, vararg val args: Any) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is ResourceString -> stringResource(resource, *args)
    }
}
