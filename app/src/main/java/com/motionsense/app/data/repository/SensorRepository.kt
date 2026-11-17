package com.motionsense.app.data.repository

import android.hardware.SensorManager
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorMetadata
import com.motionsense.app.data.model.SensorType
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining access to hardware sensors and their reactive data streams.
 */
interface SensorRepository {
    /**
     * Checks whether the specified sensor type is physically present on the device.
     */
    fun isSensorAvailable(sensorType: SensorType): Boolean

    /**
     * Retrieves static hardware specifications and metadata for a sensor.
     */
    fun getSensorMetadata(sensorType: SensorType): SensorMetadata

    /**
     * Creates a cold reactive stream of sensor readings.
     * The underlying [android.hardware.SensorEventListener] is registered only when the Flow
     * is collected and is guaranteed to be unregistered when collection stops or is cancelled.
     *
     * @param sensorType The sensor to stream (Accelerometer or Gyroscope)
     * @param samplingPeriodUs Sampling delay hint in microseconds (e.g. SENSOR_DELAY_GAME)
     */
    fun getSensorStream(
        sensorType: SensorType,
        samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME
    ): Flow<SensorDataPoint>
}
