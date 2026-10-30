package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.darwin.NSObject
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.ic_add
import roadbuddy.sharedui.generated.resources.ic_remove

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

    val mapDelegate = remember(onLocationSelected) {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                mapView.centerCoordinate.useContents {
                    onLocationSelected(this.latitude, this.longitude)
                }
            }
        }
    }

    LaunchedEffect(mapView, mapDelegate) {
        mapView.delegate = mapDelegate
    }

    LaunchedEffect(latitude, longitude) {
        val centerLat = latitude ?: DEFAULT_LAT
        val centerLon = longitude ?: DEFAULT_LON
        val center = CLLocationCoordinate2DMake(centerLat, centerLon)
        
        val currentCenter = mapView.centerCoordinate.useContents { this }
        val distance = kotlin.math.hypot(
            currentCenter.latitude - centerLat,
            currentCenter.longitude - centerLon
        )
        if (distance > 0.0001) {
            val span = MKCoordinateSpanMake(0.01, 0.01)
            mapView.setRegion(MKCoordinateRegionMake(center, span), animated = true)
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
                .offset(y = (-20).dp) // Сдвигаем, чтобы кончик иглы был ровно в центре экрана
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