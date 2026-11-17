package com.motionsense.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an individual recorded sensor measurement.
 *
 * Configured with CASCADE deletion from [SensorSessionEntity] and targeted indexes
 * for high-speed chronological queries.
 */
@Entity(
    tableName = "sensor_readings",
    foreignKeys = [
        ForeignKey(
            entity = SensorSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestampNs"]),
        Index(value = ["sessionId", "sensorType"])
    ]
)
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val readingId: Long = 0L,
    val sessionId: Long,
    val sensorType: String, // "ACCELEROMETER" or "GYROSCOPE"
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val timestampNs: Long,
    val accuracy: Int,
    val receivedAtMs: Long
)
