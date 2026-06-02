package com.zendrive.simulator.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.zendrive.simulator.App
import com.zendrive.simulator.MainActivity
import com.zendrive.simulator.R
import com.zendrive.simulator.domain.GeoPoint

class LocationService : Service(), AMapLocationListener {

    private lateinit var locationClient: AMapLocationClient

    override fun onCreate() {
        super.onCreate()
        locationClient = AMapLocationClient(applicationContext)
        locationClient.setLocationListener(this)

        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            interval = 2000
            isNeedAddress = false
            isOnceLocation = false
        }
        locationClient.setLocationOption(option)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        locationClient.startLocation()
        return START_STICKY
    }

    override fun onDestroy() {
        locationClient.stopLocation()
        locationClient.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: AMapLocation?) {
        if (location == null || location.errorCode != 0) return
        LocationPublisher.publish(
            GeoPoint(location.latitude, location.longitude)
        )
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_LOCATION)
            .setContentTitle(getString(R.string.location_notification_title))
            .setContentText(getString(R.string.location_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
