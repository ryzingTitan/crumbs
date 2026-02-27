package com.ryzingtitan.crumbs.wear

import com.ryzingtitan.crumbs.wear.data.RoutePayload
import com.ryzingtitan.crumbs.wear.data.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutePayloadTest {

    private val payload = RoutePayload(
        name = "Bear Creek Trail",
        lengthMeters = 12345.6,
        elevationGainMeters = 200.5,
        elevationLossMeters = 150.0,
        estimatedTimeMinutes = 180,
        difficulty = "Moderate",
        points = listOf(
            RoutePoint(lat = 37.1234, lon = -122.4567, ele = 0.0),
            RoutePoint(lat = 37.1300, lon = -122.4600, ele = 0.0),
        ),
    )

    @Test
    fun `toJson and fromJson round-trip preserves all fields`() {
        val json = payload.toJson()
        val restored = RoutePayload.fromJson(json)

        assertEquals(payload.name, restored.name)
        assertEquals(payload.lengthMeters, restored.lengthMeters, 0.001)
        assertEquals(payload.elevationGainMeters, restored.elevationGainMeters, 0.001)
        assertEquals(payload.elevationLossMeters, restored.elevationLossMeters, 0.001)
        assertEquals(payload.estimatedTimeMinutes, restored.estimatedTimeMinutes)
        assertEquals(payload.difficulty, restored.difficulty)
        assertEquals(payload.points.size, restored.points.size)
    }

    @Test
    fun `toJson and fromJson preserves point coordinates`() {
        val json = payload.toJson()
        val restored = RoutePayload.fromJson(json)

        assertEquals(payload.points[0].lat, restored.points[0].lat, 1e-9)
        assertEquals(payload.points[0].lon, restored.points[0].lon, 1e-9)
        assertEquals(payload.points[1].lat, restored.points[1].lat, 1e-9)
        assertEquals(payload.points[1].lon, restored.points[1].lon, 1e-9)
    }

    @Test
    fun `fromJson handles empty points list`() {
        val emptyPayload = payload.copy(points = emptyList())
        val json = emptyPayload.toJson()
        val restored = RoutePayload.fromJson(json)
        assertEquals(0, restored.points.size)
    }
}
