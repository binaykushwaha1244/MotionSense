package com.motionsense.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.motionsense.app.data.local.MotionSenseDatabase
import com.motionsense.app.data.repository.RecordingRepository
import com.motionsense.app.data.repository.RecordingRepositoryImpl
import com.motionsense.app.data.repository.SensorRepository
import com.motionsense.app.data.repository.SensorRepositoryImpl
import com.motionsense.app.presentation.navigation.MotionSenseNavHost
import com.motionsense.app.presentation.theme.MotionSenseTheme
import com.motionsense.app.viewmodel.SessionHistoryViewModel
import com.motionsense.app.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy { MotionSenseDatabase.getInstance(this) }
    private val sensorRepository: SensorRepository by lazy { SensorRepositoryImpl(this) }
    private val recordingRepository: RecordingRepository by lazy { RecordingRepositoryImpl(database) }

    private val telemetryViewModel: TelemetryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TelemetryViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TelemetryViewModel(sensorRepository, recordingRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }

    private val historyViewModel: SessionHistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SessionHistoryViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SessionHistoryViewModel(recordingRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MotionSenseTheme {
                MotionSenseNavHost(
                    telemetryViewModel = telemetryViewModel,
                    historyViewModel = historyViewModel
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        telemetryViewModel.onLifecyclePause()
    }

    override fun onResume() {
        super.onResume()
        telemetryViewModel.onLifecycleResume()
    }
}
