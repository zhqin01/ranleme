package com.zendrive.simulator.data.repository

import com.zendrive.simulator.data.db.dao.TripDao
import com.zendrive.simulator.data.db.entity.TripRecordEntity
import com.zendrive.simulator.domain.TripRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripRecord>> = tripDao.getAllFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(record: TripRecord): Long {
        return tripDao.insert(
            TripRecordEntity(
                sceneTitle = record.sceneTitle,
                passengerName = record.passengerName,
                orderTitle = record.orderTitle,
                startLat = record.startLat,
                startLng = record.startLng,
                endLat = record.endLat,
                endLng = record.endLng,
                estimatedDistanceMeters = record.estimatedDistanceMeters,
                coinsEarned = record.coinsEarned,
                completedAtMillis = record.completedAtMillis
            )
        )
    }

    suspend fun totalOrders(): Int = tripDao.count()

    suspend fun totalDistanceMeters(): Double = tripDao.totalDistanceMeters()

    suspend fun totalCoinsEarned(): Int = tripDao.totalCoins()

    suspend fun deleteById(id: Long) = tripDao.deleteById(id)

    private fun TripRecordEntity.toDomain() = TripRecord(
        id = id,
        sceneTitle = sceneTitle,
        passengerName = passengerName,
        orderTitle = orderTitle,
        startLat = startLat,
        startLng = startLng,
        endLat = endLat,
        endLng = endLng,
        estimatedDistanceMeters = estimatedDistanceMeters,
        coinsEarned = coinsEarned,
        completedAtMillis = completedAtMillis
    )
}
