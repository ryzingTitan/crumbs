package com.ryzingtitan.crumbs.wear.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RouteRepository {
    private val _routePayload = MutableStateFlow<RoutePayload?>(null)
    val routePayload: StateFlow<RoutePayload?> = _routePayload.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    fun setRoute(payload: RoutePayload) {
        _routePayload.value = payload
    }

    fun setNavigating(navigating: Boolean) {
        _isNavigating.value = navigating
    }

    fun clearRoute() {
        _routePayload.value = null
        _isNavigating.value = false
    }
}
