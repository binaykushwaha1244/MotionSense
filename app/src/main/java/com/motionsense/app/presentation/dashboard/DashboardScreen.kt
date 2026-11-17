package com.motionsense.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.presentation.dashboard.components.MonitoringControls
import com.motionsense.app.presentation.dashboard.components.SensorCard
import com.motionsense.app.presentation.dashboard.components.StartRecordingDialog
import com.motionsense.app.presentation.dashboard.components.TelemetryHeader
import com.motionsense.app.viewmodel.TelemetryViewModel

/**
 * Main dashboard screen rendering real-time smartphone telemetry and recording controls.
 */
@Composable
fun DashboardScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showStartRecordingDialog) {
        StartRecordingDialog(
            onDismiss = { viewModel.dismissStartRecordingDialog() },
            onConfirm = { name -> viewModel.startRecording(name) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Telemetry Header & Branding
        item {
            TelemetryHeader(uiState = uiState)
        }

        // Monitoring & Recording Controls Bar
        item {
            MonitoringControls(
                isMonitoring = uiState.isMonitoring,
                isRecording = uiState.isRecording,
                recordingDurationText = uiState.formattedRecordingDuration,
                recordedSamplesCount = uiState.recordedSamplesCount,
                onStartMonitoring = { viewModel.startMonitoring() },
                onStopMonitoring = { viewModel.stopMonitoring() },
                onResetMonitoring = { viewModel.resetLiveTelemetry() },
                onStartRecording = { viewModel.openStartRecordingDialog() },
                onStopRecording = { viewModel.stopRecording() }
            )
        }

        // Accelerometer Telemetry Card
        item {
            SensorCard(
                sensorType = SensorType.ACCELEROMETER,
                metadata = uiState.accelerometerMetadata,
                reading = uiState.currentAccelerometerReading,
                history = uiState.accelerometerHistory,
                isMonitoring = uiState.isMonitoring,
                icon = Icons.Default.Speed
            )
        }

        // Gyroscope Telemetry Card
        item {
            SensorCard(
                sensorType = SensorType.GYROSCOPE,
                metadata = uiState.gyroscopeMetadata,
                reading = uiState.currentGyroscopeReading,
                history = uiState.gyroscopeHistory,
                isMonitoring = uiState.isMonitoring,
                icon = Icons.AutoMirrored.Filled.RotateRight
            )
        }
    }
}
