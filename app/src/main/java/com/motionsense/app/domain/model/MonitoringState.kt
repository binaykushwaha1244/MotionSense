package com.motionsense.app.domain.model

/**
 * High-level state of telemetry data acquisition.
 */
enum class MonitoringState {
    /** Telemetry is stopped; listeners are unregistered. */
    IDLE,

    /** Active real-time sensor polling and telemetry streaming. */
    MONITORING,

    /** Temporary suspension (e.g. Activity backgrounded) with intent to resume. */
    PAUSED
}
