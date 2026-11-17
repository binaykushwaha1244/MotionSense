package com.motionsense.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an individual telemetry recording session.
 */
@Entity(tableName = "sensor_sessions")
data class SensorSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    val sessionName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0L,
    val deviceModel: String,
    val status: String = STATUS_ACTIVE,
    val accelerometerSamples: Int = 0,
    val gyroscopeSamples: Int = 0
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
    }

    val totalSamples: Int
        get() = accelerometerSamples + gyroscopeSamples

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
