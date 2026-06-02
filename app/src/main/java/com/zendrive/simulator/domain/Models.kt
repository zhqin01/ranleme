package com.zendrive.simulator.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    fun distanceMetersTo(other: GeoPoint): Double {
        val earthRadius = 6_371_000.0
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2.0)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun shiftedBy(metersNorth: Double, metersEast: Double): GeoPoint {
        val earthRadius = 6_371_000.0
        val dLat = metersNorth / earthRadius
        val dLng = metersEast / (earthRadius * cos(Math.toRadians(latitude)))
        return GeoPoint(
            latitude = latitude + Math.toDegrees(dLat),
            longitude = longitude + Math.toDegrees(dLng)
        )
    }
}

enum class DriveScene(
    val title: String,
    val shortText: String,
    val pickupOffset: Pair<Double, Double>,
    val destinationOffset: Pair<Double, Double>
) {
    CityNeon("城市霓虹", "繁华路段", 950.0 to 420.0, 2_800.0 to 1_200.0),
    MountainCurve("山路攻弯", "弯道落差", 1_100.0 to -600.0, 4_500.0 to -2_200.0),
    RelaxedRoad("松弛省道", "开阔少车", -900.0 to 650.0, -3_500.0 to 2_800.0),
    RouteRelay("网红路线接力", "连续打卡", 850.0 to 350.0, 1_800.0 to 1_100.0)
}

enum class DriveStage(val label: String) {
    Offline("待机"),
    Dispatching("派单中"),
    Pickup("前往接人"),
    WaitingPassenger("等待上车"),
    Trip("行程中"),
    Arriving("抵达结算"),
    Complete("已完成")
}

data class VirtualOrder(
    val pickup: GeoPoint,
    val destination: GeoPoint,
    val title: String,
    val passengerName: String,
    val checkpointIndex: Int = 0
)

data class GarageItem(
    val id: String,
    val name: String,
    val price: Int,
    val unlocked: Boolean
)

data class GarageState(
    val coins: Int = 0,
    val completedOrders: Int = 0,
    val selectedAvatarFrame: String = "",
    val selectedCarMarker: String = "default",
    val items: List<GarageItem> = listOf(
        GarageItem("frame_gold", "金色头像框", 200, false),
        GarageItem("frame_neon", "霓虹头像框", 250, false),
        GarageItem("frame_ocean", "海洋头像框", 300, false),
        GarageItem("car_porsche", "保时捷 911 车标", 500, false),
        GarageItem("car_ferrari", "法拉利车标", 600, false),
        GarageItem("car_lambo", "兰博基尼车标", 700, false),
        GarageItem("car_gtr", "GTR 车标", 550, false),
        GarageItem("badge_5orders", "5单成就徽章", 0, false),
        GarageItem("badge_10orders", "10单成就徽章", 0, false),
        GarageItem("badge_nightowl", "夜猫子徽章", 0, false)
    )
) {
    fun reward(): GarageState = copy(
        coins = coins + 60,
        completedOrders = completedOrders + 1
    )

    fun unlock(itemId: String): GarageState {
        val item = items.firstOrNull { it.id == itemId } ?: return this
        if (item.unlocked || coins < item.price) return this
        return copy(
            coins = coins - item.price,
            items = items.map {
                if (it.id == itemId) it.copy(unlocked = true) else it
            }
        )
    }
}

data class ZenDriveUiState(
    val selectedScene: DriveScene = DriveScene.CityNeon,
    val stage: DriveStage = DriveStage.Offline,
    val order: VirtualOrder? = null,
    val garage: GarageState = GarageState(),
    val lastMessage: String = "准备出车",
    val distanceToTargetMeters: Double? = null,
    val relayIndex: Int = 0,
    val currentLocation: GeoPoint? = null,
    val needsConfirm: Boolean = false // true = 到点了，等用户手动确认
)

/**
 * 历史行程记录（UI 层展示用）
 */
data class TripRecord(
    val id: Long = 0,
    val sceneTitle: String,
    val passengerName: String,
    val orderTitle: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val estimatedDistanceMeters: Double,
    val coinsEarned: Int,
    val completedAtMillis: Long
)

/**
 * 驾驶统计数据
 */
data class DriveStats(
    val totalOrders: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalCoins: Int = 0,
    val totalHours: Double = 0.0
)
