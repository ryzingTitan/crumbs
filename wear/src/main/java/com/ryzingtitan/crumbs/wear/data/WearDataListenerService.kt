package com.ryzingtitan.crumbs.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class WearDataListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            when (event.dataItem.uri.path) {
                "/crumbs/route" -> {
                    if (event.type == DataEvent.TYPE_CHANGED) {
                        val asset = DataMapItem.fromDataItem(event.dataItem)
                            .dataMap.getAsset("route") ?: continue
                        scope.launch {
                            val json = readAsset(asset) ?: return@launch
                            val payload = runCatching { RoutePayload.fromJson(json) }
                                .getOrNull() ?: return@launch
                            saveRouteJson(json, payload.name)
                            showRouteReceivedNotification(payload.name)
                            RouteRepository.addSavedRoute(sanitizeFilename(payload.name))
                            RouteRepository.setRoute(payload)
                            RouteRepository.setNavigating(true)
                        }
                    } else if (event.type == DataEvent.TYPE_DELETED) {
                        RouteRepository.clearRoute()
                    }
                }
                "/crumbs/status" -> {
                    if (event.type == DataEvent.TYPE_CHANGED) {
                        val navigating = DataMapItem.fromDataItem(event.dataItem)
                            .dataMap.getBoolean("navigating", false)
                        RouteRepository.setNavigating(navigating)
                    }
                }
            }
        }
    }

    private suspend fun readAsset(asset: Asset): String? {
        return runCatching {
            val client: DataClient = Wearable.getDataClient(this)
            val response: DataClient.GetFdForAssetResponse = client.getFdForAsset(asset).await()
            response.inputStream.use { it.bufferedReader().readText() }
        }.getOrNull()
    }

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(100)

    private fun saveRouteJson(json: String, name: String) {
        val dir = File(filesDir, "routes").also { it.mkdirs() }
        File(dir, "${sanitizeFilename(name)}.json").writeText(json)
    }

    private fun showRouteReceivedNotification(name: String) {
        val channelId = "route_received"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Route Received", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Route Saved")
            .setContentText("$name is ready for navigation")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(ROUTE_SAVED_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ROUTE_SAVED_NOTIFICATION_ID = 3001
    }
}
