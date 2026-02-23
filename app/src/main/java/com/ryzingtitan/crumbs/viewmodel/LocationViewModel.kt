package com.ryzingtitan.crumbs.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationViewModel : ViewModel() {
    private val _locationState = MutableStateFlow<Location?>(null)
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()

    fun updateLocation(location: Location) {
        _locationState.value = location
    }
}
