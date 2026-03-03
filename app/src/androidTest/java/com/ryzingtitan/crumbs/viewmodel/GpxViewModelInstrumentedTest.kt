package com.ryzingtitan.crumbs.viewmodel

import android.Manifest
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ryzingtitan.crumbs.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class GpxViewModelInstrumentedTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var vm: GpxViewModel
    private lateinit var context: android.content.Context

    companion object {
        private val GPX_CONTENT = """
            <?xml version="1.0"?><gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="40.0" lon="-105.0"><ele>1500.0</ele></trkpt>
                <trkpt lat="40.01" lon="-105.01"><ele>1550.0</ele></trkpt>
                <trkpt lat="40.02" lon="-105.02"><ele>1520.0</ele></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()

        private val GPX_MISSING_ELE = """
            <?xml version="1.0"?><gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="40.0" lon="-105.0"><ele>1500.0</ele></trkpt>
                <trkpt lat="40.01" lon="-105.01"></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()

        private val GPX_EMPTY = """
            <?xml version="1.0"?><gpx version="1.1"/>
        """.trimIndent()
    }

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            vm = ViewModelProvider(activity)[GpxViewModel::class.java]
            context = activity
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun callParseGpxPoints(inputStream: InputStream): Pair<List<com.mapbox.geojson.Point>, List<Double?>> {
        val method = GpxViewModel::class.java.getDeclaredMethod(
            "parseGpxPoints",
            InputStream::class.java,
        )
        method.isAccessible = true
        return method.invoke(vm, inputStream) as Pair<List<com.mapbox.geojson.Point>, List<Double?>>
    }

    // ── parseGpxPoints tests (direct, synchronous) ────────────────────────

    @Test
    fun parseGpxPoints_withValidGpx_returnsCorrectPointCount() {
        val (points, _) = callParseGpxPoints(GPX_CONTENT.byteInputStream())

        assertEquals(3, points.size)
        assertEquals(40.0, points[0].latitude(), 0.001)
        assertEquals(-105.0, points[0].longitude(), 0.001)
    }

    @Test
    fun parseGpxPoints_withElevation_returnsElevationList() {
        val (_, elevations) = callParseGpxPoints(GPX_CONTENT.byteInputStream())

        assertEquals(3, elevations.size)
        assertEquals(1500.0, elevations[0]!!, 0.001)
        assertEquals(1550.0, elevations[1]!!, 0.001)
        assertEquals(1520.0, elevations[2]!!, 0.001)
    }

    @Test
    fun parseGpxPoints_withMissingElevation_returnsNullForMissingEle() {
        val (points, elevations) = callParseGpxPoints(GPX_MISSING_ELE.byteInputStream())

        assertEquals(2, points.size)
        assertEquals(1500.0, elevations[0]!!, 0.001)
        assertNull(elevations[1])
    }

    @Test
    fun parseGpxPoints_withEmptyGpx_returnsEmptyLists() {
        val (points, elevations) = callParseGpxPoints(GPX_EMPTY.byteInputStream())

        assertTrue(points.isEmpty())
        assertTrue(elevations.isEmpty())
    }

    // ── setGpxFile tests (async, uses StateFlow observation) ──────────────

    @Test
    fun setGpxFile_withValidGpx_populatesTrailPoints() = runBlocking {
        val file = File(context.cacheDir, "test.gpx").also { it.writeText(GPX_CONTENT) }
        val uri = Uri.fromFile(file)

        activityRule.scenario.onActivity { vm.setGpxFile(uri, "test.gpx") }

        withTimeout(10_000) {
            vm.trailPoints.first { it.size == 3 }
        }

        assertEquals(3, vm.trailPoints.value.size)
    }

    @Test
    fun setGpxFile_withValidGpx_populatesTrailInfo() = runBlocking {
        val file = File(context.cacheDir, "test.gpx").also { it.writeText(GPX_CONTENT) }
        val uri = Uri.fromFile(file)

        activityRule.scenario.onActivity { vm.setGpxFile(uri, "test.gpx") }

        withTimeout(10_000) {
            vm.trailInfo.first { it != null }
        }

        assertNotNull(vm.trailInfo.value)
        assertEquals("test", vm.trailInfo.value!!.name)
    }

    @Test
    fun setGpxFile_withInvalidUri_producesEmptyState() {
        val invalidUri = Uri.parse("content://invalid.provider/nonexistent")

        activityRule.scenario.onActivity { vm.setGpxFile(invalidUri, "bad.gpx") }

        // gpxFileName is set synchronously; wait for IO coroutine to fail
        assertEquals("bad.gpx", vm.gpxFileName.value)
        Thread.sleep(1000)

        assertTrue(vm.trailPoints.value.isEmpty())
        assertNull(vm.trailInfo.value)
    }

    @Test
    fun clearRoute_afterSetGpxFile_resetsState() = runBlocking {
        val file = File(context.cacheDir, "clear_test.gpx").also { it.writeText(GPX_CONTENT) }
        val uri = Uri.fromFile(file)

        activityRule.scenario.onActivity { vm.setGpxFile(uri, "clear_test.gpx") }

        withTimeout(10_000) {
            vm.trailPoints.first { it.isNotEmpty() }
        }

        activityRule.scenario.onActivity { vm.clearRoute() }

        assertTrue(vm.trailPoints.value.isEmpty())
        assertNull(vm.trailInfo.value)
        assertNull(vm.gpxUri.value)
        assertNull(vm.gpxFileName.value)
    }
}
