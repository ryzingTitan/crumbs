package com.ryzingtitan.crumbs.wear.location

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

class WearLocationServiceConnection(
    private val onConnected: (WearLocationService) -> Unit,
    private val onDisconnected: () -> Unit,
) : ServiceConnection {

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val service = (binder as WearLocationService.LocalBinder).getService()
        onConnected(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected()
    }
}
