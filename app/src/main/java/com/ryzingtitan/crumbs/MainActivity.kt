package com.ryzingtitan.crumbs

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ryzingtitan.crumbs.location.LocationServiceConnection
import com.ryzingtitan.crumbs.location.LocationTrackingService
import com.ryzingtitan.crumbs.ui.LocationScreen
import com.ryzingtitan.crumbs.ui.theme.CrumbsTheme
import com.ryzingtitan.crumbs.viewmodel.GpxViewModel
import com.ryzingtitan.crumbs.viewmodel.LocationViewModel
import com.ryzingtitan.crumbs.wearable.WearRouteSender

class MainActivity : ComponentActivity(), LocationTrackingService.LocationUpdateListener {

    private val viewModel: LocationViewModel by viewModels()
    private val gpxViewModel: GpxViewModel by viewModels()
    private var locationService: LocationTrackingService? = null
    private var isBound = false

    private val serviceConnection = LocationServiceConnection(
        onConnected = { service ->
            locationService = service
            service.setLocationUpdateListener(this)
        },
        onDisconnected = {
            locationService = null
        },
    )

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) requestNotificationPermission()
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startLocationService()
    }

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val gpxFilePicker: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                }
                ?: uri.lastPathSegment
                ?: uri.toString()
            gpxViewModel.setGpxFile(uri, fileName)
            viewModel.startNavigation()
        }
        // no else: user cancelled, do nothing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrumbsTheme {
                LocationScreen(
                    viewModel = viewModel,
                    gpxViewModel = gpxViewModel,
                    onStartNavigation = {
                        gpxFilePicker.launch(
                            arrayOf("application/gpx+xml", "application/octet-stream", "text/xml")
                        )
                    },
                    onEndNavigation = {
                        viewModel.endNavigation()
                        gpxViewModel.clearRoute()
                        WearRouteSender.clearRoute(applicationContext, lifecycleScope)
                    },
                    onSendToWatch = {
                        val info = gpxViewModel.trailInfo.value ?: return@LocationScreen
                        WearRouteSender.sendRoute(
                            applicationContext,
                            info,
                            gpxViewModel.trailPoints.value,
                            lifecycleScope,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        requestLocationPermissions()
    }

    override fun onDestroy() {
        locationService?.setLocationUpdateListener(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, LocationTrackingService::class.java))
        super.onDestroy()
    }

    override fun onLocationUpdated(location: Location) {
        viewModel.updateLocation(location)
    }

    private fun requestLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            requestNotificationPermission()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startLocationService()
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isBound = bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }
}
