package com.zendrive.simulator.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zendrive.simulator.data.db.dao.GarageDao
import com.zendrive.simulator.data.db.dao.TripDao
import com.zendrive.simulator.data.db.entity.GarageItemEntity
import com.zendrive.simulator.data.db.entity.TripRecordEntity

@Database(
    entities = [TripRecordEntity::class, GarageItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun garageDao(): GarageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ranleme.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
