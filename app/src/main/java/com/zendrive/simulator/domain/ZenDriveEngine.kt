package com.zendrive.simulator.domain

import kotlin.random.Random

class ZenDriveEngine {
    private val passengers = listOf("林女士", "周先生", "阿岚", "老陈", "小许")
    private val chats = listOf(
        "师傅，今天路上不赶，慢慢开就行。",
        "这段夜景挺好看的，窗外像在放一部安静的电影。",
        "要是前面方便，可以找个地方停一下，喝口水。",
        "辛苦了，别急，安全比什么都重要。"
    )
    private val relayCheckpoints = listOf(
        "江边观景点",
        "城市高架夜景位",
        "山脚咖啡停车区",
        "湖畔安静路段"
    )

    var state = ZenDriveUiState()
        private set

    fun selectScene(scene: DriveScene): ZenDriveUiState {
        state = state.copy(selectedScene = scene)
        return state
    }

    fun beginDispatch(current: GeoPoint?): ZenDriveUiState {
        state = state.copy(
            stage = DriveStage.Dispatching,
            order = null,
            distanceToTargetMeters = null,
            lastMessage = if (current == null) "等待 GPS 定位" else "正在寻找合适路线"
        )
        return state
    }

    fun assignOrder(current: GeoPoint): ZenDriveUiState {
        val scene = state.selectedScene
        val relayMode = scene == DriveScene.RouteRelay
        val pickup = current.shiftedBy(scene.pickupOffset.first, scene.pickupOffset.second)
        val destinationOffset = if (relayMode) {
            val step = state.relayIndex + 1
            (scene.destinationOffset.first * step) to (scene.destinationOffset.second * step)
        } else {
            randomize(scene.destinationOffset)
        }
        val destination = current.shiftedBy(destinationOffset.first, destinationOffset.second)
        val title = if (relayMode) {
            relayCheckpoints[state.relayIndex % relayCheckpoints.size]
        } else {
            "${scene.title}散心单"
        }
        val order = VirtualOrder(
            pickup = pickup,
            destination = destination,
            title = title,
            passengerName = passengers.random(),
            checkpointIndex = state.relayIndex
        )
        state = state.copy(
            stage = DriveStage.Pickup,
            order = order,
            lastMessage = "新订单：$title",
            distanceToTargetMeters = current.distanceMetersTo(pickup)
        )
        return state
    }

    fun updateLocation(current: GeoPoint): ZenDriveUiState {
        val order = state.order ?: return state
        val target = when (state.stage) {
            DriveStage.Pickup, DriveStage.WaitingPassenger -> order.pickup
            DriveStage.Trip, DriveStage.Arriving -> order.destination
            else -> null
        }
        val distance = target?.let { current.distanceMetersTo(it) }
        state = state.copy(distanceToTargetMeters = distance)
        // 到点 → 暂停等用户手动确认
        if (distance != null && distance <= ARRIVAL_RADIUS_METERS && !state.needsConfirm) {
            state = when (state.stage) {
                DriveStage.Pickup -> state.copy(needsConfirm = true, lastMessage = "已到达接送点，请确认乘客上车")
                DriveStage.Trip -> state.copy(needsConfirm = true, lastMessage = "已到达目的地，请确认到达")
                else -> state
            }
        }
        return state
    }

    /** 用户手动确认到达 */
    fun confirmArrival(): ZenDriveUiState {
        state = when (state.stage) {
            DriveStage.Pickup -> state.copy(stage = DriveStage.WaitingPassenger, needsConfirm = false, lastMessage = "等待乘客上车")
            DriveStage.Trip -> state.copy(stage = DriveStage.Arriving, needsConfirm = false, lastMessage = "行程结束，正在结算")
            else -> state.copy(needsConfirm = false)
        }
        return state
    }

    fun passengerBoarded(): ZenDriveUiState {
        val order = state.order ?: return state
        state = state.copy(
            stage = DriveStage.Trip,
            lastMessage = "${order.passengerName}已上车"
        )
        return state
    }

    fun completeOrder(): ZenDriveUiState {
        val nextRelayIndex = if (state.selectedScene == DriveScene.RouteRelay) {
            (state.relayIndex + 1) % relayCheckpoints.size
        } else {
            state.relayIndex
        }
        state = state.copy(
            stage = DriveStage.Complete,
            garage = state.garage.reward(),
            relayIndex = nextRelayIndex,
            distanceToTargetMeters = null,
            lastMessage = "本次模拟行程已结束"
        )
        return state
    }

    fun finishShift(): ZenDriveUiState {
        state = state.copy(
            stage = DriveStage.Offline,
            order = null,
            distanceToTargetMeters = null,
            lastMessage = "已收车"
        )
        return state
    }

    fun unlockGarageItem(itemId: String): ZenDriveUiState {
        state = state.copy(garage = state.garage.unlock(itemId))
        return state
    }

    fun randomChat(): String = chats.random()

    // ── 管理员模拟方法 ──

    fun simulateToPickup(): ZenDriveUiState {
        val order = state.order ?: return state
        state = state.copy(
            stage = DriveStage.WaitingPassenger,
            distanceToTargetMeters = 0.0,
            lastMessage = "[模拟] 已到达接人点"
        )
        return state
    }

    fun simulateBoarded(): ZenDriveUiState {
        val order = state.order ?: return state
        state = state.copy(
            stage = DriveStage.Trip,
            distanceToTargetMeters = order.pickup.distanceMetersTo(order.destination),
            lastMessage = "[模拟] ${order.passengerName}已上车"
        )
        return state
    }

    fun simulateArriveDest(): ZenDriveUiState {
        state = state.copy(
            stage = DriveStage.Arriving,
            distanceToTargetMeters = 0.0,
            lastMessage = "[模拟] 已到达目的地"
        )
        return state
    }

    fun simulateComplete(): ZenDriveUiState {
        val nextRelayIndex = if (state.selectedScene == DriveScene.RouteRelay) {
            (state.relayIndex + 1) % 4
        } else state.relayIndex
        state = state.copy(
            stage = DriveStage.Complete,
            garage = state.garage.reward(),
            relayIndex = nextRelayIndex,
            distanceToTargetMeters = null,
            lastMessage = "[模拟] 行程已完成"
        )
        return state
    }

    // ── 气泡订单 ──

    fun generateBubbleOrders(current: GeoPoint): List<VirtualOrder> {
        return (1..4).map { i ->
            val angle = i * 72.0 + kotlin.random.Random.nextDouble(-20.0, 20.0)
            val dist = 400.0 + kotlin.random.Random.nextDouble(300.0, 2000.0)
            val pickup = current.shiftedBy(
                dist * kotlin.math.cos(Math.toRadians(angle)),
                dist * kotlin.math.sin(Math.toRadians(angle))
            )
            val dest = pickup.shiftedBy(
                800.0 + kotlin.random.Random.nextDouble(-300.0, 500.0),
                500.0 + kotlin.random.Random.nextDouble(-400.0, 400.0)
            )
            VirtualOrder(
                pickup = pickup,
                destination = dest,
                title = listOf("顺路小单", "轻松长途", "夜景专线", "短途快送", "风景漫游")[i - 1],
                passengerName = passengers[i % passengers.size],
                checkpointIndex = i - 1
            )
        }
    }

    fun selectBubbleOrder(order: VirtualOrder): ZenDriveUiState {
        state = state.copy(
            stage = DriveStage.Pickup,
            order = order,
            lastMessage = "已接单：${order.title}",
            distanceToTargetMeters = null // 将由 updateLocation 计算
        )
        return state
    }

    private fun randomize(offset: Pair<Double, Double>): Pair<Double, Double> {
        val factor = Random.nextDouble(0.85, 1.25)
        val drift = Random.nextDouble(-180.0, 180.0)
        return (offset.first * factor + drift) to (offset.second * factor - drift)
    }

    companion object {
        const val ARRIVAL_RADIUS_METERS = 20.0
    }
}
