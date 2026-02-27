package com.ryzingtitan.crumbs.wear.data

import org.json.JSONArray
import org.json.JSONObject

data class RoutePayload(
    val name: String,
    val lengthMeters: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
    val points: List<RoutePoint>,
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("lengthMeters", lengthMeters)
        obj.put("elevationGainMeters", elevationGainMeters)
        obj.put("elevationLossMeters", elevationLossMeters)
        obj.put("estimatedTimeMinutes", estimatedTimeMinutes)
        obj.put("difficulty", difficulty)
        val arr = JSONArray()
        for (pt in points) {
            val ptObj = JSONObject()
            ptObj.put("lat", pt.lat)
            ptObj.put("lon", pt.lon)
            ptObj.put("ele", pt.ele)
            arr.put(ptObj)
        }
        obj.put("points", arr)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): RoutePayload {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("points")
            val points = (0 until arr.length()).map { i ->
                val pt = arr.getJSONObject(i)
                RoutePoint(
                    lat = pt.getDouble("lat"),
                    lon = pt.getDouble("lon"),
                    ele = pt.getDouble("ele"),
                )
            }
            return RoutePayload(
                name = obj.optString("name", ""),
                lengthMeters = obj.getDouble("lengthMeters"),
                elevationGainMeters = obj.getDouble("elevationGainMeters"),
                elevationLossMeters = obj.getDouble("elevationLossMeters"),
                estimatedTimeMinutes = obj.getInt("estimatedTimeMinutes"),
                difficulty = obj.getString("difficulty"),
                points = points,
            )
        }
    }
}
