package com.motionsense.app.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.data.repository.SensorRepository
import com.motionsense.app.domain.model.MonitoringState
import com.motionsense.app.presentation.dashboard.DashboardUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ViewModel managing telemetry state, sensor lifecycle, and rate-limiting updates
 * to ensure optimal Compose UI performance without rendering jank.
 */
class TelemetryViewModel(
    private val repository: SensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var accelerometerJob: Job? = null
    private var gyroscopeJob: Job? = null
    private var timerJob: Job? = null
    private var uiUpdateJob: Job? = null

    // Rolling buffers for incoming sensor events before being pushed to UI state
    private val accBuffer = ConcurrentLinkedQueue<SensorDataPoint>()
    private val gyroBuffer = ConcurrentLinkedQueue<SensorDataPoint>()

    @Volatile
    private var latestAcc: SensorDataPoint? = null
    @Volatile
    private var latestGyro: SensorDataPoint? = null

    private var sampleCounter = 0L
    private var samplesInCurrentWindow = 0L
    private var lastSampleRateCalcTime = System.currentTimeMillis()

    private var wasMonitoringBeforePause = false

    companion object {
        const val MAX_CHART_HISTORY_POINTS = 50
        const val UI_REFRESH_INTERVAL_MS = 33L // ~30 fps UI update decoupling
    }

    init {
        loadHardwareMetadata()
    }

    private fun loadHardwareMetadata() {
        val accMeta = repository.getSensorMetadata(SensorType.ACCELEROMETER)
        val gyroMeta = repository.getSensorMetadata(SensorType.GYROSCOPE)
        val deviceStr = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val osStr = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        _uiState.update {
            it.copy(
                accelerometerMetadata = accMeta,
                gyroscopeMetadata = gyroMeta,
                deviceModel = deviceStr,
                androidVersion = osStr
            )
        }
    }

    /**
     * Starts real-time monitoring and telemetry acquisition.
     */
    fun startMonitoring() {
        if (_uiState.value.monitoringState == MonitoringState.MONITORING) return

        stopAllSensorJobs()

        _uiState.update {
            it.copy(monitoringState = MonitoringState.MONITORING)
        }

        // Start Accelerometer Stream
        accelerometerJob = viewModelScope.launch {
            repository.getSensorStream(SensorType.ACCELEROMETER).collect { point ->
                latestAcc = point
                accBuffer.add(point)
                sampleCounter++
                samplesInCurrentWindow++
            }
        }

        // Start Gyroscope Stream
        gyroscopeJob = viewModelScope.launch {
            repository.getSensorStream(SensorType.GYROSCOPE).collect { point ->
                latestGyro = point
                gyroBuffer.add(point)
                sampleCounter++
                samplesInCurrentWindow++
            }
        }

        // Start UI decoupling ticker to update StateFlow at controlled 30 Hz
        uiUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(UI_REFRESH_INTERVAL_MS)
                flushBuffersToUi()
            }
        }

        // Start 1-second Session Timer and Sampling Rate calculator
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val deltaSec = (now - lastSampleRateCalcTime).coerceAtLeast(1L) / 1000f
                val measuredRate = samplesInCurrentWindow / deltaSec
                samplesInCurrentWindow = 0L
                lastSampleRateCalcTime = now

                _uiState.update { current ->
                    current.copy(
                        sessionDurationSeconds = current.sessionDurationSeconds + 1,
                        currentSampleRateHz = measuredRate
                    )
                }
            }
        }
    }

    /**
     * Stops active telemetry data collection and unregisters all hardware listeners.
     */
    fun stopMonitoring() {
        stopAllSensorJobs()
        _uiState.update {
            it.copy(
                monitoringState = MonitoringState.IDLE,
                currentSampleRateHz = 0f
            )
        }
        wasMonitoringBeforePause = false
    }

    /**
     * Clears rolling chart histories and resets counters.
     */
    fun resetSession() {
        val wasActive = _uiState.value.isMonitoring
        if (wasActive) {
            stopMonitoring()
        }
        accBuffer.clear()
        gyroBuffer.clear()
        latestAcc = null
        latestGyro = null
        sampleCounter = 0L
        samplesInCurrentWindow = 0L

        _uiState.update {
            it.copy(
                sessionDurationSeconds = 0L,
                totalSamplesProcessed = 0L,
                currentSampleRateHz = 0f,
                currentAccelerometerReading = null,
                currentGyroscopeReading = null,
                accelerometerHistory = emptyList(),
                gyroscopeHistory = emptyList()
            )
        }

        if (wasActive) {
            startMonitoring()
        }
    }

    /**
     * Called when the hosting Activity pauses/stops. Unregisters listeners to prevent leaks.
     */
    fun onLifecyclePause() {
        if (_uiState.value.monitoringState == MonitoringState.MONITORING) {
            wasMonitoringBeforePause = true
            stopAllSensorJobs()
            _uiState.update {
                it.copy(
                    monitoringState = MonitoringState.PAUSED,
                    currentSampleRateHz = 0f
                )
            }
        }
    }

    /**
     * Called when the hosting Activity resumes. Restores monitoring if previously active.
     */
    fun onLifecycleResume() {
        if (wasMonitoringBeforePause) {
            wasMonitoringBeforePause = false
            startMonitoring()
        }
    }

    private fun flushBuffersToUi() {
        val currentAccHistory = _uiState.value.accelerometerHistory.toMutableList()
        while (!accBuffer.isEmpty()) {
            accBuffer.poll()?.let { currentAccHistory.add(it) }
        }
        // Trim history to bounded window
        while (currentAccHistory.size > MAX_CHART_HISTORY_POINTS) {
            currentAccHistory.removeAt(0)
        }

        val currentGyroHistory = _uiState.value.gyroscopeHistory.toMutableList()
        while (!gyroBuffer.isEmpty()) {
            gyroBuffer.poll()?.let { currentGyroHistory.add(it) }
        }
        // Trim history to bounded window
        while (currentGyroHistory.size > MAX_CHART_HISTORY_POINTS) {
            currentGyroHistory.removeAt(0)
        }

        _uiState.update { current ->
            current.copy(
                totalSamplesProcessed = sampleCounter,
                currentAccelerometerReading = latestAcc,
                currentGyroscopeReading = latestGyro,
                accelerometerHistory = currentAccHistory,
                gyroscopeHistory = currentGyroHistory
            )
        }
    }

    private fun stopAllSensorJobs() {
        accelerometerJob?.cancel()
        gyroscopeJob?.cancel()
        timerJob?.cancel()
        uiUpdateJob?.cancel()

        accelerometerJob = null
        gyroscopeJob = null
        timerJob = null
        uiUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllSensorJobs()
        accBuffer.clear()
        gyroBuffer.clear()
    }
}
