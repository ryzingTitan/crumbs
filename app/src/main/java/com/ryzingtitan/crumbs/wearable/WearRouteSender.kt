package com.ryzingtitan.crumbs.wearable

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mapbox.geojson.Point
import com.ryzingtitan.crumbs.viewmodel.TrailInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

object WearRouteSender {

    fun sendRoute(
        context: Context,
        info: TrailInfo,
        points: List<Point>,
        scope: CoroutineScope,
    ) {
        scope.launch {
            runCatching {
                val json = buildRouteJson(info, points)
                val asset = Asset.createFromBytes(json.toByteArray(Charsets.UTF_8))

                val request = PutDataMapRequest.create("/crumbs/route").apply {
                    dataMap.putAsset("route", asset)
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request).await()

                val statusRequest = PutDataMapRequest.create("/crumbs/status").apply {
                    dataMap.putBoolean("navigating", true)
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(statusRequest).await()
            }
        }
    }

    fun clearRoute(context: Context, scope: CoroutineScope) {
        scope.launch {
            runCatching {
                val statusRequest = PutDataMapRequest.create("/crumbs/status").apply {
                    dataMap.putBoolean("navigating", false)
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(statusRequest).await()

                Wearable.getDataClient(context)
                    .deleteDataItems(Uri.parse("wear://*/crumbs/route"))
                    .await()
            }
        }
    }

    private fun buildRouteJson(info: TrailInfo, points: List<Point>): String {
        val obj = JSONObject()
        obj.put("name", info.name ?: "")
        obj.put("lengthMeters", info.lengthMeters)
        obj.put("elevationGainMeters", info.elevationGainMeters)
        obj.put("elevationLossMeters", info.elevationLossMeters)
        obj.put("estimatedTimeMinutes", info.estimatedTimeMinutes)
        obj.put("difficulty", info.difficulty)
        val arr = JSONArray()
        for (pt in points) {
            val ptObj = JSONObject()
            ptObj.put("lat", pt.latitude())
            ptObj.put("lon", pt.longitude())
            ptObj.put("ele", 0.0)
            arr.put(ptObj)
        }
        obj.put("points", arr)
        return obj.toString()
    }
}
