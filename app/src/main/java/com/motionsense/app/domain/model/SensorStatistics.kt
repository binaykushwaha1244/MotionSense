package com.motionsense.app.domain.model

data class SensorStatistics(
    val minX: Float = 0f,
    val maxX: Float = 0f,
    val avgX: Float = 0f,
    val minY: Float = 0f,
    val maxY: Float = 0f,
    val avgY: Float = 0f,
    val minZ: Float = 0f,
    val maxZ: Float = 0f,
    val avgZ: Float = 0f,
    val peakMagnitude: Float = 0f,
    val avgMagnitude: Float = 0f,
    val sampleCount: Int = 0
)
