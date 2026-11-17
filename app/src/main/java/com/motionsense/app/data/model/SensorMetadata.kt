package com.motionsense.app.data.model

/**
 * Detailed hardware metadata for an on-device sensor.
 */
data class SensorMetadata(
    val sensorType: SensorType,
    val isAvailable: Boolean,
    val name: String = "Unavailable",
    val vendor: String = "N/A",
    val version: Int = 0,
    val powerMa: Float = 0f,
    val maxRange: Float = 0f,
    val resolution: Float = 0f,
    val minDelayUs: Int = 0,
    val maxDelayUs: Int = 0,
    val reportingMode: String = "N/A"
)
