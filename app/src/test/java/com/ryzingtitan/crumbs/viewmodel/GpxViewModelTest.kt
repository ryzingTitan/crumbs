package com.ryzingtitan.crumbs.viewmodel

import android.app.Application
import com.mapbox.geojson.Point
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpxViewModelTest {

    private lateinit var viewModel: GpxViewModel

    @Before
    fun setup() {
        mockkStatic(android.location.Location::class)
        val app = mockk<Application>(relaxed = true)
        viewModel = GpxViewModel(app)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ── distanceBetween stub helpers ──────────────────────────────────────

    private fun stubDistanceBetween(metersPerSegment: Float) {
        every {
            android.location.Location.distanceBetween(any(), any(), any(), any(), any())
        } answers {
            val results = it.invocation.args[4] as FloatArray
            results[0] = metersPerSegment
        }
    }

    private fun callComputeTrailInfo(
        points: List<Point>,
        elevations: List<Double?>,
        name: String?,
    ): TrailInfo {
        val method = GpxViewModel::class.java.getDeclaredMethod(
            "computeTrailInfo",
            List::class.java,
            List::class.java,
            String::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(viewModel, points, elevations, name) as TrailInfo
    }

    private fun makeTestTrailInfo() = TrailInfo(
        name = "Test Trail",
        lengthMeters = 1000.0,
        elevationGainMeters = 50.0,
        elevationLossMeters = 30.0,
        estimatedTimeMinutes = 15,
        difficulty = "Easy",
    )

    // ── computeTrailInfo tests ────────────────────────────────────────────

    @Test
    fun computeTrailInfo_withElevationData_computesGainAndLoss() {
        stubDistanceBetween(1000f) // 1000m per segment, 2 segments = 2000m total
        val points = listOf(
            Point.fromLngLat(-105.0, 40.0),
            Point.fromLngLat(-105.01, 40.01),
            Point.fromLngLat(-105.02, 40.02),
        )
        val elevations = listOf(1500.0, 1550.0, 1520.0)

        val result = callComputeTrailInfo(points, elevations, "test")

        // gain: 1550-1500=50, loss: 1550-1520=30
        assertEquals(50.0, result.elevationGainMeters, 0.001)
        assertEquals(30.0, result.elevationLossMeters, 0.001)
    }

    @Test
    fun computeTrailInfo_easyDifficultyWithElevation() {
        // 100m distance, 10m gain → adjustedScore ≈ 2.2 < 50
        stubDistanceBetween(100f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.01, 0.01))
        val elevations = listOf(0.0, 10.0)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Easy", result.difficulty)
    }

    @Test
    fun computeTrailInfo_moderateDifficultyWithElevation() {
        // 1000m distance, 500m gain → adjustedScore ≈ 67.7, in [50, 100]
        stubDistanceBetween(1000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.1, 0.1))
        val elevations = listOf(0.0, 500.0)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Moderate", result.difficulty)
    }

    @Test
    fun computeTrailInfo_hardDifficultyWithElevation() {
        // 1000m distance, 1000m gain → adjustedScore ≈ 127.7 > 100
        stubDistanceBetween(1000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.1, 0.1))
        val elevations = listOf(0.0, 1000.0)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Hard", result.difficulty)
    }

    @Test
    fun computeTrailInfo_noElevation_shortTrail_isEasy() {
        // 4000m = 4km ≤ 8km → "Easy"
        stubDistanceBetween(4000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.1, 0.1))
        val elevations = listOf<Double?>(null, null) // no valid elevations

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Easy", result.difficulty)
    }

    @Test
    fun computeTrailInfo_noElevation_mediumTrail_isModerate() {
        // 10000m = 10km, 8 < 10 ≤ 16 → "Moderate"
        stubDistanceBetween(10000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.5, 0.5))
        val elevations = listOf<Double?>(null, null)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Moderate", result.difficulty)
    }

    @Test
    fun computeTrailInfo_noElevation_longTrail_isHard() {
        // 20000m = 20km > 16km → "Hard"
        stubDistanceBetween(20000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(1.0, 1.0))
        val elevations = listOf<Double?>(null, null)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals("Hard", result.difficulty)
    }

    @Test
    fun computeTrailInfo_computesEstimatedTime() {
        // lengthMeters=8000 → km=8, elevGain=600m
        // estimatedMinutes = ((8/4.0) + (600/600.0)) * 60 = 3 * 60 = 180
        stubDistanceBetween(8000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.5, 0.5))
        val elevations = listOf(0.0, 600.0)

        val result = callComputeTrailInfo(points, elevations, null)

        assertEquals(180, result.estimatedTimeMinutes)
    }

    @Test
    fun computeTrailInfo_namePropagated() {
        stubDistanceBetween(1000f)
        val points = listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(0.1, 0.1))

        val result = callComputeTrailInfo(points, emptyList(), "My Trail")

        assertEquals("My Trail", result.name)
    }

    // ── clearRoute tests ──────────────────────────────────────────────────

    @Test
    fun clearRoute_resetsAllStateFlows() {
        // Pre-set _trailInfo and _gpxFileName via reflection
        val trailInfoField = GpxViewModel::class.java.getDeclaredField("_trailInfo")
        trailInfoField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (trailInfoField.get(viewModel) as MutableStateFlow<TrailInfo?>).value = makeTestTrailInfo()

        val fileNameField = GpxViewModel::class.java.getDeclaredField("_gpxFileName")
        fileNameField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (fileNameField.get(viewModel) as MutableStateFlow<String?>).value = "test.gpx"

        val trailPointsField = GpxViewModel::class.java.getDeclaredField("_trailPoints")
        trailPointsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (trailPointsField.get(viewModel) as MutableStateFlow<List<Point>>).value =
            listOf(Point.fromLngLat(0.0, 0.0))

        viewModel.clearRoute()

        assertNull(viewModel.gpxUri.value)
        assertNull(viewModel.gpxFileName.value)
        assertTrue(viewModel.trailPoints.value.isEmpty())
        assertNull(viewModel.trailInfo.value)
    }
}
