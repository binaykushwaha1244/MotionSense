package com.motionsense.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.motionsense.app.data.local.entity.SensorReadingEntity

@Dao
interface SensorReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(readings: List<SensorReadingEntity>): List<Long>

    @Query("SELECT * FROM sensor_readings WHERE sessionId = :sessionId ORDER BY timestampNs ASC")
    suspend fun getReadingsForSession(sessionId: Long): List<SensorReadingEntity>

    @Query("SELECT * FROM sensor_readings WHERE sessionId = :sessionId AND sensorType = :sensorType ORDER BY timestampNs ASC")
    suspend fun getReadingsForSessionBySensor(sessionId: Long, sensorType: String): List<SensorReadingEntity>

    @Query("SELECT COUNT(*) FROM sensor_readings WHERE sessionId = :sessionId")
    suspend fun getReadingCountForSession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM sensor_readings WHERE sessionId = :sessionId AND sensorType = :sensorType")
    suspend fun getReadingCountForSessionBySensor(sessionId: Long, sensorType: String): Int

    @Query("DELETE FROM sensor_readings WHERE sessionId = :sessionId")
    suspend fun deleteReadingsForSession(sessionId: Long): Int
}
