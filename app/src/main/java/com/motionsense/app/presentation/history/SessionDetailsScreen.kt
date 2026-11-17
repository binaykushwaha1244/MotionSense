package com.motionsense.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motionsense.app.data.local.entity.SensorReadingEntity
import com.motionsense.app.data.model.SensorAccuracy
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.domain.model.SensorStatistics
import com.motionsense.app.presentation.dashboard.components.TelemetryChart
import com.motionsense.app.presentation.theme.AxisXColor
import com.motionsense.app.presentation.theme.AxisYColor
import com.motionsense.app.presentation.theme.AxisZColor
import com.motionsense.app.presentation.theme.TelemetryNumberStyle
import com.motionsense.app.viewmodel.SessionHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(
    sessionId: Long,
    viewModel: SessionHistoryViewModel,
    onBack: () -> Unit,
    onExport: (sessionId: Long, fileName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val details by viewModel.selectedSessionDetails.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetails.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.loadSessionDetails(sessionId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = details?.session?.sessionName ?: "Session Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading || details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val sessionDetails = details!!
            val session = sessionDetails.session

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Overview Card
                item {
                    SessionSummaryCard(session = session)
                }

                // Accelerometer Statistics Card
                item {
                    SensorStatisticsCard(
                        title = "Accelerometer Statistics",
                        unit = "m/s²",
                        stats = sessionDetails.accelerometerStats
                    )
                }

                // Accelerometer Waveform Chart (Downsampled if necessary)
                item {
                    val accPoints = downsampleReadings(sessionDetails.accelerometerReadings, SensorType.ACCELEROMETER)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Accelerometer Waveform (${sessionDetails.accelerometerReadings.size} samples)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TelemetryChart(
                                history = accPoints,
                                unit = "m/s²",
                                minYRange = 12f
                            )
                        }
                    }
                }

                // Gyroscope Statistics Card
                item {
                    SensorStatisticsCard(
                        title = "Gyroscope Statistics",
                        unit = "rad/s",
                        stats = sessionDetails.gyroscopeStats
                    )
                }

                // Gyroscope Waveform Chart (Downsampled if necessary)
                item {
                    val gyroPoints = downsampleReadings(sessionDetails.gyroscopeReadings, SensorType.GYROSCOPE)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Gyroscope Waveform (${sessionDetails.gyroscopeReadings.size} samples)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TelemetryChart(
                                history = gyroPoints,
                                unit = "rad/s",
                                minYRange = 3f
                            )
                        }
                    }
                }

                // Export to CSV Action
                item {
                    Button(
                        onClick = {
                            val cleanName = session.sessionName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            onExport(session.sessionId, "MotionSense_${cleanName}.csv")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export to CSV"
                        )
                        Text(
                            text = "Export Session to CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(session: com.motionsense.app.data.local.entity.SensorSessionEntity) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US).format(Date(session.startedAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Session Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recorded At", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dateStr, style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Device Model", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(session.deviceModel, style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recording Duration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(session.formattedDuration, style = TelemetryNumberStyle.copy(fontSize = 13.sp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Measurements", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "%,d", session.totalSamples), style = TelemetryNumberStyle.copy(fontSize = 13.sp))
            }
        }
    }
}

@Composable
private fun SensorStatisticsCard(
    title: String,
    unit: String,
    stats: SensorStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Axis", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Min", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Max", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Average", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            }

            StatAxisRow(label = "X", color = AxisXColor, min = stats.minX, max = stats.maxX, avg = stats.avgX, unit = unit)
            StatAxisRow(label = "Y", color = AxisYColor, min = stats.minY, max = stats.maxY, avg = stats.avgY, unit = unit)
            StatAxisRow(label = "Z", color = AxisZColor, min = stats.minZ, max = stats.maxZ, avg = stats.avgZ, unit = unit)

            // Magnitude Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peak Magnitude: ${String.format(Locale.US, "%.2f %s", stats.peakMagnitude, unit)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Avg: ${String.format(Locale.US, "%.2f %s", stats.avgMagnitude, unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatAxisRow(
    label: String,
    color: Color,
    min: Float,
    max: Float,
    avg: Float,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Axis $label", style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.weight(1f))
        Text(text = String.format(Locale.US, "%+5.2f", min), style = TelemetryNumberStyle.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
        Text(text = String.format(Locale.US, "%+5.2f", max), style = TelemetryNumberStyle.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
        Text(text = String.format(Locale.US, "%+5.2f", avg), style = TelemetryNumberStyle.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
    }
}

/**
 * Downsamples recorded points if history is large to maintain crisp Canvas rendering.
 */
private fun downsampleReadings(
    readings: List<SensorReadingEntity>,
    sensorType: SensorType,
    maxPoints: Int = 100
): List<SensorDataPoint> {
    if (readings.isEmpty()) return emptyList()
    if (readings.size <= maxPoints) {
        return readings.map {
            SensorDataPoint(
                sensorType = sensorType,
                x = it.x,
                y = it.y,
                z = it.z,
                timestampNs = it.timestampNs,
                accuracy = SensorAccuracy.fromInt(it.accuracy),
                receivedAtMs = it.receivedAtMs
            )
        }
    }

    val step = readings.size.toDouble() / maxPoints
    val result = mutableListOf<SensorDataPoint>()
    for (i in 0 until maxPoints) {
        val idx = (i * step).toInt().coerceIn(0, readings.size - 1)
        val r = readings[idx]
        result.add(
            SensorDataPoint(
                sensorType = sensorType,
                x = r.x,
                y = r.y,
                z = r.z,
                timestampNs = r.timestampNs,
                accuracy = SensorAccuracy.fromInt(r.accuracy),
                receivedAtMs = r.receivedAtMs
            )
        )
    }
    return result
}
