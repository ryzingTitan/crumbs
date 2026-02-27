package com.ryzingtitan.crumbs.wear.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel

@Composable
fun WearInfoScreen(
    viewModel: WearNavigationViewModel,
    onBack: () -> Unit,
) {
    val routePayload by viewModel.routePayload.collectAsStateWithLifecycle()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        routePayload?.let { payload ->
            item { ListHeader { Text("Trail Info") } }
            item { InfoRow(label = "Name", value = payload.name) }
            item {
                InfoRow(
                    label = "Length",
                    value = "%.2f mi".format(payload.lengthMeters * 0.000621371),
                )
            }
            item {
                InfoRow(
                    label = "Traveled",
                    value = "%.2f mi".format(viewModel.distanceMiles),
                )
            }
            item {
                InfoRow(
                    label = "Remaining",
                    value = "%.2f mi".format(viewModel.distanceRemainingMiles),
                )
            }
            item {
                InfoRow(
                    label = "Gain",
                    value = "%.0f ft".format(payload.elevationGainMeters * 3.28084),
                )
            }
            item {
                InfoRow(
                    label = "Loss",
                    value = "%.0f ft".format(payload.elevationLossMeters * 3.28084),
                )
            }
            item {
                InfoRow(
                    label = "Est. Time",
                    value = formatEstimatedTime(payload.estimatedTimeMinutes),
                )
            }
            item { InfoRow(label = "Difficulty", value = payload.difficulty) }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Chip(
                label = { Text("Back") },
                onClick = onBack,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatEstimatedTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListHeader {
        Text(text = "$label: $value")
    }
}
