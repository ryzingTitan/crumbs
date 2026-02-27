package com.ryzingtitan.crumbs.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.ryzingtitan.crumbs.wear.data.RoutePayload
import com.ryzingtitan.crumbs.wear.data.RouteRepository
import com.ryzingtitan.crumbs.wear.location.WearLocationService
import com.ryzingtitan.crumbs.wear.location.WearLocationServiceConnection
import com.ryzingtitan.crumbs.wear.ui.WearInfoScreen
import com.ryzingtitan.crumbs.wear.ui.WearMapScreen
import com.ryzingtitan.crumbs.wear.ui.WearSummaryScreen
import com.ryzingtitan.crumbs.wear.ui.WearWaitingScreen
import com.ryzingtitan.crumbs.wear.ui.theme.CrumbsWearTheme
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ROUTE_WAITING = "waiting"
private const val ROUTE_MAP = "map"
private const val ROUTE_INFO = "info"
private const val ROUTE_SUMMARY = "summary"

class WearMainActivity : ComponentActivity(), WearLocationService.LocationUpdateListener {

    private val viewModel: WearNavigationViewModel by viewModels()
    private var locationService: WearLocationService? = null
    private var isBound = false

    private val serviceConnection = WearLocationServiceConnection(
        onConnected = { service ->
            locationService = service
            service.setLocationUpdateListener(this)
        },
        onDisconnected = { locationService = null },
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) startLocationService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Late-join: query DataClient for an already-delivered route
        lifecycleScope.launch(Dispatchers.IO) {
            restoreRouteFromDataClient()
        }

        requestLocationPermissions()

        setContent {
            CrumbsWearTheme {
                val navController: NavHostController = rememberSwipeDismissableNavController()
                var isOnMapScreen by remember { mutableStateOf(false) }
                DisposableEffect(navController) {
                    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                        isOnMapScreen = destination.route == ROUTE_MAP
                    }
                    navController.addOnDestinationChangedListener(listener)
                    onDispose { navController.removeOnDestinationChangedListener(listener) }
                }

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = ROUTE_WAITING,
                    userSwipeEnabled = !isOnMapScreen,
                ) {
                    composable(ROUTE_WAITING) {
                        WearWaitingScreen()

                        // Auto-navigate to map when route arrives
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            RouteRepository.routePayload.collect { payload ->
                                if (payload != null &&
                                    navController.currentDestination?.route == ROUTE_WAITING
                                ) {
                                    navController.navigate(ROUTE_MAP) {
                                        popUpTo(ROUTE_WAITING) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }

                    composable(ROUTE_MAP) {
                        WearMapScreen(
                            viewModel = viewModel,
                            onEndNavigation = { viewModel.endNavigation() },
                            onInfoTap = { navController.navigate(ROUTE_INFO) },
                        )

                        // Watch for summary to navigate to summary screen
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            viewModel.navigationSummary.collect { summary ->
                                if (summary != null) {
                                    navController.navigate(ROUTE_SUMMARY) {
                                        popUpTo(ROUTE_MAP) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }

                    composable(ROUTE_INFO) {
                        WearInfoScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(ROUTE_SUMMARY) {
                        WearSummaryScreen(
                            viewModel = viewModel,
                            onDone = {
                                viewModel.dismissSummary()
                                navController.navigate(ROUTE_WAITING) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        locationService?.setLocationUpdateListener(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, WearLocationService::class.java))
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
            startLocationService()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, WearLocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isBound = bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private suspend fun restoreRouteFromDataClient() {
        runCatching {
            val dataClient: DataClient = Wearable.getDataClient(this@WearMainActivity)
            val items = dataClient
                .getDataItems(Uri.parse("wear://*/crumbs/route"))
                .await()
            if (items.count > 0) {
                val item = items[0]
                val asset = DataMapItem.fromDataItem(item).dataMap.getAsset("route")
                if (asset != null) {
                    val response: DataClient.GetFdForAssetResponse =
                        dataClient.getFdForAsset(asset).await()
                    val json = response.inputStream.use { it.bufferedReader().readText() }
                    val payload = RoutePayload.fromJson(json)
                    RouteRepository.setRoute(payload)
                    RouteRepository.setNavigating(true)
                }
            }
            items.release()
        }
    }
}
