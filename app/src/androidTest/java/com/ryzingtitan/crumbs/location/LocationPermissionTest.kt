package com.ryzingtitan.crumbs.location

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ryzingtitan.crumbs.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationPermissionTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @Test
    fun withPermissionGranted_activityLaunchesWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Verify the activity reaches a resumed state without crashing
            scenario.onActivity { /* no-op: successful reach here means no crash */ }
        }
    }
}
