package com.motionsense.app.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.presentation.theme.AxisXColor
import com.motionsense.app.presentation.theme.AxisYColor
import com.motionsense.app.presentation.theme.AxisZColor
import java.util.Locale
import kotlin.math.max

/**
 * Lightweight, hardware-accelerated time-series chart rendered using Jetpack Compose Canvas.
 * Visualizes continuous X, Y, and Z waveforms without external charting libraries.
 */
@Composable
fun TelemetryChart(
    history: List<SensorDataPoint>,
    unit: String,
    modifier: Modifier = Modifier,
    minYRange: Float = 12f // Sensible default min half-range to avoid jitter when flat
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val zeroLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chart Header Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time Waveform",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(label = "X", color = AxisXColor)
                LegendItem(label = "Y", color = AxisYColor)
                LegendItem(label = "Z", color = AxisZColor)
            }
        }

        // Canvas Waveform Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            if (history.isEmpty()) {
                Text(
                    text = "Awaiting telemetry stream...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw Horizontal Grid Lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Zero Reference Line
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                // Upper & Lower Guide Lines
                val quarterHeight = height / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY - quarterHeight),
                    end = Offset(width, midY - quarterHeight),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY + quarterHeight),
                    end = Offset(width, midY + quarterHeight),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )

                if (history.size < 2) return@Canvas

                // Compute dynamic vertical scale based on peak amplitude
                var maxAbs = minYRange
                for (pt in history) {
                    maxAbs = max(maxAbs, kotlin.math.abs(pt.x))
                    maxAbs = max(maxAbs, kotlin.math.abs(pt.y))
                    maxAbs = max(maxAbs, kotlin.math.abs(pt.z))
                }
                // Add 15% headroom
                val yRange = maxAbs * 1.15f
                val scaleY = (height / 2f) / yRange

                val stepX = width / (history.size - 1).coerceAtLeast(1)

                val pathX = Path()
                val pathY = Path()
                val pathZ = Path()

                history.forEachIndexed { index, point ->
                    val xPos = index * stepX
                    val yValX = (midY - (point.x * scaleY)).coerceIn(0f, height)
                    val yValY = (midY - (point.y * scaleY)).coerceIn(0f, height)
                    val yValZ = (midY - (point.z * scaleY)).coerceIn(0f, height)

                    if (index == 0) {
                        pathX.moveTo(xPos, yValX)
                        pathY.moveTo(xPos, yValY)
                        pathZ.moveTo(xPos, yValZ)
                    } else {
                        pathX.lineTo(xPos, yValX)
                        pathY.lineTo(xPos, yValY)
                        pathZ.lineTo(xPos, yValZ)
                    }
                }

                // Render continuous series waveforms
                drawPath(
                    path = pathX,
                    color = AxisXColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = pathY,
                    color = AxisYColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = pathZ,
                    color = AxisZColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Bottom Scale Reference
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-3.0s",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Rolling Buffer (${history.size} samples)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Live (0s)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
