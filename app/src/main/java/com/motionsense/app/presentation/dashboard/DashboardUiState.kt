package com.motionsense.app.presentation.dashboard

import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorMetadata
import com.motionsense.app.domain.model.MonitoringState

/**
 * Immutable UI state model for the MotionSense telemetry dashboard.
 */
data class DashboardUiState(
    val monitoringState: MonitoringState = MonitoringState.IDLE,
    val sessionDurationSeconds: Long = 0L,
    val totalSamplesProcessed: Long = 0L,
    val currentSampleRateHz: Float = 0f,

    // Hardware metadata
    val accelerometerMetadata: SensorMetadata? = null,
    val gyroscopeMetadata: SensorMetadata? = null,

    // Live single-point readings
    val currentAccelerometerReading: SensorDataPoint? = null,
    val currentGyroscopeReading: SensorDataPoint? = null,

    // Bounded rolling history for real-time visualization (max 50-60 points)
    val accelerometerHistory: List<SensorDataPoint> = emptyList(),
    val gyroscopeHistory: List<SensorDataPoint> = emptyList(),

    // System info
    val deviceModel: String = "",
    val androidVersion: String = ""
) {
    val isMonitoring: Boolean
        get() = monitoringState == MonitoringState.MONITORING

    /**
     * Formats duration in mm:ss or hh:mm:ss.
     */
    val formattedDuration: String
        get() {
            val hours = sessionDurationSeconds / 3600
            val minutes = (sessionDurationSeconds % 3600) / 60
            val seconds = sessionDurationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
