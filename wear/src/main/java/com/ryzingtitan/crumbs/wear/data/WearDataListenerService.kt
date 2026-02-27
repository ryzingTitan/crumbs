package com.ryzingtitan.crumbs.wear.data

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
}
