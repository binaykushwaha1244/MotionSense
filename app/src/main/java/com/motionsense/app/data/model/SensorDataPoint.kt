package com.motionsense.app.data.model

import kotlin.math.sqrt

/**
 * Immutable representation of a single 3-axis motion sensor event.
 *
 * @property sensorType The origin sensor (Accelerometer or Gyroscope)
 * @property x Measured value along the X-axis (m/s² for acc, rad/s for gyro)
 * @property y Measured value along the Y-axis (m/s² for acc, rad/s for gyro)
 * @property z Measured value along the Z-axis (m/s² for acc, rad/s for gyro)
 * @property timestampNs Hardware sensor event timestamp in nanoseconds since boot
 * @property accuracy Hardware reporting accuracy state
 * @property receivedAtMs System epoch time in milliseconds when the event was received
 */
data class SensorDataPoint(
    val sensorType: SensorType,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNs: Long,
    val accuracy: SensorAccuracy,
    val receivedAtMs: Long = System.currentTimeMillis()
) {
    /**
     * Euclidean vector magnitude: sqrt(x² + y² + z²).
     * For Accelerometer at rest, magnitude ≈ 9.81 m/s² (Earth gravity 1g).
     * For Gyroscope at rest, magnitude ≈ 0.00 rad/s.
     */
    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z)
}
