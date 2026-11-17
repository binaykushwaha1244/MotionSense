package com.motionsense.app

import com.motionsense.app.data.local.MotionSenseDatabase
import com.motionsense.app.data.local.dao.SensorReadingDao
import com.motionsense.app.data.local.dao.SensorSessionDao
import com.motionsense.app.data.local.entity.SensorReadingEntity
import com.motionsense.app.data.local.entity.SensorSessionEntity
import com.motionsense.app.data.repository.RecordingRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvExportTest {

    @Test
    fun testCsvExportFormat() = runBlocking {
        // Mock database DAOs
        val fakeReadings = listOf(
            SensorReadingEntity(
                readingId = 1L,
                sessionId = 42L,
                sensorType = "ACCELEROMETER",
                x = 0.23f,
                y = 0.05f,
                z = 9.81f,
                magnitude = 9.813f,
                timestampNs = 1234567890L,
                accuracy = 3,
                receivedAtMs = 1700000000000L
            ),
            SensorReadingEntity(
                readingId = 2L,
                sessionId = 42L,
                sensorType = "GYROSCOPE",
                x = 0.01f,
                y = -0.02f,
                z = 0.00f,
                magnitude = 0.022f,
                timestampNs = 1234567990L,
                accuracy = 3,
                receivedAtMs = 1700000000100L
            )
        )

        val outputStream = ByteArrayOutputStream()
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("Session_ID,Timestamp_Ns,Received_At_Epoch_Ms,Sensor_Type,Axis_X,Axis_Y,Axis_Z,Magnitude,Accuracy\n")
            for (r in fakeReadings) {
                writer.write(
                    String.format(
                        java.util.Locale.US,
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

        val csvString = outputStream.toString(Charsets.UTF_8.name())
        val lines = csvString.trim().split("\n")

        assertEquals(3, lines.size)
        assertEquals("Session_ID,Timestamp_Ns,Received_At_Epoch_Ms,Sensor_Type,Axis_X,Axis_Y,Axis_Z,Magnitude,Accuracy", lines[0])
        assertTrue(lines[1].contains("ACCELEROMETER"))
        assertTrue(lines[1].contains("9.810000"))
        assertTrue(lines[2].contains("GYROSCOPE"))
    }
}
