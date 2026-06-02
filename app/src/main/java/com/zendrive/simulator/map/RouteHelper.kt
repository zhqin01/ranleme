package com.zendrive.simulator.map

import com.zendrive.simulator.domain.DriveScene
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.VirtualOrder
import kotlin.random.Random

/**
 * 基于当前 GPS 坐标和场景偏好，生成虚拟接送点与目的地。
 */
object RouteHelper {

    private val relayCheckpoints = listOf(
        "江边观景点" to (760.0 to 340.0),
        "城市高架夜景位" to (1400.0 to 700.0),
        "山脚咖啡停车区" to (2100.0 to -950.0),
        "湖畔安静路段" to (2800.0 to -1300.0)
    )

    fun generateOrder(
        current: GeoPoint,
        scene: DriveScene,
        relayIndex: Int = 0,
        passengerNames: List<String>,
        orderTitles: List<String>
    ): VirtualOrder {
        val relayMode = scene == DriveScene.RouteRelay

        val pickup = current.shiftedBy(scene.pickupOffset.first, scene.pickupOffset.second)

        val destinationOffset = if (relayMode) {
            val (name, offset) = relayCheckpoints[relayIndex % relayCheckpoints.size]
            offset
        } else {
            randomize(scene.destinationOffset)
        }
        val destination = current.shiftedBy(destinationOffset.first, destinationOffset.second)

        val title = if (relayMode) {
            relayCheckpoints[relayIndex % relayCheckpoints.size].first
        } else {
            "${scene.title}散心单"
        }

        return VirtualOrder(
            pickup = pickup,
            destination = destination,
            title = title,
            passengerName = passengerNames.random(),
            checkpointIndex = if (relayMode) relayIndex else 0
        )
    }

    private fun randomize(offset: Pair<Double, Double>): Pair<Double, Double> {
        val factor = Random.nextDouble(0.85, 1.25)
        val drift = Random.nextDouble(-180.0, 180.0)
        return (offset.first * factor + drift) to (offset.second * factor - drift)
    }
}
