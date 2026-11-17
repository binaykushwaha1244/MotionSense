package com.motionsense.app.presentation.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorMetadata
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.presentation.theme.AxisXColor
import com.motionsense.app.presentation.theme.AxisYColor
import com.motionsense.app.presentation.theme.AxisZColor
import com.motionsense.app.presentation.theme.StatusActiveColor
import com.motionsense.app.presentation.theme.StatusErrorColor
import com.motionsense.app.presentation.theme.StatusIdleColor
import com.motionsense.app.presentation.theme.TelemetryNumberStyle
import java.util.Locale

/**
 * Sensor card containing real-time 3-axis telemetry readings, hardware metadata,
 * vector magnitude, and live waveform visualization.
 */
@Composable
fun SensorCard(
    sensorType: SensorType,
    metadata: SensorMetadata?,
    reading: SensorDataPoint?,
    history: List<SensorDataPoint>,
    isMonitoring: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    val isAvailable = metadata?.isAvailable == true

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon, Title, Status Badges, Info toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = sensorType.displayName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = sensorType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAvailable && metadata != null) "${metadata.vendor} • ${metadata.name}" else "Sensor Not Detected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Availability Badge
                    val statusColor = when {
                        !isAvailable -> StatusErrorColor
                        isMonitoring -> StatusActiveColor
                        else -> StatusIdleColor
                    }
                    val statusText = when {
                        !isAvailable -> "Unavailable"
                        isMonitoring -> "Streaming"
                        else -> "Standby"
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = statusColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(color = statusColor, shape = CircleShape)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Sensor Specifications",
                            tint = if (showDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable Hardware Specifications
            AnimatedVisibility(visible = showDetails && metadata != null) {
                metadata?.let { meta ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetadataRow(label = "Hardware Driver", value = meta.name)
                        MetadataRow(label = "Vendor", value = meta.vendor)
                        MetadataRow(label = "Max Range", value = String.format(Locale.US, "%.2f %s", meta.maxRange, sensorType.defaultUnit))
                        MetadataRow(label = "Resolution", value = String.format(Locale.US, "%.6f %s", meta.resolution, sensorType.defaultUnit))
                        MetadataRow(label = "Power Consumption", value = String.format(Locale.US, "%.3f mA", meta.powerMa))
                        MetadataRow(label = "Sampling Delay", value = "${meta.minDelayUs} µs - ${meta.maxDelayUs} µs")
                        MetadataRow(label = "Reporting Mode", value = meta.reportingMode)
                    }
                }
            }

            // 3-Axis Metric Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AxisMetricChip(
                    axisLabel = "X",
                    value = reading?.x,
                    unit = sensorType.defaultUnit,
                    axisColor = AxisXColor,
                    modifier = Modifier.weight(1f)
                )
                AxisMetricChip(
                    axisLabel = "Y",
                    value = reading?.y,
                    unit = sensorType.defaultUnit,
                    axisColor = AxisYColor,
                    modifier = Modifier.weight(1f)
                )
                AxisMetricChip(
                    axisLabel = "Z",
                    value = reading?.z,
                    unit = sensorType.defaultUnit,
                    axisColor = AxisZColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Vector Magnitude & Accuracy Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Vector Magnitude:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val magStr = reading?.let {
                        String.format(Locale.US, "%.2f %s", it.magnitude, sensorType.defaultUnit)
                    } ?: "--.-- ${sensorType.defaultUnit}"
                    Text(
                        text = magStr,
                        style = TelemetryNumberStyle.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                reading?.let { pt ->
                    Text(
                        text = "Accuracy: ${pt.accuracy.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Time-Series Chart
            TelemetryChart(
                history = history,
                unit = sensorType.defaultUnit,
                minYRange = if (sensorType == SensorType.ACCELEROMETER) 12f else 3f
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
