package com.ryzingtitan.crumbs.wearable

import com.mapbox.geojson.Point
import com.ryzingtitan.crumbs.viewmodel.TrailInfo
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WearRouteSenderTest {

    private fun buildRouteJson(info: TrailInfo, points: List<Point>): String {
        val method = WearRouteSender::class.java.getDeclaredMethod(
            "buildRouteJson",
            TrailInfo::class.java,
            List::class.java,
        )
        method.isAccessible = true
        return method.invoke(WearRouteSender, info, points) as String
    }

    private fun makeTrailInfo(name: String? = "Test Trail") = TrailInfo(
        name = name,
        lengthMeters = 5000.0,
        elevationGainMeters = 200.0,
        elevationLossMeters = 150.0,
        estimatedTimeMinutes = 75,
        difficulty = "Moderate",
    )

    @Test
    fun buildRouteJson_containsAllTrailInfoFields() {
        val info = makeTrailInfo()
        val points = listOf(Point.fromLngLat(-105.0, 40.0))

        val json = JSONObject(buildRouteJson(info, points))

        assertEquals("Test Trail", json.getString("name"))
        assertEquals(5000.0, json.getDouble("lengthMeters"), 0.001)
        assertEquals(200.0, json.getDouble("elevationGainMeters"), 0.001)
        assertEquals(150.0, json.getDouble("elevationLossMeters"), 0.001)
        assertEquals(75, json.getInt("estimatedTimeMinutes"))
        assertEquals("Moderate", json.getString("difficulty"))
    }

    @Test
    fun buildRouteJson_withNullName_usesEmptyString() {
        val info = makeTrailInfo(name = null)

        val json = JSONObject(buildRouteJson(info, emptyList()))

        assertEquals("", json.getString("name"))
    }

    @Test
    fun buildRouteJson_containsPointsArray() {
        val info = makeTrailInfo()
        val points = listOf(
            Point.fromLngLat(-105.0, 40.0),
            Point.fromLngLat(-105.01, 40.01),
            Point.fromLngLat(-105.02, 40.02),
        )

        val json = JSONObject(buildRouteJson(info, points))

        assertEquals(3, json.getJSONArray("points").length())
    }

    @Test
    fun buildRouteJson_pointsHaveLatLonEle() {
        val info = makeTrailInfo()
        val points = listOf(Point.fromLngLat(-105.5, 39.7))

        val json = JSONObject(buildRouteJson(info, points))
        val firstPoint = json.getJSONArray("points").getJSONObject(0)

        assertEquals(39.7, firstPoint.getDouble("lat"), 0.001)
        assertEquals(-105.5, firstPoint.getDouble("lon"), 0.001)
        assertEquals(0.0, firstPoint.getDouble("ele"), 0.001)
    }

    @Test
    fun buildRouteJson_withEmptyPoints_hasEmptyArray() {
        val info = makeTrailInfo()

        val json = JSONObject(buildRouteJson(info, emptyList()))

        assertEquals(0, json.getJSONArray("points").length())
    }
}
