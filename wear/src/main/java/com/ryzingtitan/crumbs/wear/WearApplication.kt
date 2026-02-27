package com.ryzingtitan.crumbs.wear

import android.app.Application
import com.mapbox.common.MapboxOptions
import com.ryzingtitan.crumbs.wear.BuildConfig

class WearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
