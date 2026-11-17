package com.motionsense.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.motionsense.app.data.local.entity.SensorSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SensorSessionEntity): Long

    @Update
    suspend fun updateSession(session: SensorSessionEntity): Int

    @Delete
    suspend fun deleteSession(session: SensorSessionEntity): Int

    @Query("DELETE FROM sensor_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: Long): Int

    @Query("SELECT * FROM sensor_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SensorSessionEntity?

    @Query("SELECT * FROM sensor_sessions ORDER BY startedAt DESC")
    fun getAllSessionsFlow(): Flow<List<SensorSessionEntity>>

    @Query("SELECT * FROM sensor_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): SensorSessionEntity?
}
