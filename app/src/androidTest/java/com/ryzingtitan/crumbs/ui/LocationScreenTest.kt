package com.ryzingtitan.crumbs.ui

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ryzingtitan.crumbs.viewmodel.LocationViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsWaitingText_whenLocationIsNull() {
        val viewModel = LocationViewModel()

        composeTestRule.setContent {
            LocationScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Waiting for GPS signal\u2026").assertIsDisplayed()
    }

    @Test
    fun hidesWaitingText_whenLocationIsUpdated() {
        val viewModel = LocationViewModel()
        val mockLocation = mockk<Location>(relaxed = true)
        viewModel.updateLocation(mockLocation)

        composeTestRule.setContent {
            LocationScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Waiting for GPS signal\u2026").assertDoesNotExist()
    }
}
