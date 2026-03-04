package com.ryzingtitan.crumbs.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.TimeText
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgba
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.ryzingtitan.crumbs.wear.R
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel
import kotlinx.coroutines.delay

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
    val isFollowingUserState = remember { mutableStateOf(true) }
    var isFollowingUser by isFollowingUserState

    LaunchedEffect(isNavigating) {
        if (isNavigating) isFollowingUser = true
        while (isNavigating) {
            elapsedMs = viewModel.elapsedMs
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        MapboxMap(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = """{"version":8,"sources":{},"layers":[]}""") },
            scaleBar = {},
            compass = {},
            logo = {},
            attribution = {},
        ) {
            MapEffect(Unit) { mapView ->
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
                mapView.mapboxMap.getStyle { style ->
                    style.addSource(rasterSource("usgs-topo") {
                        tiles(listOf("https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}"))
                        tileSize(256)
                        maxzoom(16)
                        attribution("Map services and data available from U.S. Geological Survey, National Geospatial Program")
                    })
                    style.addLayer(rasterLayer("usgs-topo-layer", "usgs-topo") {
                        rasterColorMix(listOf(0.2126, 0.7152, 0.0722, 0.0))
                        rasterColor(
                            interpolate {
                                linear()
                                rasterValue()
                                stop(0.0) { rgba(255.0, 255.0, 255.0, 1.0) }
                                stop(1.0) { rgba(0.0, 0.0, 0.0, 1.0) }
                            }
                        )
                    })
                }
            }

            MapEffect(currentLocation) { _ ->
                if (isFollowingUser) {
                    currentLocation?.let { loc ->
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                .zoom(15.0)
                                .bearing(0.0)
                                .build()
                        )
                    }
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
                    isFollowingUser = true
                    currentLocation?.let { loc ->
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(loc.longitude, loc.latitude))
                                .zoom(15.0)
                                .bearing(0.0)
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
