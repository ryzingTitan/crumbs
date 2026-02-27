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
import androidx.wear.compose.material.Text
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel
import java.util.concurrent.TimeUnit

@Composable
fun WearSummaryScreen(
    viewModel: WearNavigationViewModel,
    onDone: () -> Unit,
) {
    val summary by viewModel.navigationSummary.collectAsStateWithLifecycle()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Summary") }
        item { Spacer(Modifier.height(4.dp)) }
        summary?.let { s ->
            val hours = TimeUnit.MILLISECONDS.toHours(s.durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(s.durationMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(s.durationMs) % 60

            item { Text("Distance: %.2f mi".format(s.distanceMiles)) }
            item { Text("Elev Change: %.0f ft".format(s.elevationChangeFeet)) }
            item { Text("Avg Speed: %.1f mph".format(s.averageSpeedMph)) }
            item { Text("Time: %02d:%02d:%02d".format(hours, minutes, seconds)) }
        } ?: run {
            item { Text("No data recorded") }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Chip(
                label = { Text("Done") },
                onClick = onDone,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
