package com.zendrive.simulator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zendrive.simulator.data.db.entity.TripRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(record: TripRecordEntity): Long

    @Query("SELECT * FROM trip_records ORDER BY completedAtMillis DESC")
    fun getAllFlow(): Flow<List<TripRecordEntity>>

    @Query("SELECT * FROM trip_records ORDER BY completedAtMillis DESC")
    suspend fun getAll(): List<TripRecordEntity>

    @Query("SELECT COUNT(*) FROM trip_records")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(estimatedDistanceMeters), 0) FROM trip_records")
    suspend fun totalDistanceMeters(): Double

    @Query("SELECT COALESCE(SUM(coinsEarned), 0) FROM trip_records")
    suspend fun totalCoins(): Int

    @Query("DELETE FROM trip_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
