package com.ryzingtitan.crumbs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.hillshadeLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.ryzingtitan.crumbs.R
import com.ryzingtitan.crumbs.ui.theme.CrumbsTheme
import com.ryzingtitan.crumbs.viewmodel.GpxViewModel
import com.ryzingtitan.crumbs.viewmodel.LocationViewModel

@Composable
fun LocationScreen(
    viewModel: LocationViewModel = viewModel(),
    gpxViewModel: GpxViewModel = viewModel(),
    onStartNavigation: () -> Unit = {},
    onEndNavigation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val location by viewModel.locationState.collectAsState()
    val trailPoints by gpxViewModel.trailPoints.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val navigationSummary by viewModel.navigationSummary.collectAsState()
    val mapViewportState = rememberMapViewportState()
    var mapboxMapRef by remember { mutableStateOf<MapboxMap?>(null) }
    var hasInitialLocationBeenCentered by remember { mutableStateOf(false) }

    LaunchedEffect(location) {
        location?.let { loc ->
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                .zoom(15.0)
                .build()

            when {
                !hasInitialLocationBeenCentered -> {
                    mapViewportState.flyTo(cameraOptions)
                    hasInitialLocationBeenCentered = true
                }
                isNavigating -> {
                    mapViewportState.flyTo(cameraOptions)
                }
            }
        }
    }

    LaunchedEffect(trailPoints, mapboxMapRef) {
        if (trailPoints.size >= 2) {
            mapboxMapRef?.let { map ->
                val camera = map.cameraForCoordinates(
                    trailPoints,
                    EdgeInsets(100.0, 100.0, 200.0, 100.0),
                    null,
                    null,
                )
                mapViewportState.flyTo(camera)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = "mapbox://styles/mapbox/outdoors-v12") },
        ) {
            MapEffect(Unit) { mapView ->
                mapboxMapRef = mapView.mapboxMap
                mapView.mapboxMap.getStyle { style ->
                    style.addSource(rasterDemSource("mapbox-dem") {
                        url("mapbox://mapbox.mapbox-terrain-dem-v1")
                        tileSize(512L)
                    })
                    val hillshade = hillshadeLayer("hillshade-layer", "mapbox-dem") {}
                    val firstSymbolLayerId = style.styleLayers
                        .firstOrNull { it.type == "symbol" }?.id
                    if (firstSymbolLayerId != null) {
                        style.addLayerBelow(hillshade, firstSymbolLayerId)
                    } else {
                        style.addLayer(hillshade)
                    }
                }
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(true)
                }
            }

            MapEffect(trailPoints) { mapView ->
                if (trailPoints.size >= 2) {
                    mapView.mapboxMap.getStyle { style ->
                        if (style.styleLayerExists("gpx-trail-layer"))
                            style.removeStyleLayer("gpx-trail-layer")
                        if (style.styleSourceExists("gpx-trail-source"))
                            style.removeStyleSource("gpx-trail-source")

                        val feature = Feature.fromGeometry(LineString.fromLngLats(trailPoints))
                        style.addSource(geoJsonSource("gpx-trail-source") {
                            featureCollection(FeatureCollection.fromFeature(feature))
                        })
                        val layer = lineLayer("gpx-trail-layer", "gpx-trail-source") {
                            lineColor("#E8553E")
                            lineWidth(4.0)
                            lineCap(LineCap.ROUND)
                            lineJoin(LineJoin.ROUND)
                            lineOpacity(0.9)
                        }
                        val firstSymbolId = style.styleLayers.firstOrNull { it.type == "symbol" }?.id
                        if (firstSymbolId != null) style.addLayerBelow(layer, firstSymbolId)
                        else style.addLayer(layer)
                    }
                } else {
                    mapView.mapboxMap.getStyle { style ->
                        if (style.styleLayerExists("gpx-trail-layer"))
                            style.removeStyleLayer("gpx-trail-layer")
                        if (style.styleSourceExists("gpx-trail-source"))
                            style.removeStyleSource("gpx-trail-source")
                    }
                }
            }
        }

        if (location == null) {
            Text(
                text = stringResource(R.string.waiting_for_gps),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SmallFloatingActionButton(
                onClick = {
                    location?.let { loc ->
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                .zoom(15.0)
                                .build(),
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Re-center map",
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ExtendedFloatingActionButton(
                onClick = { if (isNavigating) onEndNavigation() else onStartNavigation() },
                icon = {
                    Icon(
                        imageVector = if (isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(if (isNavigating) "End Navigation" else "Start Navigation")
                },
            )
        }

        navigationSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                title = { Text("Navigation Summary") },
                text = {
                    Column {
                        Text("Duration: ${formatDuration(summary.durationMs)}")
                        Text("Distance: ${"%.2f".format(summary.distanceMeters / 1000.0)} km")
                        val sign = if (summary.elevationChangeMeters >= 0) "+" else ""
                        Text("Elevation: $sign${"%.0f".format(summary.elevationChangeMeters)} m")
                        Text("Avg Speed: ${"%.1f".format(summary.averageSpeedKmh)} km/h")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSummary() }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"
}

@Preview(showBackground = true)
@Composable
fun LocationScreenPreview() {
    CrumbsTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Waiting for GPS signal\u2026",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}
