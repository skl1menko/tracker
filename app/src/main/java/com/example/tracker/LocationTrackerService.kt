package com.example.tracker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: LocationDatabase

    override fun onCreate() {
        super.onCreate()
        database = LocationDatabase.getDatabase(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Action.START.name -> startTracking()
            Action.STOP.name -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        val locationManager = LocationManager(applicationContext)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, LOCATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location Tracker")
            .setStyle(NotificationCompat.BigTextStyle())

        startForeground(1, notification.build())

        scope.launch {
            locationManager.trackLocation().collect { location ->
                val latitude = location.latitude
                val longitude = location.longitude

                // Сохраняем координаты в Room
                database.locationDao().insertLocation(
                    LocationEntity(latitude = latitude, longitude = longitude, timestamp = System.currentTimeMillis())
                )

                notificationManager.notify(
                    1, notification.setContentText("Location: $latitude / $longitude").build()
                )
            }
        }
    }

    private fun stopTracking() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    enum class Action { START, STOP }

    companion object {
        const val LOCATION_CHANNEL = "location_channel"
    }
}
