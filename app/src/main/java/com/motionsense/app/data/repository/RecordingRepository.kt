package com.motionsense.app.data.repository

import com.motionsense.app.data.local.entity.SensorSessionEntity
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.domain.model.SessionDetails
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

/**
 * Repository interface for telemetry session recording, batched persistence,
 * history queries, and CSV file exports.
 */
interface RecordingRepository {

    /**
     * Creates and starts a new recording session.
     * @return The unique auto-generated session ID.
     */
    suspend fun startRecording(sessionName: String, deviceModel: String): Long

    /**
     * Enqueues a live sensor data point into the in-memory write buffer.
     * Batches are periodically written to Room on Dispatchers.IO.
     */
    fun enqueueReading(sessionId: Long, dataPoint: SensorDataPoint)

    /**
     * Flushes all buffered readings to SQLite immediately.
     */
    suspend fun flushPendingReadings()

    /**
     * Stops the active recording session, flushes all remaining readings,
     * updates sample tallies, and finalizes the session status to COMPLETED.
     */
    suspend fun stopRecording(sessionId: Long): SensorSessionEntity?

    /**
     * Reactive stream of all recording sessions ordered by newest first.
     */
    fun getAllSessions(): Flow<List<SensorSessionEntity>>

    /**
     * Retrieves full session metadata, associated readings, and computed statistical metrics.
     */
    suspend fun getSessionDetails(sessionId: Long): SessionDetails?

    /**
     * Deletes a session and triggers CASCADE deletion of all its sensor readings.
     */
    suspend fun deleteSession(sessionId: Long)

    /**
     * Exports a recorded session to CSV format writing directly to the provided OutputStream.
     */
    suspend fun exportSessionToCsv(sessionId: Long, outputStream: OutputStream)
}
