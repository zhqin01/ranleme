package com.zendrive.simulator.data.repository

import com.zendrive.simulator.data.db.dao.GarageDao
import com.zendrive.simulator.data.db.entity.GarageItemEntity
import com.zendrive.simulator.data.prefs.AppPreferences
import com.zendrive.simulator.domain.GarageItem
import com.zendrive.simulator.domain.GarageState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GarageRepository(
    private val garageDao: GarageDao,
    private val prefs: AppPreferences
) {

    /** 默认车库道具列表 */
    private val defaultItems = listOf(
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

    /** 初始化车库（仅在首次启动时调用） */
    suspend fun initIfEmpty() {
        val existing = garageDao.getAll()
        if (existing.isEmpty()) {
            garageDao.insertAll(defaultItems.map { it.toEntity() })
        }
    }

    val garageState: Flow<GarageState> = combine(
        garageDao.getAllFlow(),
        prefs.coins
    ) { entities, coinsBalance ->
        val items = entities.map { it.toDomain() }
        GarageState(
            coins = coinsBalance,
            items = items
        )
    }

    @Synchronized
    suspend fun unlockItem(itemId: String): Boolean {
        val item = garageDao.getAll().firstOrNull { it.id == itemId } ?: return false
        if (item.unlocked) return false
        // 先扣金币，成功再解锁（避免金币扣了但解锁失败）
        if (!prefs.spendCoins(item.price)) return false
        garageDao.unlock(itemId)
        return true
    }

    private fun GarageItem.toEntity() = GarageItemEntity(
        id = id,
        name = name,
        price = price,
        unlocked = unlocked
    )

    private fun GarageItemEntity.toDomain() = GarageItem(
        id = id,
        name = name,
        price = price,
        unlocked = unlocked
    )
}
