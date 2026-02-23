package com.ryzingtitan.crumbs.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ryzingtitan.crumbs.R

/**
 * Pure-Kotlin callback handler — no Android Service dependency, fully unit-testable.
 * Owns the LocationCallback and forwards results to the registered listener.
 */
internal class LocationCallbackHandler(
    private val fusedClient: FusedLocationProviderClient,
) {
    private var listener: LocationTrackingService.LocationUpdateListener? = null

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { listener?.onLocationUpdated(it) }
        }
    }

    fun setListener(listener: LocationTrackingService.LocationUpdateListener?) {
        this.listener = listener
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(looper: Looper) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, looper)
    }

    fun stopUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }
}

class LocationTrackingService(
    private val fusedClientFactory: (Context) -> FusedLocationProviderClient =
        { ctx -> LocationServices.getFusedLocationProviderClient(ctx) },
) : Service() {

    interface LocationUpdateListener {
        fun onLocationUpdated(location: Location)
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    private val binder = LocalBinder()
    private lateinit var callbackHandler: LocationCallbackHandler

    override fun onCreate() {
        super.onCreate()
        callbackHandler = LocationCallbackHandler(fusedClientFactory(this))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        callbackHandler.startUpdates(Looper.getMainLooper())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        callbackHandler.stopUpdates()
    }

    fun setLocationUpdateListener(listener: LocationUpdateListener?) {
        callbackHandler.setListener(listener)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking"
    }
}
