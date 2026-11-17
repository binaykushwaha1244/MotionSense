package com.motionsense.app

import com.motionsense.app.data.model.SensorAccuracy
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SensorDataPointTest {

    @Test
    fun testMagnitudeCalculation_standardGravity() {
        // Accelerometer stationary on flat surface: X=0, Y=0, Z=9.81
        val point = SensorDataPoint(
            sensorType = SensorType.ACCELEROMETER,
            x = 0f,
            y = 0f,
            z = 9.81f,
            timestampNs = 1000000L,
            accuracy = SensorAccuracy.HIGH
        )

        assertEquals(9.81f, point.magnitude, 0.001f)
    }

    @Test
    fun testMagnitudeCalculation_3dVector() {
        // 3-4-5 right triangle extended to 3D: (1, 2, 2) -> sqrt(1+4+4) = 3
        val point = SensorDataPoint(
            sensorType = SensorType.ACCELEROMETER,
            x = 1f,
            y = 2f,
            z = 2f,
            timestampNs = 1000000L,
            accuracy = SensorAccuracy.HIGH
        )

        assertEquals(3.0f, point.magnitude, 0.001f)
    }

    @Test
    fun testAccuracyMapping() {
        assertEquals(SensorAccuracy.HIGH, SensorAccuracy.fromInt(3))
        assertEquals(SensorAccuracy.MEDIUM, SensorAccuracy.fromInt(2))
        assertEquals(SensorAccuracy.LOW, SensorAccuracy.fromInt(1))
        assertEquals(SensorAccuracy.UNRELIABLE, SensorAccuracy.fromInt(0))
        assertEquals(SensorAccuracy.UNKNOWN, SensorAccuracy.fromInt(-1))
    }
}
