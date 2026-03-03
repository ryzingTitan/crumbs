package com.ryzingtitan.crumbs.ui

import android.Manifest
import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ryzingtitan.crumbs.MainActivity
import com.ryzingtitan.crumbs.viewmodel.GpxViewModel
import com.ryzingtitan.crumbs.viewmodel.LocationViewModel
import com.ryzingtitan.crumbs.viewmodel.TrailInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationScreenTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun locationVm(): LocationViewModel =
        ViewModelProvider(composeTestRule.activity)[LocationViewModel::class.java]

    private fun gpxVm(): GpxViewModel =
        ViewModelProvider(composeTestRule.activity)[GpxViewModel::class.java]

    @Suppress("UNCHECKED_CAST")
    private fun setTrailInfoOnVm(vm: GpxViewModel, info: TrailInfo) {
        val field = GpxViewModel::class.java.getDeclaredField("_trailInfo")
        field.isAccessible = true
        (field.get(vm) as MutableStateFlow<TrailInfo?>).value = info
    }

    private fun makeTestTrailInfo() = TrailInfo(
        name = "Test Trail",
        lengthMeters = 5000.0,
        elevationGainMeters = 100.0,
        elevationLossMeters = 50.0,
        estimatedTimeMinutes = 90,
        difficulty = "Moderate",
    )

    // ── GPS waiting text ──────────────────────────────────────────────────

    @Test
    fun showsWaitingText_whenLocationIsNull() {
        composeTestRule.onNodeWithText("Waiting for GPS signal\u2026").assertIsDisplayed()
    }

    @Test
    fun hidesWaitingText_whenLocationIsUpdated() {
        val mockLocation = mockk<Location>(relaxed = true)

        composeTestRule.runOnUiThread { locationVm().updateLocation(mockLocation) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Waiting for GPS signal\u2026").assertDoesNotExist()
    }

    // ── Altitude display ──────────────────────────────────────────────────

    @Test
    fun showsAltitude_whenLocationHasAltitude() {
        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.hasAltitude() } returns true
        every { mockLocation.altitude } returns 304.8 // 304.8m * 3.28084 ≈ 1000 ft

        composeTestRule.runOnUiThread { locationVm().updateLocation(mockLocation) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("1000 ft").assertIsDisplayed()
    }

    @Test
    fun doesNotShowAltitude_whenLocationHasNoAltitude() {
        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.hasAltitude() } returns false

        composeTestRule.runOnUiThread { locationVm().updateLocation(mockLocation) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ft", substring = true).assertDoesNotExist()
    }

    // ── Navigation FAB ────────────────────────────────────────────────────

    @Test
    fun navigationFab_hasStartNavigationDescription_initially() {
        composeTestRule.onNodeWithContentDescription("Start Navigation").assertIsDisplayed()
    }

    @Test
    fun navigationFab_hasStopDescription_whenNavigating() {
        composeTestRule.runOnUiThread { locationVm().startNavigation() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("End Navigation").assertIsDisplayed()
    }

    // ── Info FAB ──────────────────────────────────────────────────────────

    @Test
    fun infoFab_notVisible_whenNotNavigating() {
        composeTestRule.onNodeWithContentDescription("Trail Info").assertDoesNotExist()
    }

    @Test
    fun infoFab_visible_whenNavigatingWithTrail() {
        composeTestRule.runOnUiThread {
            setTrailInfoOnVm(gpxVm(), makeTestTrailInfo())
            locationVm().startNavigation()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Trail Info").assertIsDisplayed()
    }

    // ── Trail info dialog ─────────────────────────────────────────────────

    @Test
    fun trailInfoDialog_showsOnInfoFabClick() {
        composeTestRule.runOnUiThread {
            setTrailInfoOnVm(gpxVm(), makeTestTrailInfo())
            locationVm().startNavigation()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Trail Info").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Length:", substring = true).assertIsDisplayed()
    }

    @Test
    fun trailInfoDialog_closesOnOkClick() {
        composeTestRule.runOnUiThread {
            setTrailInfoOnVm(gpxVm(), makeTestTrailInfo())
            locationVm().startNavigation()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Trail Info").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Length:", substring = true).assertDoesNotExist()
    }

    // ── Navigation summary dialog ─────────────────────────────────────────

    @Test
    fun navigationSummaryDialog_showsWhenSummaryPresent() {
        composeTestRule.runOnUiThread { locationVm().endNavigation() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Navigation Summary").assertIsDisplayed()
    }

    @Test
    fun navigationSummaryDialog_closesOnOkClick() {
        composeTestRule.runOnUiThread { locationVm().endNavigation() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()

        assertNull(locationVm().navigationSummary.value)
    }

    @Test
    fun navigationSummaryDialog_showsDuration() {
        composeTestRule.runOnUiThread { locationVm().endNavigation() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Duration:", substring = true).assertIsDisplayed()
    }

    @Test
    fun navigationSummaryDialog_showsDistance() {
        composeTestRule.runOnUiThread { locationVm().endNavigation() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(" mi", substring = true).assertIsDisplayed()
    }

    // ── Other FABs ────────────────────────────────────────────────────────

    @Test
    fun mapStyleFab_exists() {
        composeTestRule.onNodeWithContentDescription("Switch map style").assertIsDisplayed()
    }

    @Test
    fun recenterFab_exists() {
        composeTestRule.onNodeWithContentDescription("Re-center map").assertIsDisplayed()
    }
}
