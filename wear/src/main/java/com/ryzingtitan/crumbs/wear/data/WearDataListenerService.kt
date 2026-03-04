package com.ryzingtitan.crumbs.wear.data

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.File

class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            when (event.dataItem.uri.path) {
                "/crumbs/route" -> {
                    if (event.type == DataEvent.TYPE_CHANGED) {
                        val asset = DataMapItem.fromDataItem(event.dataItem)
                            .dataMap.getAsset("route") ?: continue
                        runBlocking {
                            val json = readAsset(asset) ?: return@runBlocking
                            val payload = runCatching { RoutePayload.fromJson(json) }
                                .getOrNull() ?: return@runBlocking
                            runCatching { saveRouteJson(json, payload.name) }
                            RouteRepository.addSavedRoute(sanitizeFilename(payload.name))
                        }
                    } else if (event.type == DataEvent.TYPE_DELETED) {
                        RouteRepository.clearRoute()
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
}
