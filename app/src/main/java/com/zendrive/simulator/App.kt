package com.zendrive.simulator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.zendrive.simulator.data.db.AppDatabase
import com.zendrive.simulator.data.prefs.AppPreferences
import com.zendrive.simulator.data.repository.GarageRepository
import com.zendrive.simulator.data.repository.TripRepository
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.zendrive.simulator.services.CrashLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class App : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var prefs: AppPreferences
        private set
    lateinit var tripRepo: TripRepository
        private set
    lateinit var garageRepo: GarageRepository
        private set

    override fun onCreate() {
        super.onCreate()

        CrashLogger.init()

        // ── 高德 SDK 隐私合规（10.x 必须调用，否则地图白屏 + 定位失败）──
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        database = AppDatabase.getInstance(this)
        prefs = AppPreferences(this)
        tripRepo = TripRepository(database.tripDao())
        garageRepo = GarageRepository(database.garageDao(), prefs)

        // 初始化车库默认道具（仅首次）
        GlobalScope.launch {
            garageRepo.initIfEmpty()
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_LOCATION,
            getString(R.string.location_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于在后台持续获取位置信息"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_LOCATION = "location_service"
    }
}
