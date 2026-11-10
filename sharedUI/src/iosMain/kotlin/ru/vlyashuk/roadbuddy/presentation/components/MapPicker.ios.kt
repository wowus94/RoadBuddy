package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.compose.resources.painterResource
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.ic_add
import roadbuddy.sharedui.generated.resources.ic_remove

private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier,
    markers: List<MapMarker>,
    onMarkerClick: (id: String) -> Unit,
    focusPoints: List<Pair<Double, Double>>
) {
    val mapView = remember { MKMapView() }
    val annotations = remember { mutableMapOf<String, MKPointAnnotation>() }
    val annotationIds = remember { mutableMapOf<MKPointAnnotation, String>() }
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    val mapDelegate = remember(onLocationSelected) {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                mapView.centerCoordinate.useContents {
                    onLocationSelected(this.latitude, this.longitude)
                }
            }

            override fun mapView(
                mapView: MKMapView,
                viewForAnnotation: MKAnnotationProtocol
            ): MKAnnotationView? {
                if (viewForAnnotation !is MKPointAnnotation) return null
                val view = MKMarkerAnnotationView(viewForAnnotation, "requestPin")
                view.canShowCallout = true
                view.markerTintColor = UIColor(red = 0.9, green = 0.22, blue = 0.21, alpha = 1.0)
                return view
            }

            override fun mapView(
                mapView: MKMapView,
                didSelectAnnotationView: MKAnnotationView
            ) {
                val ann = didSelectAnnotationView.annotation as? MKPointAnnotation
                ann?.let { annotationIds[it]?.let { id -> currentOnMarkerClick(id) } }
            }
        }
    }

    LaunchedEffect(mapView, mapDelegate) {
        mapView.delegate = mapDelegate
    }

    LaunchedEffect(markers) {
        val wantedIds = markers.mapTo(HashSet()) { it.id }
        annotations.keys.filter { it !in wantedIds }.forEach { id ->
            annotations.remove(id)?.let { ann ->
                annotationIds.remove(ann)
                mapView.removeAnnotation(ann)
            }
        }
        markers.forEach { marker ->
            val annotation = annotations[marker.id] ?: run {
                val created = MKPointAnnotation()
                created.setCoordinate(
                    CLLocationCoordinate2DMake(marker.latitude, marker.longitude)
                )
                created.setTitle(marker.title)
                annotationIds[created] = marker.id
                annotations[marker.id] = created
                mapView.addAnnotation(created)
                created
            }
            annotation.setCoordinate(
                CLLocationCoordinate2DMake(marker.latitude, marker.longitude)
            )
            annotation.setTitle(marker.title)
        }
    }

    LaunchedEffect(focusPoints) {
        if (focusPoints.isEmpty()) return@LaunchedEffect
        if (focusPoints.size == 1) {
            val (lat, lon) = focusPoints.first()
            dispatch_async(dispatch_get_main_queue()) {
                val c = CLLocationCoordinate2DMake(lat, lon)
                val s = MKCoordinateSpanMake(0.01, 0.01)
                mapView.setRegion(MKCoordinateRegionMake(c, s), animated = false)
            }
            return@LaunchedEffect
        }
        val minLat = focusPoints.minOf { it.first }
        val maxLat = focusPoints.maxOf { it.first }
        val minLon = focusPoints.minOf { it.second }
        val maxLon = focusPoints.maxOf { it.second }
        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0
        val latDelta = ((maxLat - minLat).coerceAtLeast(0.01) * 1.4).coerceAtMost(120.0)
        val lonDelta = ((maxLon - minLon).coerceAtLeast(0.01) * 1.4).coerceAtMost(120.0)
        dispatch_async(dispatch_get_main_queue()) {
            val c = CLLocationCoordinate2DMake(centerLat, centerLon)
            val s = MKCoordinateSpanMake(latDelta, lonDelta)
            mapView.setRegion(MKCoordinateRegionMake(c, s), animated = false)
        }
    }

    LaunchedEffect(latitude, longitude, focusPoints.isEmpty()) {
        if (focusPoints.isNotEmpty()) return@LaunchedEffect
        val centerLat = latitude ?: DEFAULT_LAT
        val centerLon = longitude ?: DEFAULT_LON
        
        val currentCenter = mapView.centerCoordinate.useContents { this }
        val distance = kotlin.math.hypot(
            currentCenter.latitude - centerLat,
            currentCenter.longitude - centerLon
        )
        if (distance > 0.0001) {
            dispatch_async(dispatch_get_main_queue()) {
                val c = CLLocationCoordinate2DMake(centerLat, centerLon)
                val s = MKCoordinateSpanMake(0.01, 0.01)
                mapView.setRegion(MKCoordinateRegionMake(c, s), animated = false)
            }
        }
    }

    Box(modifier = modifier) {
        UIKitView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Red),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ComposeColor.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.size(6.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = CardDefaults.cardColors(containerColor = ComposeColor.White)
                        ) {}
                    }
                }
                Card(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Red)
                ) {}
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        val currentRegion = mapView.region
                        val center = mapView.centerCoordinate
                        currentRegion.useContents {
                            val newSpan = MKCoordinateSpanMake(
                                latitudeDelta = this.span.latitudeDelta / 2.0,
                                longitudeDelta = this.span.longitudeDelta / 2.0
                            )
                            mapView.setRegion(
                                MKCoordinateRegionMake(center, newSpan),
                                animated = true
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = "Zoom In",
                        tint = ComposeColor.Black
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 1.dp,
                    color = ComposeColor.LightGray.copy(alpha = 0.5f)
                )

                IconButton(
                    onClick = {
                        val currentRegion = mapView.region
                        val center = mapView.centerCoordinate
                        currentRegion.useContents {
                            val newSpan = MKCoordinateSpanMake(
                                latitudeDelta = (this.span.latitudeDelta * 2.0).coerceAtMost(120.0),
                                longitudeDelta = (this.span.longitudeDelta * 2.0).coerceAtMost(120.0)
                            )
                            mapView.setRegion(
                                MKCoordinateRegionMake(center, newSpan),
                                animated = true
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_remove),
                        contentDescription = "Zoom Out",
                        tint = ComposeColor.Black
                    )
                }
            }
        }
    }
}