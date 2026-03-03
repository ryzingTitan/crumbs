package com.ryzingtitan.crumbs.location

import android.content.ComponentName
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationServiceConnectionTest {

    @Test
    fun onServiceConnected_callsOnConnectedWithService() {
        val mockService = mockk<LocationTrackingService>(relaxed = true)
        val mockBinder = mockk<LocationTrackingService.LocalBinder>()
        every { mockBinder.getService() } returns mockService

        var connectedService: LocationTrackingService? = null
        val connection = LocationServiceConnection(
            onConnected = { connectedService = it },
            onDisconnected = {},
        )

        connection.onServiceConnected(mockk<ComponentName>(relaxed = true), mockBinder)

        assertEquals(mockService, connectedService)
    }

    @Test
    fun onServiceDisconnected_callsOnDisconnectedCallback() {
        var disconnected = false
        val connection = LocationServiceConnection(
            onConnected = {},
            onDisconnected = { disconnected = true },
        )

        connection.onServiceDisconnected(mockk<ComponentName>(relaxed = true))

        assertTrue(disconnected)
    }
}
