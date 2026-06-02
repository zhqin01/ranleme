package com.zendrive.simulator.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garage_items")
data class GarageItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Int,
    val unlocked: Boolean
)
