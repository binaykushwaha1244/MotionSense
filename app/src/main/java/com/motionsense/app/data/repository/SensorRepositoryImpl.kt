package com.motionsense.app.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.motionsense.app.data.model.SensorAccuracy
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorMetadata
import com.motionsense.app.data.model.SensorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Concrete implementation of [SensorRepository] interacting directly with Android's [SensorManager].
 *
 * Utilizes Kotlin Coroutines [callbackFlow] to safely wrap asynchronous sensor callbacks into
 * lifecycle-safe reactive streams, ensuring zero memory leaks and proper unregistration.
 */
class SensorRepositoryImpl(
    context: Context
) : SensorRepository {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun isSensorAvailable(sensorType: SensorType): Boolean {
        return sensorManager.getDefaultSensor(sensorType.androidType) != null
    }

    override fun getSensorMetadata(sensorType: SensorType): SensorMetadata {
        val sensor = sensorManager.getDefaultSensor(sensorType.androidType)
            ?: return SensorMetadata(sensorType = sensorType, isAvailable = false)

        val reportingModeStr = when (sensor.reportingMode) {
            Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
            Sensor.REPORTING_MODE_ON_CHANGE -> "On Change"
            Sensor.REPORTING_MODE_ONE_SHOT -> "One Shot"
            Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special Trigger"
            else -> "Unknown"
        }

        return SensorMetadata(
            sensorType = sensorType,
            isAvailable = true,
            name = sensor.name.orEmpty().ifBlank { "Unknown" },
            vendor = sensor.vendor.orEmpty().ifBlank { "Unknown" },
            version = sensor.version,
            powerMa = sensor.power,
            maxRange = sensor.maximumRange,
            resolution = sensor.resolution,
            minDelayUs = sensor.minDelay,
            maxDelayUs = sensor.maxDelay,
            reportingMode = reportingModeStr
        )
    }

    override fun getSensorStream(
        sensorType: SensorType,
        samplingPeriodUs: Int
    ): Flow<SensorDataPoint> {
        val sensor = sensorManager.getDefaultSensor(sensorType.androidType) ?: return emptyFlow()

        return callbackFlow {
            var currentAccuracy = SensorAccuracy.UNKNOWN

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.values.size < 3) return

                    val dataPoint = SensorDataPoint(
                        sensorType = sensorType,
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        timestampNs = event.timestamp,
                        accuracy = currentAccuracy
                    )

                    // trySend drops events if the buffer is saturated, avoiding backpressure lag
                    trySend(dataPoint)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    currentAccuracy = SensorAccuracy.fromInt(accuracy)
                }
            }

            val registered = sensorManager.registerListener(
                listener,
                sensor,
                samplingPeriodUs
            )

            if (!registered) {
                close(IllegalStateException("Failed to register sensor listener for ${sensorType.displayName}"))
                return@callbackFlow
            }

            // Guaranteed cleanup when collector scope is cancelled or flow closes
            awaitClose {
                sensorManager.unregisterListener(listener, sensor)
            }
        }
    }
}
