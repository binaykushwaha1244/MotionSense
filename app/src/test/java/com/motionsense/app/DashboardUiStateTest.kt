package com.motionsense.app

import com.motionsense.app.domain.model.MonitoringState
import com.motionsense.app.presentation.dashboard.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateTest {

    @Test
    fun testFormattedDuration_underOneMinute() {
        val state = DashboardUiState(sessionDurationSeconds = 42L)
        assertEquals("00:42", state.formattedDuration)
    }

    @Test
    fun testFormattedDuration_multipleMinutes() {
        val state = DashboardUiState(sessionDurationSeconds = 125L)
        assertEquals("02:05", state.formattedDuration)
    }

    @Test
    fun testFormattedDuration_overOneHour() {
        val state = DashboardUiState(sessionDurationSeconds = 3665L)
        assertEquals("01:01:05", state.formattedDuration)
    }

    @Test
    fun testIsMonitoringState() {
        val idleState = DashboardUiState(monitoringState = MonitoringState.IDLE)
        assertFalse(idleState.isMonitoring)

        val activeState = DashboardUiState(monitoringState = MonitoringState.MONITORING)
        assertTrue(activeState.isMonitoring)

        val pausedState = DashboardUiState(monitoringState = MonitoringState.PAUSED)
        assertFalse(pausedState.isMonitoring)
    }
}
