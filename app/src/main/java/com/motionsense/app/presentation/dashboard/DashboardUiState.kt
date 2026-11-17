package com.motionsense.app.presentation.dashboard

import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorMetadata
import com.motionsense.app.domain.model.MonitoringState

/**
 * Immutable UI state model for the MotionSense telemetry dashboard.
 * Separates Live Telemetry Monitoring from Local Database Recording.
 */
data class DashboardUiState(
    // Live Monitoring State
    val monitoringState: MonitoringState = MonitoringState.IDLE,
    val sessionDurationSeconds: Long = 0L,
    val totalSamplesProcessed: Long = 0L,
    val currentSampleRateHz: Float = 0f,

    // Local Database Recording State
    val isRecording: Boolean = false,
    val activeRecordingSessionId: Long? = null,
    val activeRecordingSessionName: String = "",
    val recordingDurationSeconds: Long = 0L,
    val recordedSamplesCount: Long = 0L,
    val showStartRecordingDialog: Boolean = false,

    // Hardware metadata
    val accelerometerMetadata: SensorMetadata? = null,
    val gyroscopeMetadata: SensorMetadata? = null,

    // Live single-point readings
    val currentAccelerometerReading: SensorDataPoint? = null,
    val currentGyroscopeReading: SensorDataPoint? = null,

    // Bounded rolling history for real-time visualization (max 50 points)
    val accelerometerHistory: List<SensorDataPoint> = emptyList(),
    val gyroscopeHistory: List<SensorDataPoint> = emptyList(),

    // System info
    val deviceModel: String = "",
    val androidVersion: String = ""
) {
    val isMonitoring: Boolean
        get() = monitoringState == MonitoringState.MONITORING

    val formattedDuration: String
        get() = formatSeconds(sessionDurationSeconds)

    val formattedRecordingDuration: String
        get() = formatSeconds(recordingDurationSeconds)

    private fun formatSeconds(sec: Long): String {
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
