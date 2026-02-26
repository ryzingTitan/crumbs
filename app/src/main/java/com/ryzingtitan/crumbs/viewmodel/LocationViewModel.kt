package com.ryzingtitan.crumbs.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavigationSummary(
    val durationMs: Long,
    val distanceMeters: Double,
    val elevationChangeMeters: Double,
    val averageSpeedMph: Double,
)

class LocationViewModel : ViewModel() {
    private val _locationState = MutableStateFlow<Location?>(null)
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _navigationSummary = MutableStateFlow<NavigationSummary?>(null)
    val navigationSummary: StateFlow<NavigationSummary?> = _navigationSummary.asStateFlow()

    private var navigationStartTime: Long? = null
    private val navigationLocations = mutableListOf<Location>()

    fun updateLocation(location: Location) {
        _locationState.value = location
        if (_isNavigating.value) {
            navigationLocations.add(location)
        }
    }

    fun startNavigation() {
        navigationLocations.clear()
        navigationStartTime = System.currentTimeMillis()
        _isNavigating.value = true
    }

    fun endNavigation() {
        val startTime = navigationStartTime ?: System.currentTimeMillis()
        val durationMs = System.currentTimeMillis() - startTime

        var totalDistanceMeters = 0.0
        for (i in 0 until navigationLocations.size - 1) {
            totalDistanceMeters += navigationLocations[i].distanceTo(navigationLocations[i + 1])
        }

        val elevationChangeMeters = if (navigationLocations.size >= 2) {
            navigationLocations.last().altitude - navigationLocations.first().altitude
        } else {
            0.0
        }

        val averageSpeedMph = if (durationMs > 0) {
            (totalDistanceMeters / 1609.344) / (durationMs / 3_600_000.0)
        } else {
            0.0
        }

        _navigationSummary.value = NavigationSummary(
            durationMs = durationMs,
            distanceMeters = totalDistanceMeters,
            elevationChangeMeters = elevationChangeMeters,
            averageSpeedMph = averageSpeedMph,
        )
        _isNavigating.value = false
        navigationStartTime = null
        navigationLocations.clear()
    }

    fun dismissSummary() {
        _navigationSummary.value = null
    }
}
