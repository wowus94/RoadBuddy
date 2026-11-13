package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
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
import kotlin.math.exp
import kotlin.math.ln
import androidx.compose.ui.graphics.Color as ComposeColor

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
    focusPoints: List<Pair<Double, Double>>,
    onZoomChanged: (zoom: Float) -> Unit,
    initialCamera: MapCamera?
) {
    val mapView = remember { MKMapView() }
    val annotations = remember { mutableMapOf<String, MKPointAnnotation>() }
    val annotationIds = remember { mutableMapOf<MKPointAnnotation, String>() }
    val markersById = remember { mutableMapOf<String, MapMarker>() }
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    val mapDelegate = remember(onLocationSelected) {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                mapView.region.useContents {
                    val lonDelta = this.span.longitudeDelta
                    if (lonDelta > 0.0) {
                        val zoom = ln(360.0 / lonDelta) / ln(2.0)
                        currentOnZoomChanged(zoom.toFloat())
                    }
                }
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
                val marker = annotationIds[viewForAnnotation]
                    ?.let { markersById[it] }
                if (marker != null && isClusterMarker(marker)) {
                    view.markerTintColor = UIColor(red = 0.12, green = 0.53, blue = 0.9, alpha = 1.0)
                    view.glyphText = marker.clusterCount.toString()
                } else {
                    view.markerTintColor = UIColor(red = 0.9, green = 0.22, blue = 0.21, alpha = 1.0)
                    view.glyphText = null
                }
                return view
            }

            override fun mapView(
                mapView: MKMapView,
                didSelectAnnotationView: MKAnnotationView
            ) {
                val ann = didSelectAnnotationView.annotation as? MKPointAnnotation
                ann ?: return
                val id = annotationIds[ann] ?: return
                val marker = markersById[id]
                val cam = marker?.takeIf { isClusterMarker(it) }
                    ?.let { cameraForPoints(it.clusterPoints) }
                if (cam != null) {
                    dispatch_async(dispatch_get_main_queue()) {
                        mapView.setRegion(
                            MKCoordinateRegionMake(
                                CLLocationCoordinate2DMake(cam.latitude, cam.longitude),
                                MKCoordinateSpanMake(cam.latitudeSpan, cam.longitudeSpan)
                            ),
                            animated = true
                        )
                    }
                } else {
                    currentOnMarkerClick(id)
                }
            }
        }
    }

    LaunchedEffect(mapView, mapDelegate) {
        mapView.delegate = mapDelegate
    }

    LaunchedEffect(markers) {
        markersById.clear()
        markers.forEach { markersById[it.id] = it }
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

    var cameraRestored by remember { mutableStateOf(false) }
    val currentInitialCamera by rememberUpdatedState(initialCamera)

    LaunchedEffect(focusPoints) {
        if (focusPoints.isEmpty()) return@LaunchedEffect
        if (!cameraRestored) {
            cameraRestored = true
            val cam = currentInitialCamera
            if (cam != null) {
                val span = 360.0 * exp(-cam.zoom.toDouble() * ln(2.0))
                dispatch_async(dispatch_get_main_queue()) {
                    mapView.setRegion(
                        MKCoordinateRegionMake(
                            CLLocationCoordinate2DMake(cam.latitude, cam.longitude),
                            MKCoordinateSpanMake(span, span)
                        ),
                        animated = false
                    )
                }
                return@LaunchedEffect
            }
        }
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