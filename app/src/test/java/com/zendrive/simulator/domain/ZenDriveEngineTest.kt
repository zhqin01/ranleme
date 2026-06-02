package com.zendrive.simulator.domain

import com.zendrive.simulator.domain.DriveScene
import com.zendrive.simulator.domain.DriveStage
import com.zendrive.simulator.domain.GeoPoint
import com.zendrive.simulator.domain.ZenDriveUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZenDriveEngineTest {

    private lateinit var engine: ZenDriveEngine
    private val testLocation = GeoPoint(29.56376, 106.55156)

    @Before
    fun setUp() {
        engine = ZenDriveEngine()
    }

    // ── 初始状态 ──

    @Test
    fun `初始状态为Offline`() {
        assertEquals(DriveStage.Offline, engine.state.stage)
    }

    @Test
    fun `初始场景为CityNeon`() {
        assertEquals(DriveScene.CityNeon, engine.state.selectedScene)
    }

    // ── 场景选择 ──

    @Test
    fun `选择场景后状态更新`() {
        engine.selectScene(DriveScene.MountainCurve)
        assertEquals(DriveScene.MountainCurve, engine.state.selectedScene)
    }

    // ── 派单流程 ──

    @Test
    fun `开始接单后状态变为Dispatching`() {
        engine.beginDispatch(testLocation)
        assertEquals(DriveStage.Dispatching, engine.state.stage)
    }

    @Test
    fun `GPS未就绪时开始接单给出提示`() {
        engine.beginDispatch(null)
        assertEquals("等待 GPS 定位", engine.state.lastMessage)
    }

    @Test
    fun `分配订单后状态变为Pickup`() {
        engine.beginDispatch(testLocation)
        engine.assignOrder(testLocation)
        assertEquals(DriveStage.Pickup, engine.state.stage)
        assertNotNull(engine.state.order)
    }

    @Test
    fun `订单包含乘客和标题`() {
        engine.beginDispatch(testLocation)
        engine.assignOrder(testLocation)
        val order = engine.state.order
        assertNotNull(order)
        assertTrue(order!!.passengerName.isNotEmpty())
        assertTrue(order.title.isNotEmpty())
    }

    // ── 到点判定 ──

    @Test
    fun `距离目标20米以内触发下一阶段_Pickup到WaitingPassenger`() {
        engine.beginDispatch(testLocation)
        engine.assignOrder(testLocation)
        val pickup = engine.state.order!!.pickup
        // 移动到恰好接人点位置
        engine.updateLocation(pickup)
        assertEquals(DriveStage.WaitingPassenger, engine.state.stage)
    }

    @Test
    fun `距离目标超过20米不触发阶段变化`() {
        engine.beginDispatch(testLocation)
        engine.assignOrder(testLocation)
        val farAway = testLocation // 原始位置远离 pickup
        val state = engine.updateLocation(farAway)
        assertEquals(DriveStage.Pickup, state.stage)
    }

    // ── 完单 ──

    @Test
    fun `完成订单后获得金币`() {
        engine.beginDispatch(testLocation)
        engine.assignOrder(testLocation)
        val pickup = engine.state.order!!.pickup
        engine.updateLocation(pickup) // 到达接人点
        assertEquals(DriveStage.WaitingPassenger, engine.state.stage)

        engine.passengerBoarded()
        assertEquals(DriveStage.Trip, engine.state.stage)

        val dest = engine.state.order!!.destination
        engine.updateLocation(dest) // 到达目的地
        assertEquals(DriveStage.Arriving, engine.state.stage)

        val state = engine.completeOrder()
        assertEquals(DriveStage.Complete, state.stage)
        assertEquals(60, state.garage.coins)
    }

    // ── 取消 ──

    @Test
    fun `任意状态可取消收车`() {
        engine.beginDispatch(testLocation)
        val state = engine.finishShift()
        assertEquals(DriveStage.Offline, state.stage)
        assertNull(state.order)
    }

    // ── 随机闲聊 ──

    @Test
    fun `randomChat返回非空字符串`() {
        val chat = engine.randomChat()
        assertTrue(chat.isNotEmpty())
    }

    // ── 车库解锁 ──

    @Test
    fun `金币不足无法解锁`() {
        val state = engine.unlockGarageItem("frame_gold")
        assertTrue(state.garage.items.first { it.id == "frame_gold" }.unlocked == false)
    }

    @Test
    fun `完单后金币足够可解锁`() {
        // 先积攒足够金币
        repeat(4) {
            engine.beginDispatch(testLocation)
            engine.assignOrder(testLocation)
            val pickup = engine.state.order!!.pickup
            engine.updateLocation(pickup)
            engine.confirmArrival()
            engine.passengerBoarded()
            val dest = engine.state.order!!.destination
            engine.updateLocation(dest)
            engine.confirmArrival()
            engine.completeOrder()
        }
        // 4单 × 60金币 = 240金币，解锁 200 的 frame_gold
        val state = engine.unlockGarageItem("frame_gold")
        assertTrue(state.garage.items.first { it.id == "frame_gold" }.unlocked)
    }
}
