package com.ryzingtitan.crumbs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.PuckBearing
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
    onSendToWatch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val location by viewModel.locationState.collectAsState()
    val trailPoints by gpxViewModel.trailPoints.collectAsState()
    val trailInfo by gpxViewModel.trailInfo.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val navigationSummary by viewModel.navigationSummary.collectAsState()
    val mapViewportState = rememberMapViewportState()
    var mapboxMapRef by remember { mutableStateOf<MapboxMap?>(null) }
    var hasInitialLocationBeenCentered by remember { mutableStateOf(false) }
    var showTrailInfo by remember { mutableStateOf(false) }
    val isFollowingUserState = remember { mutableStateOf(true) }
    var isFollowingUser by isFollowingUserState
    var selectedMapStyle by remember { mutableStateOf(MapStyleType.USGS_TOPO) }

    LaunchedEffect(isNavigating) {
        if (isNavigating) isFollowingUser = true
    }

    LaunchedEffect(location) {
        location?.let { loc ->
            when {
                !hasInitialLocationBeenCentered -> {
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(loc.longitude, loc.latitude))
                            .zoom(16.0)
                            .build()
                    )
                    hasInitialLocationBeenCentered = true
                }
                isNavigating && isFollowingUser -> {
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(loc.longitude, loc.latitude))
                            .build()
                    )
                }
            }
        }
    }

    LaunchedEffect(trailPoints, mapboxMapRef) {
        if (trailPoints.size >= 2) {
            mapboxMapRef?.let { map ->
                val camera = map.cameraForCoordinates(
                    trailPoints,
                    CameraOptions.Builder().build(),
                    EdgeInsets(100.0, 100.0, 200.0, 100.0),
                    null,
                    null,
                )
                mapViewportState.flyTo(camera)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            scaleBar = { ScaleBar(isMetricUnit = false) },
            style = {
                MapStyle(
                    style = if (selectedMapStyle == MapStyleType.USGS_TOPO)
                        USGS_IMAGERY_TOPO_STYLE
                    else
                        MAPBOX_CUSTOM_STYLE
                )
            },
        ) {
            MapEffect(Unit) { mapView ->
                mapboxMapRef = mapView.mapboxMap
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                }
                mapView.gestures.addOnMoveListener(object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        isFollowingUserState.value = false
                    }
                    override fun onMove(detector: MoveGestureDetector): Boolean = false
                    override fun onMoveEnd(detector: MoveGestureDetector) {}
                })
            }

            MapEffect(selectedMapStyle) { mapView ->
                val maxZoom = if (selectedMapStyle == MapStyleType.USGS_TOPO) 16.0 else 22.0
                mapView.mapboxMap.setBounds(
                    CameraBoundsOptions.Builder()
                        .maxZoom(maxZoom)
                        .build()
                )
            }

            MapEffect(trailPoints, selectedMapStyle) { mapView ->
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

        location?.takeIf { it.hasAltitude() }?.let { loc ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 4.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 4.dp,
            ) {
                Text(
                    text = "${"%.0f".format(loc.altitude * 3.28084)} ft",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SmallFloatingActionButton(
                onClick = {
                    selectedMapStyle = if (selectedMapStyle == MapStyleType.USGS_TOPO)
                        MapStyleType.MAPBOX_CUSTOM
                    else
                        MapStyleType.USGS_TOPO
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Switch map style",
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SmallFloatingActionButton(
                onClick = {
                    isFollowingUser = true
                    location?.let { loc ->
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                .zoom(16.0)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isNavigating && trailInfo != null) {
                    FloatingActionButton(onClick = { showTrailInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Trail Info",
                        )
                    }
                    FloatingActionButton(onClick = onSendToWatch) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Send to Watch",
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { if (isNavigating) onEndNavigation() else onStartNavigation() },
                ) {
                    Icon(
                        imageVector = if (isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                        contentDescription = if (isNavigating) "End Navigation" else "Start Navigation",
                    )
                }
            }
        }

        if (showTrailInfo) {
            trailInfo?.let { info ->
                AlertDialog(
                    onDismissRequest = { showTrailInfo = false },
                    title = { Text(info.name ?: "Trail Info") },
                    text = {
                        Column {
                            Text("Length: ${"%.2f".format(info.lengthMeters * 0.000621371)} mi")
                            Text("Elevation Gain: ${"%.0f".format(info.elevationGainMeters * 3.28084)} ft")
                            Text("Elevation Loss: ${"%.0f".format(info.elevationLossMeters * 3.28084)} ft")
                            Text("Est. Time: ${formatDuration((info.estimatedTimeMinutes * 60_000L))}")
                            Text("Difficulty: ${info.difficulty}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTrailInfo = false }) { Text("OK") }
                    },
                )
            }
        }

        navigationSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                title = { Text("Navigation Summary") },
                text = {
                    Column {
                        Text("Duration: ${formatDuration(summary.durationMs)}")
                        Text("Distance: ${"%.2f".format(summary.distanceMeters * 0.000621371)} mi")
                        val sign = if (summary.elevationChangeMeters >= 0) "+" else ""
                        Text("Elevation: $sign${"%.0f".format(summary.elevationChangeMeters * 3.28084)} ft")
                        Text("Avg Speed: ${"%.1f".format(summary.averageSpeedMph)} mph")
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

private enum class MapStyleType { USGS_TOPO, MAPBOX_CUSTOM }

private const val MAPBOX_CUSTOM_STYLE = "mapbox://styles/kstoltzfus/cmm41n5tx006101s2dwhj8ph3"

private val USGS_IMAGERY_TOPO_STYLE = """
{
  "version": 8,
  "sources": {
    "usgs-topo-source": {
      "type": "raster",
      "tiles": [
        "https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/{z}/{y}/{x}"
      ],
      "tileSize": 256,
      "maxzoom": 20,
      "attribution": "Map services and data available from U.S. Geological Survey, National Geospatial Program"
    }
  },
  "layers": [
    {
      "id": "usgs-topo-layer",
      "type": "raster",
      "source": "usgs-topo-source"
    }
  ]
}
""".trimIndent()

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
