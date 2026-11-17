package com.motionsense.app.data.model

import android.hardware.SensorManager

/**
 * Human-readable mapping of Android SensorManager accuracy constants.
 */
enum class SensorAccuracy(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    UNRELIABLE("Unreliable"),
    UNKNOWN("Unknown");

    companion object {
        fun fromInt(accuracy: Int): SensorAccuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> UNRELIABLE
            else -> UNKNOWN
        }
    }
}
