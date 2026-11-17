package com.motionsense.app.data.repository

import com.motionsense.app.data.local.MotionSenseDatabase
import com.motionsense.app.data.local.entity.SensorReadingEntity
import com.motionsense.app.data.local.entity.SensorSessionEntity
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.domain.model.SensorStatistics
import com.motionsense.app.domain.model.SessionDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class RecordingRepositoryImpl(
    private val database: MotionSenseDatabase
) : RecordingRepository {

    private val sessionDao = database.sensorSessionDao()
    private val readingDao = database.sensorReadingDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // High-frequency in-memory buffer before batch insertion
    private val writeBuffer = ConcurrentLinkedQueue<SensorReadingEntity>()
    private val bufferedCount = AtomicInteger(0)

    companion object {
        const val BATCH_FLUSH_THRESHOLD = 100
    }

    override suspend fun startRecording(sessionName: String, deviceModel: String): Long = withContext(Dispatchers.IO) {
        val session = SensorSessionEntity(
            sessionName = sessionName.ifBlank { "Session-${System.currentTimeMillis()}" },
            startedAt = System.currentTimeMillis(),
            deviceModel = deviceModel,
            status = SensorSessionEntity.STATUS_ACTIVE
        )
        sessionDao.insertSession(session)
    }

    override fun enqueueReading(sessionId: Long, dataPoint: SensorDataPoint) {
        val entity = SensorReadingEntity(
            sessionId = sessionId,
            sensorType = dataPoint.sensorType.name,
            x = dataPoint.x,
            y = dataPoint.y,
            z = dataPoint.z,
            magnitude = dataPoint.magnitude,
            timestampNs = dataPoint.timestampNs,
            accuracy = dataPoint.accuracy.ordinal,
            receivedAtMs = dataPoint.receivedAtMs
        )
        writeBuffer.add(entity)
        val count = bufferedCount.incrementAndGet()

        // Immediate async flush if buffer threshold exceeded
        if (count >= BATCH_FLUSH_THRESHOLD) {
            repositoryScope.launch {
                flushPendingReadings()
            }
        }
    }

    override suspend fun flushPendingReadings() = withContext(Dispatchers.IO) {
        if (writeBuffer.isEmpty()) return@withContext

        val batch = mutableListOf<SensorReadingEntity>()
        while (!writeBuffer.isEmpty()) {
            writeBuffer.poll()?.let {
                batch.add(it)
                bufferedCount.decrementAndGet()
            }
        }

        if (batch.isNotEmpty()) {
            readingDao.insertBatch(batch)
        }
    }

    override suspend fun stopRecording(sessionId: Long): SensorSessionEntity? = withContext(Dispatchers.IO) {
        // Guaranteed flush of remaining buffer
        flushPendingReadings()

        val session = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val duration = now - session.startedAt

        val accCount = readingDao.getReadingCountForSessionBySensor(sessionId, SensorType.ACCELEROMETER.name)
        val gyroCount = readingDao.getReadingCountForSessionBySensor(sessionId, SensorType.GYROSCOPE.name)

        val finalizedSession = session.copy(
            endedAt = now,
            durationMs = duration,
            status = SensorSessionEntity.STATUS_COMPLETED,
            accelerometerSamples = accCount,
            gyroscopeSamples = gyroCount
        )

        sessionDao.updateSession(finalizedSession)
        finalizedSession
    }

    override fun getAllSessions(): Flow<List<SensorSessionEntity>> {
        return sessionDao.getAllSessionsFlow()
    }

    override suspend fun getSessionDetails(sessionId: Long): SessionDetails? = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val accReadings = readingDao.getReadingsForSessionBySensor(sessionId, SensorType.ACCELEROMETER.name)
        val gyroReadings = readingDao.getReadingsForSessionBySensor(sessionId, SensorType.GYROSCOPE.name)

        val accStats = computeStatistics(accReadings)
        val gyroStats = computeStatistics(gyroReadings)

        SessionDetails(
            session = session,
            accelerometerReadings = accReadings,
            gyroscopeReadings = gyroReadings,
            accelerometerStats = accStats,
            gyroscopeStats = gyroStats
        )
    }

    override suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        sessionDao.deleteSessionById(sessionId)
        Unit
    }

    override suspend fun exportSessionToCsv(sessionId: Long, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val readings = readingDao.getReadingsForSession(sessionId)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            // CSV Header
            writer.write("Session_ID,Timestamp_Ns,Received_At_Epoch_Ms,Sensor_Type,Axis_X,Axis_Y,Axis_Z,Magnitude,Accuracy\n")

            for (r in readings) {
                writer.write(
                    String.format(
                        Locale.US,
                        "%d,%d,%d,%s,%.6f,%.6f,%.6f,%.6f,%d\n",
                        r.sessionId,
                        r.timestampNs,
                        r.receivedAtMs,
                        r.sensorType,
                        r.x,
                        r.y,
                        r.z,
                        r.magnitude,
                        r.accuracy
                    )
                )
            }
            writer.flush()
        }
    }

    private fun computeStatistics(readings: List<SensorReadingEntity>): SensorStatistics {
        if (readings.isEmpty()) return SensorStatistics()

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var sumX = 0.0

        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var sumY = 0.0

        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var sumZ = 0.0

        var peakMag = 0f
        var sumMag = 0.0

        for (r in readings) {
            minX = min(minX, r.x)
            maxX = max(maxX, r.x)
            sumX += r.x

            minY = min(minY, r.y)
            maxY = max(maxY, r.y)
            sumY += r.y

            minZ = min(minZ, r.z)
            maxZ = max(maxZ, r.z)
            sumZ += r.z

            peakMag = max(peakMag, r.magnitude)
            sumMag += r.magnitude
        }

        val count = readings.size
        return SensorStatistics(
            minX = minX,
            maxX = maxX,
            avgX = (sumX / count).toFloat(),
            minY = minY,
            maxY = maxY,
            avgY = (sumY / count).toFloat(),
            minZ = minZ,
            maxZ = maxZ,
            avgZ = (sumZ / count).toFloat(),
            peakMagnitude = peakMag,
            avgMagnitude = (sumMag / count).toFloat(),
            sampleCount = count
        )
    }
}
