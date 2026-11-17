package com.motionsense.app.data.model

import android.hardware.Sensor

/**
 * Supported motion sensor types for MotionSense telemetry.
 */
enum class SensorType(
    val androidType: Int,
    val displayName: String,
    val defaultUnit: String
) {
    ACCELEROMETER(
        androidType = Sensor.TYPE_ACCELEROMETER,
        displayName = "Accelerometer",
        defaultUnit = "m/s²"
    ),
    GYROSCOPE(
        androidType = Sensor.TYPE_GYROSCOPE,
        displayName = "Gyroscope",
        defaultUnit = "rad/s"
    )
}
