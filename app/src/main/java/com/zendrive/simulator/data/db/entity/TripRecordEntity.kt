package com.zendrive.simulator.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_records")
data class TripRecordEntity(
    @PrimaryKey(autoGenerate = true)
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
