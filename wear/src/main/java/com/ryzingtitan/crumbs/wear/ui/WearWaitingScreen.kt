package com.ryzingtitan.crumbs.wear.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Text

@Composable
fun WearWaitingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "Waiting for route\nfrom phone",
            textAlign = TextAlign.Center,
        )
    }
}
