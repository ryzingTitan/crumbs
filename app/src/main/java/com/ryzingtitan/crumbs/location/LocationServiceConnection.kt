package com.ryzingtitan.crumbs.location

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

class LocationServiceConnection(
    private val onConnected: (LocationTrackingService) -> Unit,
    private val onDisconnected: () -> Unit,
) : ServiceConnection {

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val service = (binder as LocationTrackingService.LocalBinder).getService()
        onConnected(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected()
    }
}
