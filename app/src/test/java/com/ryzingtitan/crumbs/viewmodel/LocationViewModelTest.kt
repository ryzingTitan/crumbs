package com.ryzingtitan.crumbs.viewmodel

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LocationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LocationViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun locationState_initialValue_isNull() = runTest {
        assertNull(viewModel.locationState.first())
    }

    @Test
    fun updateLocation_emitsNewLocation() = runTest {
        val location = mockk<Location>()

        viewModel.updateLocation(location)

        assertEquals(location, viewModel.locationState.value)
    }

    @Test
    fun updateLocation_multipleUpdates_emitsLatestLocation() = runTest {
        val location1 = mockk<Location>()
        val location2 = mockk<Location>()

        viewModel.updateLocation(location1)
        viewModel.updateLocation(location2)

        assertEquals(location2, viewModel.locationState.value)
    }

    @Test
    fun isNavigating_initialValue_isFalse() = runTest {
        assertFalse(viewModel.isNavigating.first())
    }

    @Test
    fun navigationSummary_initialValue_isNull() = runTest {
        assertNull(viewModel.navigationSummary.first())
    }

    @Test
    fun startNavigation_setsIsNavigatingToTrue() = runTest {
        viewModel.startNavigation()

        assertTrue(viewModel.isNavigating.value)
    }

    @Test
    fun startNavigation_clearsPreviousNavigationLocations() = runTest {
        val loc = mockk<Location>(relaxed = true)
        every { loc.distanceTo(any()) } returns 1000f
        every { loc.altitude } returns 0.0

        // First navigation with a location
        viewModel.startNavigation()
        viewModel.updateLocation(loc)
        viewModel.endNavigation()

        // Second navigation with no new locations → distance should be 0
        viewModel.startNavigation()
        viewModel.endNavigation()

        assertEquals(0.0, viewModel.navigationSummary.value!!.distanceMeters, 0.001)
    }

    @Test
    fun updateLocation_whileNavigating_tracksLocationForMetrics() = runTest {
        val loc1 = mockk<Location>()
        val loc2 = mockk<Location>()
        every { loc1.distanceTo(loc2) } returns 500f
        every { loc1.altitude } returns 0.0
        every { loc2.altitude } returns 0.0

        viewModel.startNavigation()
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        viewModel.endNavigation()

        assertEquals(500.0, viewModel.navigationSummary.value!!.distanceMeters, 0.001)
    }

    @Test
    fun updateLocation_whenNotNavigating_doesNotTrackLocation() = runTest {
        val loc = mockk<Location>(relaxed = true)
        every { loc.distanceTo(any()) } returns 999f

        // Update before navigation starts → should not be tracked
        viewModel.updateLocation(loc)

        viewModel.startNavigation()
        viewModel.endNavigation()

        assertEquals(0.0, viewModel.navigationSummary.value!!.distanceMeters, 0.001)
    }

    @Test
    fun endNavigation_setsIsNavigatingToFalse() = runTest {
        viewModel.startNavigation()
        viewModel.endNavigation()

        assertFalse(viewModel.isNavigating.value)
    }

    @Test
    fun endNavigation_withNoLocations_producesZeroMetrics() = runTest {
        viewModel.startNavigation()
        viewModel.endNavigation()

        val summary = viewModel.navigationSummary.value!!
        assertEquals(0.0, summary.distanceMeters, 0.001)
        assertEquals(0.0, summary.elevationChangeMeters, 0.001)
    }

    @Test
    fun endNavigation_withTwoLocations_computesDistance() = runTest {
        val loc1 = mockk<Location>()
        val loc2 = mockk<Location>()
        every { loc1.distanceTo(loc2) } returns 1000f
        every { loc1.altitude } returns 0.0
        every { loc2.altitude } returns 0.0

        viewModel.startNavigation()
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        viewModel.endNavigation()

        assertEquals(1000.0, viewModel.navigationSummary.value!!.distanceMeters, 0.001)
    }

    @Test
    fun endNavigation_withTwoLocations_computesElevationChange() = runTest {
        val loc1 = mockk<Location>()
        val loc2 = mockk<Location>()
        every { loc1.distanceTo(loc2) } returns 100f
        every { loc1.altitude } returns 100.0
        every { loc2.altitude } returns 150.0

        viewModel.startNavigation()
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        viewModel.endNavigation()

        assertEquals(50.0, viewModel.navigationSummary.value!!.elevationChangeMeters, 0.001)
    }

    @Test
    fun endNavigation_withTwoLocations_computesAverageSpeedMph() = runTest {
        val loc1 = mockk<Location>()
        val loc2 = mockk<Location>()
        every { loc1.distanceTo(loc2) } returns 1609.344f  // exactly 1 mile
        every { loc1.altitude } returns 0.0
        every { loc2.altitude } returns 0.0

        viewModel.startNavigation()
        viewModel.updateLocation(loc1)
        viewModel.updateLocation(loc2)
        viewModel.endNavigation()

        val summary = viewModel.navigationSummary.value!!
        val expectedMph = if (summary.durationMs > 0) {
            (summary.distanceMeters / 1609.344) / (summary.durationMs / 3_600_000.0)
        } else 0.0
        assertEquals(expectedMph, summary.averageSpeedMph, 0.001)
    }

    @Test
    fun endNavigation_withSingleLocation_producesZeroDistanceAndElevation() = runTest {
        val loc = mockk<Location>()
        every { loc.altitude } returns 100.0

        viewModel.startNavigation()
        viewModel.updateLocation(loc)
        viewModel.endNavigation()

        val summary = viewModel.navigationSummary.value!!
        assertEquals(0.0, summary.distanceMeters, 0.001)
        assertEquals(0.0, summary.elevationChangeMeters, 0.001)
    }

    @Test
    fun endNavigation_setsNavigationSummary() = runTest {
        viewModel.startNavigation()
        viewModel.endNavigation()

        assertNotNull(viewModel.navigationSummary.value)
    }

    @Test
    fun dismissSummary_clearsNavigationSummary() = runTest {
        viewModel.startNavigation()
        viewModel.endNavigation()

        viewModel.dismissSummary()

        assertNull(viewModel.navigationSummary.value)
    }
}
