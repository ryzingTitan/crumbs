package com.ryzingtitan.crumbs.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
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
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.ryzingtitan.crumbs.wear.R
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel
import kotlinx.coroutines.delay

private const val MAP_STYLE = "mapbox://styles/kstoltzfus/cmm41n5tx006101s2dwhj8ph3"
private const val TRAIL_SOURCE_ID = "wear-trail-source"
private const val TRAIL_LAYER_ID = "wear-trail-layer"

@Composable
fun WearMapScreen(
    viewModel: WearNavigationViewModel,
    onEndNavigation: () -> Unit,
    onInfoTap: () -> Unit,
) {
    val routePayload by viewModel.routePayload.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isNavigating by viewModel.isNavigating.collectAsStateWithLifecycle()

    val mapViewportState = rememberMapViewportState()

    val trailPoints: List<Point> = routePayload?.points?.map { pt ->
        Point.fromLngLat(pt.lon, pt.lat)
    } ?: emptyList()

    var elapsedMs by remember { mutableLongStateOf(viewModel.elapsedMs) }
    var showAttribution by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun sendUrlToPhone(url: String) {
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                nodes.firstOrNull()?.id?.let { nodeId ->
                    Wearable.getMessageClient(context)
                        .sendMessage(nodeId, "/crumbs/open-url", url.toByteArray(Charsets.UTF_8))
                        .await()
                }
            }
        }
    }

    LaunchedEffect(isNavigating) {
        while (isNavigating) {
            elapsedMs = viewModel.elapsedMs
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isNavigating) {
                if (isNavigating) {
                    detectHorizontalDragGestures { change, _ -> change.consume() }
                }
            },
    ) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = MAP_STYLE) },
            scaleBar = {},
            compass = {},
            logo = {},
            attribution = {},
        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(true)
                }
            }

            MapEffect(currentLocation) { _ ->
                currentLocation?.let { loc ->
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(loc.longitude, loc.latitude))
                            .zoom(15.0)
                            .build()
                    )
                }
            }

            MapEffect(trailPoints) { mapView ->
                if (trailPoints.size >= 2) {
                    mapView.mapboxMap.getStyle { style ->
                        if (style.styleLayerExists(TRAIL_LAYER_ID))
                            style.removeStyleLayer(TRAIL_LAYER_ID)
                        if (style.styleSourceExists(TRAIL_SOURCE_ID))
                            style.removeStyleSource(TRAIL_SOURCE_ID)

                        val feature = Feature.fromGeometry(LineString.fromLngLats(trailPoints))
                        style.addSource(geoJsonSource(TRAIL_SOURCE_ID) {
                            featureCollection(FeatureCollection.fromFeature(feature))
                        })
                        val layer = lineLayer(TRAIL_LAYER_ID, TRAIL_SOURCE_ID) {
                            lineColor("#E8553E")
                            lineWidth(4.0)
                            lineCap(LineCap.ROUND)
                            lineJoin(LineJoin.ROUND)
                            lineOpacity(0.9)
                        }
                        val firstSymbolId = style.styleLayers
                            .firstOrNull { it.type == "symbol" }?.id
                        if (firstSymbolId != null) style.addLayerBelow(layer, firstSymbolId)
                        else style.addLayer(layer)
                    }
                }
            }
        }

        TimeText(
            modifier = Modifier.fillMaxSize(),
            startCurvedContent = {
                basicCurvedText(
                    text = run {
                        val totalMiles = viewModel.distanceMiles + viewModel.distanceRemainingMiles
                        val pct = if (totalMiles > 0.0) viewModel.distanceMiles / totalMiles * 100.0 else 0.0
                        "%.0f%%".format(pct)
                    },
                    style = CurvedTextStyle(color = Color.White),
                )
            },
            endCurvedContent = {
                basicCurvedText(
                    text = "${formatElapsedTime(elapsedMs)}  %.0f ft".format(viewModel.currentElevationFeet),
                    style = CurvedTextStyle(color = Color.White),
                )
            },
        )

        // Left: Stop / End Navigation button
        Button(
            onClick = onEndNavigation,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(36.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "End Navigation",
                modifier = Modifier.size(20.dp),
            )
        }

        // Right: Recenter (above) + Info (below) stacked in a column
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Recenter — just above center
            Button(
                onClick = {
                    currentLocation?.let { loc ->
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                .zoom(15.0)
                                .build()
                        )
                    }
                },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    modifier = Modifier.size(20.dp),
                )
            }
            // Info — just below center
            Button(
                onClick = onInfoTap,
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Trail Info",
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.mapbox_logo_icon),
                contentDescription = "Mapbox",
                modifier = Modifier.size(40.dp),
            )
            Button(
                onClick = { showAttribution = true },
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Map attribution",
                    modifier = Modifier.size(16.dp),
                )
            }
//            Icon(
//                imageVector = Icons.Default.Info,
//                contentDescription = "Map attribution",
//                tint = Color.White.copy(alpha = 0.7f),
//                modifier = Modifier
//                    .size(14.dp)
//                    .clickable(
//                        indication = null,
//                        interactionSource = remember { MutableInteractionSource() },
//                    ) { showAttribution = true },
//            )
        }

        if (showAttribution) {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { ListHeader { Text("Map Attribution") } }
                item {
                    Chip(
                        label = { Text("© Mapbox") },
                        onClick = {
                            showAttribution = false
                            sendUrlToPhone("https://www.mapbox.com/about/maps/")
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Chip(
                        label = { Text("© OpenStreetMap") },
                        onClick = {
                            showAttribution = false
                            sendUrlToPhone("https://www.openstreetmap.org/copyright")
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Chip(
                        label = { Text("Improve This Map") },
                        onClick = {
                            showAttribution = false
                            sendUrlToPhone("https://apps.mapbox.com/feedback/")
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Chip(
                        label = { Text("Mapbox Telemetry Opt-out") },
                        onClick = {
                            showAttribution = false
                            sendUrlToPhone("https://www.mapbox.com/telemetry/")
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Chip(
                        label = { Text("Back") },
                        onClick = { showAttribution = false },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun formatElapsedTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
