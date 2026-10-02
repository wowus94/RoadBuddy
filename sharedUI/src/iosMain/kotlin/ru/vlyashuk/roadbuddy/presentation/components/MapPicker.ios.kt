package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject

private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier
) {
    val mapView = remember { MKMapView() }

    val tapHandler = remember(mapView, onLocationSelected) {
        object : NSObject() {
            @ObjCAction
            fun handleTap(sender: UIGestureRecognizer) {
                val point = sender.locationInView(mapView)
                val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)
                coordinate.useContents {
                    onLocationSelected(this.latitude, this.longitude)
                }
            }
        }
    }

    LaunchedEffect(mapView, tapHandler) {
        val recognizer = UITapGestureRecognizer(
            target = tapHandler,
            action = NSSelectorFromString("handleTap:")
        )
        mapView.addGestureRecognizer(recognizer)
    }

    LaunchedEffect(latitude, longitude) {
        mapView.annotations.let { mapView.removeAnnotations(it) }

        val centerLat = latitude ?: DEFAULT_LAT
        val centerLon = longitude ?: DEFAULT_LON
        val center = CLLocationCoordinate2DMake(centerLat, centerLon)
        val span = MKCoordinateSpanMake(0.05, 0.05)
        mapView.setRegion(MKCoordinateRegionMake(center, span), animated = true)

        if (latitude != null && longitude != null) {
            val annotation = MKPointAnnotation().apply {
                setCoordinate(CLLocationCoordinate2DMake(latitude, longitude))
            }
            mapView.addAnnotation(annotation)
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier
    )
}