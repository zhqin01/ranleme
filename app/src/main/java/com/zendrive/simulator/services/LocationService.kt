package com.zendrive.simulator.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.zendrive.simulator.App
import com.zendrive.simulator.MainActivity
import com.zendrive.simulator.R
import com.zendrive.simulator.domain.GeoPoint

class LocationService : Service(), AMapLocationListener {

    private var locationClient: AMapLocationClient? = null
    private var locationManager: LocationManager? = null
    private val nativeLocationListener = LocationListener { location ->
        publishNativeLocation(location)
    }

    override fun onCreate() {
        super.onCreate()
        LocationPublisher.publishStatus("定位服务启动中")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        runCatching {
            val client = AMapLocationClient(applicationContext)
            client.setLocationListener(this)

            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                interval = 2000
                isNeedAddress = false
                isOnceLocation = false
            }
            client.setLocationOption(option)
            locationClient = client
        }.onFailure { throwable ->
            Log.w(TAG, "AMap location init failed", throwable)
            LocationPublisher.publishStatus("高德定位初始化失败，启用系统定位")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        LocationPublisher.publishStatus("正在定位")
        locationClient?.startLocation()
        startNativeLocationFallback()
        return START_STICKY
    }

    override fun onDestroy() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        runCatching {
            locationManager?.removeUpdates(nativeLocationListener)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: AMapLocation?) {
        if (location == null) {
            LocationPublisher.publishStatus("高德定位无返回")
            return
        }
        if (location.errorCode != 0) {
            val status = "高德定位失败 ${location.errorCode}"
            Log.w(TAG, "$status: ${location.errorInfo}")
            LocationPublisher.publishStatus(status)
            startNativeLocationFallback()
            return
        }
        LocationPublisher.publish(
            GeoPoint(location.latitude, location.longitude)
        )
    }

    private fun startNativeLocationFallback() {
        if (!hasLocationPermission()) {
            LocationPublisher.publishStatus("缺少定位权限")
            return
        }

        val manager = locationManager ?: return
        runCatching {
            publishBestLastKnownLocation(manager)
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            providers
                .filter { manager.isProviderEnabled(it) }
                .forEach { provider ->
                    manager.requestLocationUpdates(
                        provider,
                        2_000L,
                        0f,
                        nativeLocationListener,
                        Looper.getMainLooper()
                    )
                }
        }.onFailure { throwable ->
            Log.w(TAG, "Native location fallback failed", throwable)
            LocationPublisher.publishStatus("系统定位启动失败")
        }
    }

    private fun publishBestLastKnownLocation(manager: LocationManager) {
        val lastKnown = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider ->
                runCatching {
                    if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
                }.getOrNull()
            }
            .maxByOrNull { it.time }
        if (lastKnown != null) publishNativeLocation(lastKnown)
    }

    private fun publishNativeLocation(location: Location) {
        LocationPublisher.publish(GeoPoint(location.latitude, location.longitude))
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
        private const val TAG = "ZenDriveLocation"
        private const val NOTIFICATION_ID = 1001
    }
}
