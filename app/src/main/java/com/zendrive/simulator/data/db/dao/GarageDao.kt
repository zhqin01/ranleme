package com.zendrive.simulator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zendrive.simulator.data.db.entity.GarageItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GarageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GarageItemEntity>)

    @Query("SELECT * FROM garage_items")
    fun getAllFlow(): Flow<List<GarageItemEntity>>

    @Query("SELECT * FROM garage_items")
    suspend fun getAll(): List<GarageItemEntity>

    @Query("UPDATE garage_items SET unlocked = 1 WHERE id = :id")
    suspend fun unlock(id: String)

    @Query("SELECT COALESCE(SUM(CASE WHEN unlocked = 1 THEN price ELSE 0 END), 0) FROM garage_items")
    suspend fun totalSpent(): Int
}
