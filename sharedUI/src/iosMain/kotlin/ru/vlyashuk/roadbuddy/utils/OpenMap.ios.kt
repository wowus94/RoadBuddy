package ru.vlyashuk.roadbuddy.utils

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberOpenMapAction(): (lat: Double, lon: Double, label: String?) -> Unit {
    return { lat, lon, label ->
        val encodedLabel = label?.replace(" ", "%20")?.replace(",", "%2C") ?: ""
        val urlString = "http://maps.apple.com/?q=$encodedLabel&ll=$lat,$lon"
        NSURL(string = urlString).let { url ->
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
        }
    }
}