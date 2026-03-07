package com.ryzingtitan.crumbs.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.ryzingtitan.crumbs.wear.data.RoutePayload
import com.ryzingtitan.crumbs.wear.data.RouteRepository
import com.ryzingtitan.crumbs.wear.location.WearLocationService
import com.ryzingtitan.crumbs.wear.location.WearLocationServiceConnection
import com.ryzingtitan.crumbs.wear.ui.WearInfoScreen
import com.ryzingtitan.crumbs.wear.ui.WearMapScreen
import com.ryzingtitan.crumbs.wear.ui.WearRouteListScreen
import com.ryzingtitan.crumbs.wear.ui.WearSummaryScreen
import com.ryzingtitan.crumbs.wear.ui.theme.CrumbsWearTheme
import com.ryzingtitan.crumbs.wear.viewmodel.WearNavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

private const val ROUTE_LIST = "list"
private const val ROUTE_MAP = "map"
private const val ROUTE_INFO = "info"
private const val ROUTE_SUMMARY = "summary"

class WearMainActivity : ComponentActivity(), WearLocationService.LocationUpdateListener {

    private val viewModel: WearNavigationViewModel by viewModels()
    private var locationService: WearLocationService? = null
    private var isBound = false

    private val dataChangedListener = DataClient.OnDataChangedListener { dataEvents ->
        for (event in dataEvents) {
            if (event.dataItem.uri.path == "/crumbs/route" &&
                event.type == DataEvent.TYPE_CHANGED) {
                val asset = DataMapItem.fromDataItem(event.dataItem)
                    .dataMap.getAsset("route") ?: continue
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val response = Wearable.getDataClient(this@WearMainActivity)
                            .getFdForAsset(asset).await()
                        val json = response.inputStream.use { it.bufferedReader().readText() }
                        val payload = RoutePayload.fromJson(json)
                        val sanitized = sanitizeFilename(payload.name)
                        val file = File(File(filesDir, "routes"), "$sanitized.json")
                        if (!file.exists()) {
                            file.parentFile?.mkdirs()
                            file.writeText(json)
                        }
                        RouteRepository.addSavedRoute(sanitized)
                    }
                }
            }
        }
    }

    private val serviceConnection = WearLocationServiceConnection(
        onConnected = { service ->
            locationService = service
            service.setLocationUpdateListener(this)
        },
        onDisconnected = { locationService = null },
    )

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startLocationService()
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) requestNotificationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Late-join: query DataClient for an already-delivered route
        lifecycleScope.launch(Dispatchers.IO) {
            restoreRouteFromDataClient()
        }

        // Populate saved route list from disk
        val routesDir = File(filesDir, "routes")
        if (routesDir.exists()) {
            val names = routesDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
            RouteRepository.setSavedRoutes(names)
        }

        requestLocationPermissions()

        setContent {
            CrumbsWearTheme {
                val navController: NavHostController = rememberSwipeDismissableNavController()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = ROUTE_LIST,
                ) {
                    composable(ROUTE_LIST) {
                        WearRouteListScreen(
                            viewModel = viewModel,
                            onRouteSelected = { name ->
                                loadSavedRoute(name) {
                                    navController.navigate(ROUTE_MAP) {
                                        popUpTo(ROUTE_LIST) { inclusive = true }
                                    }
                                }
                            },
                            onRouteDeleted = { name -> deleteRoute(name) },
                            onRefresh = {
                                withContext(Dispatchers.IO) {
                                    val routesDir = File(filesDir, "routes")
                                    if (routesDir.exists()) {
                                        val names = routesDir.listFiles()
                                            ?.filter { it.extension == "json" }
                                            ?.map { it.nameWithoutExtension }
                                            ?: emptyList()
                                        RouteRepository.setSavedRoutes(names)
                                    }
                                }
                            },
                        )
                    }

                    composable(ROUTE_MAP) {
                        BackHandler(enabled = true) { /* prevent swipe-to-dismiss */ }
                        WearMapScreen(
                            viewModel = viewModel,
                            onEndNavigation = { viewModel.endNavigation() },
                            onInfoTap = { navController.navigate(ROUTE_INFO) },
                        )
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
                                navController.navigate(ROUTE_LIST) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Wearable.getDataClient(this).addListener(dataChangedListener)
    }

    override fun onStop() {
        Wearable.getDataClient(this).removeListener(dataChangedListener)
        super.onStop()
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
            requestNotificationPermission()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
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
        val intent = Intent(this, WearLocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isBound = bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun loadSavedRoute(name: String, onSuccess: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val file = File(File(filesDir, "routes"), "$name.json")
                val payload = RoutePayload.fromJson(file.readText())
                RouteRepository.setRoute(payload)
                RouteRepository.setNavigating(true)
            }.onSuccess {
                launch(Dispatchers.Main) { onSuccess() }
            }
        }
    }

    private fun deleteRoute(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            File(File(filesDir, "routes"), "$name.json").delete()
            RouteRepository.removeSavedRoute(name)
        }
    }

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(100)

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
                    val sanitized = sanitizeFilename(payload.name)
                    val file = File(File(filesDir, "routes"), "$sanitized.json")
                    if (!file.exists()) {
                        file.parentFile?.mkdirs()
                        file.writeText(json)
                        RouteRepository.addSavedRoute(sanitized)
                    }
                }
            }
            items.release()
        }
    }
}
