package com.ryzingtitan.crumbs.viewmodel

import android.location.Location
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
import org.junit.Assert.assertNull
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
}
