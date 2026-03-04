package com.ryzingtitan.crumbs.wear.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RouteRepository {
    private val _routePayload = MutableStateFlow<RoutePayload?>(null)
    val routePayload: StateFlow<RoutePayload?> = _routePayload.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _savedRouteNames = MutableStateFlow<List<String>>(emptyList())
    val savedRouteNames: StateFlow<List<String>> = _savedRouteNames.asStateFlow()

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

    fun addSavedRoute(name: String) {
        _savedRouteNames.update { (it + name).distinct() }
    }

    fun setSavedRoutes(names: List<String>) {
        _savedRouteNames.value = names
    }

    fun removeSavedRoute(name: String) {
        _savedRouteNames.value = _savedRouteNames.value.filter { it != name }
    }
}
