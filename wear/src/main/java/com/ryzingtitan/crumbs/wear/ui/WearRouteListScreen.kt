package com.ryzingtitan.crumbs.wear.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel

@Composable
fun WearRouteListScreen(
    viewModel: WearNavigationViewModel,
    onRouteSelected: (String) -> Unit,
    onRouteDeleted: (String) -> Unit,
    onRefresh: suspend () -> Unit,
) {
    val savedRouteNames by viewModel.savedRouteNames.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { 48.dp.toPx() }
    var pullOffsetPx by remember { mutableStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    pullOffsetPx = (pullOffsetPx + available.y).coerceAtMost(refreshThresholdPx * 1.5f)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullOffsetPx > 0f) {
                    val consume = (-available.y).coerceAtMost(pullOffsetPx)
                    pullOffsetPx -= consume
                    return Offset(0f, -consume)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffsetPx >= refreshThresholdPx) {
                    pullOffsetPx = 0f
                    isRefreshing = true
                    onRefresh()
                    isRefreshing = false
                } else {
                    pullOffsetPx = 0f
                }
                return Velocity.Zero
            }
        }
    }

    val offsetModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, pullOffsetPx.toInt())
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (savedRouteNames.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().then(offsetModifier),
            ) {
                Text(
                    text = "No saved routes.\nSend a route from\nthe phone app.",
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().then(offsetModifier),
            ) {
                item { ListHeader { Text("Saved Routes") } }
                for (name in savedRouteNames) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Chip(
                                label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { onRouteSelected(name) },
                                colors = ChipDefaults.primaryChipColors(),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CompactButton(
                                onClick = { onRouteDeleted(name) },
                                colors = ButtonDefaults.secondaryButtonColors(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete $name",
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Map services and data available from U.S. Geological Survey, National Geospatial Program",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
