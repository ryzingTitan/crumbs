package com.ryzingtitan.crumbs

import android.app.Application
import com.mapbox.common.MapboxOptions

class CrumbsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
