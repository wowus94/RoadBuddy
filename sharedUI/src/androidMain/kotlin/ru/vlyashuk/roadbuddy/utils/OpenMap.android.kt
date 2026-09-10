package ru.vlyashuk.roadbuddy.utils

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun rememberOpenMapAction(): (lat: Double, lon: Double, label: String?) -> Unit {
    val context = LocalContext.current
    return { lat, lon, label ->
        val encodedLabel = Uri.encode(label ?: "")
        val uri = "geo:$lat,$lon?q=$lat,$lon($encodedLabel)".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(Intent.createChooser(intent, "Open in maps"))
    }
}