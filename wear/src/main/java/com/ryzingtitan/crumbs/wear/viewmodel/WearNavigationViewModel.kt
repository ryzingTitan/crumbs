package com.ryzingtitan.crumbs.wear.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryzingtitan.crumbs.wear.data.RoutePayload
import com.ryzingtitan.crumbs.wear.data.RouteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WearNavigationSummary(
    val durationMs: Long,
    val distanceMiles: Double,
    val elevationChangeFeet: Double,
    val averageSpeedMph: Double,
)

class WearNavigationViewModel : ViewModel() {

    val routePayload: StateFlow<RoutePayload?> = RouteRepository.routePayload
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isNavigating: StateFlow<Boolean> = RouteRepository.isNavigating
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val savedRouteNames: StateFlow<List<String>> = RouteRepository.savedRouteNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _navigationSummary = MutableStateFlow<WearNavigationSummary?>(null)
    val navigationSummary: StateFlow<WearNavigationSummary?> = _navigationSummary.asStateFlow()

    private val navigationLocations = mutableListOf<Location>()
    private var navigationStartTime: Long? = null

    val distanceMiles: Double
        get() {
            var total = 0.0
            for (i in 0 until navigationLocations.size - 1) {
                total += navigationLocations[i].distanceTo(navigationLocations[i + 1])
            }
            return total * 0.000621371
        }

    val distanceRemainingMiles: Double
        get() {
            val totalMiles = (routePayload.value?.lengthMeters ?: 0.0) * 0.000621371
            return (totalMiles - distanceMiles).coerceAtLeast(0.0)
        }

    val currentElevationFeet: Double
        get() = (_currentLocation.value?.altitude ?: 0.0) * 3.28084

    val elevationGainFeet: Double
        get() {
            var gain = 0.0
            for (i in 0 until navigationLocations.size - 1) {
                val delta = navigationLocations[i + 1].altitude - navigationLocations[i].altitude
                if (delta > 0) gain += delta
            }
            return gain * 3.28084
        }

    val currentSpeedMph: Double
        get() {
            val loc = _currentLocation.value ?: return 0.0
            return if (loc.hasSpeed()) loc.speed * 2.23694 else 0.0
        }

    val elapsedMs: Long
        get() {
            val start = navigationStartTime ?: return 0L
            return System.currentTimeMillis() - start
        }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
        if (isNavigating.value) {
            if (navigationStartTime == null) navigationStartTime = System.currentTimeMillis()
            navigationLocations.add(location)
        }
    }

    fun endNavigation() {
        val startTime = navigationStartTime ?: System.currentTimeMillis()
        val durationMs = System.currentTimeMillis() - startTime

        var totalMeters = 0.0
        for (i in 0 until navigationLocations.size - 1) {
            totalMeters += navigationLocations[i].distanceTo(navigationLocations[i + 1])
        }

        val elevationChangeFeet = if (navigationLocations.size >= 2) {
            (navigationLocations.last().altitude - navigationLocations.first().altitude) * 3.28084
        } else 0.0

        val distanceMiles = totalMeters * 0.000621371
        val averageSpeedMph = if (durationMs > 0) {
            distanceMiles / (durationMs / 3_600_000.0)
        } else 0.0

        _navigationSummary.value = WearNavigationSummary(
            durationMs = durationMs,
            distanceMiles = distanceMiles,
            elevationChangeFeet = elevationChangeFeet,
            averageSpeedMph = averageSpeedMph,
        )
        RouteRepository.setNavigating(false)
        navigationLocations.clear()
        navigationStartTime = null
    }

    fun dismissSummary() {
        _navigationSummary.value = null
        RouteRepository.clearRoute()
    }
}
