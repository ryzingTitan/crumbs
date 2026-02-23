package com.ryzingtitan.crumbs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.hillshadeLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.ryzingtitan.crumbs.R
import com.ryzingtitan.crumbs.ui.theme.CrumbsTheme
import com.ryzingtitan.crumbs.viewmodel.LocationViewModel

@Composable
fun LocationScreen(
    viewModel: LocationViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val location by viewModel.locationState.collectAsState()
    val mapViewportState = rememberMapViewportState()

    LaunchedEffect(location) {
        location?.let { loc ->
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.longitude, loc.latitude))
                    .zoom(15.0)
                    .build(),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = "mapbox://styles/mapbox/outdoors-v12") },
        ) {
            MapEffect(Unit) { mapView ->
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
        }

        if (location == null) {
            Text(
                text = stringResource(R.string.waiting_for_gps),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
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
