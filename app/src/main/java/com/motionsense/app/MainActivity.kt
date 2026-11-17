package com.motionsense.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.motionsense.app.data.repository.SensorRepository
import com.motionsense.app.data.repository.SensorRepositoryImpl
import com.motionsense.app.presentation.dashboard.DashboardScreen
import com.motionsense.app.presentation.theme.MotionSenseTheme
import com.motionsense.app.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TelemetryViewModel by viewModels {
        TelemetryViewModelFactory(SensorRepositoryImpl(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MotionSenseTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onLifecyclePause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onLifecycleResume()
    }
}

class TelemetryViewModelFactory(
    private val repository: SensorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelemetryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelemetryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
