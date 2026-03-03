package com.ryzingtitan.crumbs.location

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
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

    @Test
    fun startUpdates_callsRequestLocationUpdates() {
        // mockkConstructor intercepts WorkSource created inside LocationRequest.Builder.build(),
        // preventing the "not mocked" stub error when MockK hashes the LocationRequest argument.
        mockkConstructor(android.os.WorkSource::class)
        val mockLooper = mockk<Looper>(relaxed = true)

        handler.startUpdates(mockLooper)

        verify { mockFusedClient.requestLocationUpdates(any(), handler.locationCallback, mockLooper) }
        unmockkConstructor(android.os.WorkSource::class)
    }

    @Test
    fun locationCallback_withNullLastLocation_doesNotCallListener() {
        val mockListener = mockk<LocationTrackingService.LocationUpdateListener>()
        val mockResult = mockk<LocationResult>()
        every { mockResult.lastLocation } returns null
        every { mockListener.onLocationUpdated(any()) } just Runs

        handler.setListener(mockListener)
        handler.locationCallback.onLocationResult(mockResult)

        verify(exactly = 0) { mockListener.onLocationUpdated(any()) }
    }

    @Test
    fun setListener_replacement_callsNewListener() {
        val listener1 = mockk<LocationTrackingService.LocationUpdateListener>()
        val listener2 = mockk<LocationTrackingService.LocationUpdateListener>()
        val mockLocation = mockk<Location>()
        val mockResult = mockk<LocationResult>()
        every { mockResult.lastLocation } returns mockLocation
        every { listener1.onLocationUpdated(any()) } just Runs
        every { listener2.onLocationUpdated(any()) } just Runs

        handler.setListener(listener1)
        handler.setListener(listener2)
        handler.locationCallback.onLocationResult(mockResult)

        verify(exactly = 1) { listener2.onLocationUpdated(mockLocation) }
        verify(exactly = 0) { listener1.onLocationUpdated(any()) }
    }
}
