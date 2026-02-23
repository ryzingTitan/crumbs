package com.ryzingtitan.crumbs.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LocationCallbackHandler], the pure-Kotlin dispatch layer
 * extracted from [LocationTrackingService] for testability.
 *
 * Tests correspond to the service-level behaviours described in the plan:
 *  - listener receives location results from the callback
 *  - null listener does not crash
 *  - stopUpdates() calls removeLocationUpdates (equivalent to onDestroy)
 */
class LocationTrackingServiceTest {

    private val mockFusedClient = mockk<FusedLocationProviderClient>(relaxed = true)
    private lateinit var handler: LocationCallbackHandler

    @Before
    fun setup() {
        handler = LocationCallbackHandler(mockFusedClient)
    }

    @Test
    fun setLocationUpdateListener_forwardsLocationToListener() {
        val mockListener = mockk<LocationTrackingService.LocationUpdateListener>()
        val mockLocation = mockk<Location>()
        val mockResult = mockk<LocationResult>()
        every { mockResult.lastLocation } returns mockLocation
        every { mockListener.onLocationUpdated(any()) } just Runs

        handler.setListener(mockListener)
        handler.locationCallback.onLocationResult(mockResult)

        verify { mockListener.onLocationUpdated(mockLocation) }
    }

    @Test
    fun setLocationUpdateListener_null_doesNotCrash() {
        val mockResult = mockk<LocationResult>()
        val mockLocation = mockk<Location>()
        every { mockResult.lastLocation } returns mockLocation

        handler.setListener(null)
        handler.locationCallback.onLocationResult(mockResult) // must not throw
    }

    @Test
    fun onDestroy_removesLocationUpdates() {
        handler.stopUpdates()

        verify { mockFusedClient.removeLocationUpdates(handler.locationCallback) }
    }
}
