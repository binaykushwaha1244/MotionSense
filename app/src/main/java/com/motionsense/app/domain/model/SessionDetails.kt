package com.motionsense.app.domain.model

import com.motionsense.app.data.local.entity.SensorReadingEntity
import com.motionsense.app.data.local.entity.SensorSessionEntity

data class SessionDetails(
    val session: SensorSessionEntity,
    val accelerometerReadings: List<SensorReadingEntity> = emptyList(),
    val gyroscopeReadings: List<SensorReadingEntity> = emptyList(),
    val accelerometerStats: SensorStatistics = SensorStatistics(),
    val gyroscopeStats: SensorStatistics = SensorStatistics()
)
