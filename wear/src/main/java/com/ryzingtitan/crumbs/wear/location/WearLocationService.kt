package com.ryzingtitan.crumbs.wear.location

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

internal class WearLocationCallbackHandler(
    private val fusedClient: FusedLocationProviderClient,
) {
    private var listener: WearLocationService.LocationUpdateListener? = null

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { listener?.onLocationUpdated(it) }
        }
    }

    fun setListener(listener: WearLocationService.LocationUpdateListener?) {
        this.listener = listener
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(looper: Looper) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, looper)
    }

    fun stopUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }
}

class WearLocationService(
    private val fusedClientFactory: (Context) -> FusedLocationProviderClient =
        { ctx -> LocationServices.getFusedLocationProviderClient(ctx) },
) : Service() {

    interface LocationUpdateListener {
        fun onLocationUpdated(location: Location)
    }

    inner class LocalBinder : Binder() {
        fun getService(): WearLocationService = this@WearLocationService
    }

    private val binder = LocalBinder()
    private lateinit var callbackHandler: WearLocationCallbackHandler

    override fun onCreate() {
        super.onCreate()
        callbackHandler = WearLocationCallbackHandler(fusedClientFactory(this))
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
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crumbs Navigation")
            .setContentText("Tracking your location")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "wear_location_tracking"
    }
}
