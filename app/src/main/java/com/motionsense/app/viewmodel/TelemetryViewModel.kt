package com.motionsense.app.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionsense.app.data.model.SensorDataPoint
import com.motionsense.app.data.model.SensorType
import com.motionsense.app.data.repository.RecordingRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ViewModel managing live telemetry data streams, UI rate-limiting decoupling,
 * and persistent local recording sessions in Room.
 */
class TelemetryViewModel(
    private val sensorRepository: SensorRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var accelerometerJob: Job? = null
    private var gyroscopeJob: Job? = null
    private var monitoringTimerJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var uiUpdateJob: Job? = null
    private var dbFlushJob: Job? = null

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
    private var recordedCount = 0L

    private var wasMonitoringBeforePause = false

    companion object {
        const val MAX_CHART_HISTORY_POINTS = 50
        const val UI_REFRESH_INTERVAL_MS = 33L // ~30 fps UI update decoupling
        const val DB_FLUSH_INTERVAL_MS = 500L  // Flush write buffer to SQLite every 500ms
    }

    init {
        loadHardwareMetadata()
    }

    private fun loadHardwareMetadata() {
        val accMeta = sensorRepository.getSensorMetadata(SensorType.ACCELEROMETER)
        val gyroMeta = sensorRepository.getSensorMetadata(SensorType.GYROSCOPE)
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

        stopAllMonitoringJobs()

        _uiState.update {
            it.copy(monitoringState = MonitoringState.MONITORING)
        }

        // Start Accelerometer Stream
        accelerometerJob = viewModelScope.launch {
            sensorRepository.getSensorStream(SensorType.ACCELEROMETER).collect { point ->
                latestAcc = point
                accBuffer.add(point)
                sampleCounter++
                samplesInCurrentWindow++

                // Forward to database if actively recording
                val activeSessionId = _uiState.value.activeRecordingSessionId
                if (_uiState.value.isRecording && activeSessionId != null) {
                    recordingRepository.enqueueReading(activeSessionId, point)
                    recordedCount++
                }
            }
        }

        // Start Gyroscope Stream
        gyroscopeJob = viewModelScope.launch {
            sensorRepository.getSensorStream(SensorType.GYROSCOPE).collect { point ->
                latestGyro = point
                gyroBuffer.add(point)
                sampleCounter++
                samplesInCurrentWindow++

                // Forward to database if actively recording
                val activeSessionId = _uiState.value.activeRecordingSessionId
                if (_uiState.value.isRecording && activeSessionId != null) {
                    recordingRepository.enqueueReading(activeSessionId, point)
                    recordedCount++
                }
            }
        }

        // Start UI decoupling ticker to update StateFlow at steady 30 Hz
        uiUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(UI_REFRESH_INTERVAL_MS)
                flushBuffersToUi()
            }
        }

        // Start 1-second Session Timer and Sampling Rate calculator
        monitoringTimerJob = viewModelScope.launch {
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
     * Stops active telemetry data collection. Automatically finalizes active recording.
     */
    fun stopMonitoring() {
        // If actively recording, finalize recording first
        if (_uiState.value.isRecording) {
            stopRecording()
        }

        stopAllMonitoringJobs()
        _uiState.update {
            it.copy(
                monitoringState = MonitoringState.IDLE,
                currentSampleRateHz = 0f
            )
        }
        wasMonitoringBeforePause = false
    }

    /**
     * Clears rolling chart histories and resets live monitoring counters.
     */
    fun resetLiveTelemetry() {
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
    }

    // --- Recording Session Controls (Milestone 3A) ---

    fun openStartRecordingDialog() {
        _uiState.update { it.copy(showStartRecordingDialog = true) }
    }

    fun dismissStartRecordingDialog() {
        _uiState.update { it.copy(showStartRecordingDialog = false) }
    }

    /**
     * Creates and begins persistent local recording into Room database.
     */
    fun startRecording(customName: String? = null) {
        dismissStartRecordingDialog()
        if (_uiState.value.isRecording) return

        // Automatically start monitoring if not already streaming
        if (!_uiState.value.isMonitoring) {
            startMonitoring()
        }

        val sessionName = if (!customName.isNullOrBlank()) {
            customName.trim()
        } else {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            "Session $dateStr"
        }

        viewModelScope.launch {
            val sessionId = recordingRepository.startRecording(
                sessionName = sessionName,
                deviceModel = _uiState.value.deviceModel
            )

            recordedCount = 0L

            _uiState.update {
                it.copy(
                    isRecording = true,
                    activeRecordingSessionId = sessionId,
                    activeRecordingSessionName = sessionName,
                    recordingDurationSeconds = 0L,
                    recordedSamplesCount = 0L
                )
            }

            // Periodic buffer flush to SQLite
            dbFlushJob?.cancel()
            dbFlushJob = launch {
                while (isActive) {
                    delay(DB_FLUSH_INTERVAL_MS)
                    recordingRepository.flushPendingReadings()
                }
            }

            // 1-second recording duration timer
            recordingTimerJob?.cancel()
            recordingTimerJob = launch {
                while (isActive) {
                    delay(1000L)
                    _uiState.update {
                        it.copy(
                            recordingDurationSeconds = it.recordingDurationSeconds + 1,
                            recordedSamplesCount = recordedCount
                        )
                    }
                }
            }
        }
    }

    /**
     * Concludes active recording session, flushes remaining write buffer, and updates Room record.
     */
    fun stopRecording() {
        val sessionId = _uiState.value.activeRecordingSessionId ?: return

        recordingTimerJob?.cancel()
        recordingTimerJob = null
        dbFlushJob?.cancel()
        dbFlushJob = null

        viewModelScope.launch {
            recordingRepository.stopRecording(sessionId)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    activeRecordingSessionId = null,
                    activeRecordingSessionName = "",
                    recordingDurationSeconds = 0L,
                    recordedSamplesCount = 0L
                )
            }
        }
    }

    fun onLifecyclePause() {
        if (_uiState.value.monitoringState == MonitoringState.MONITORING) {
            wasMonitoringBeforePause = true
            // If recording, flush buffer to avoid losing data
            if (_uiState.value.isRecording) {
                viewModelScope.launch {
                    recordingRepository.flushPendingReadings()
                }
            }
            stopAllMonitoringJobs()
            _uiState.update {
                it.copy(
                    monitoringState = MonitoringState.PAUSED,
                    currentSampleRateHz = 0f
                )
            }
        }
    }

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
        while (currentAccHistory.size > MAX_CHART_HISTORY_POINTS) {
            currentAccHistory.removeAt(0)
        }

        val currentGyroHistory = _uiState.value.gyroscopeHistory.toMutableList()
        while (!gyroBuffer.isEmpty()) {
            gyroBuffer.poll()?.let { currentGyroHistory.add(it) }
        }
        while (currentGyroHistory.size > MAX_CHART_HISTORY_POINTS) {
            currentGyroHistory.removeAt(0)
        }

        _uiState.update { current ->
            current.copy(
                totalSamplesProcessed = sampleCounter,
                currentAccelerometerReading = latestAcc,
                currentGyroscopeReading = latestGyro,
                accelerometerHistory = currentAccHistory,
                gyroscopeHistory = currentGyroHistory,
                recordedSamplesCount = if (current.isRecording) recordedCount else current.recordedSamplesCount
            )
        }
    }

    private fun stopAllMonitoringJobs() {
        accelerometerJob?.cancel()
        gyroscopeJob?.cancel()
        monitoringTimerJob?.cancel()
        uiUpdateJob?.cancel()

        accelerometerJob = null
        gyroscopeJob = null
        monitoringTimerJob = null
        uiUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isRecording) {
            _uiState.value.activeRecordingSessionId?.let { id ->
                kotlinx.coroutines.runBlocking {
                    recordingRepository.stopRecording(id)
                }
            }
        }
        stopAllMonitoringJobs()
        recordingTimerJob?.cancel()
        dbFlushJob?.cancel()
        accBuffer.clear()
        gyroBuffer.clear()
    }
}
