package com.motionsense.app.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.motionsense.app.presentation.dashboard.DashboardScreen
import com.motionsense.app.presentation.history.SessionDetailsScreen
import com.motionsense.app.presentation.history.SessionHistoryScreen
import com.motionsense.app.viewmodel.SessionHistoryViewModel
import com.motionsense.app.viewmodel.TelemetryViewModel

enum class NavTab {
    DASHBOARD,
    HISTORY
}

@Composable
fun MotionSenseNavHost(
    telemetryViewModel: TelemetryViewModel,
    historyViewModel: SessionHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var viewingSessionId by remember { mutableStateOf<Long?>(null) }

    // SAF Document Creation for CSV Export
    var pendingExportSessionId by remember { mutableStateOf<Long?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingExportSessionId != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                    historyViewModel.exportSession(pendingExportSessionId!!, outputStream) {
                        Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        pendingExportSessionId = null
    }

    val triggerExport: (Long, String) -> Unit = { sessionId, suggestedFileName ->
        pendingExportSessionId = sessionId
        exportLauncher.launch(suggestedFileName)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (viewingSessionId == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavTab.DASHBOARD,
                        onClick = { currentTab = NavTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Live Telemetry"
                            )
                        },
                        label = { Text("Live Telemetry") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == NavTab.HISTORY,
                        onClick = { currentTab = NavTab.HISTORY },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Saved Recordings"
                            )
                        },
                        label = { Text("Recordings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (viewingSessionId != null) {
                SessionDetailsScreen(
                    sessionId = viewingSessionId!!,
                    viewModel = historyViewModel,
                    onBack = { viewingSessionId = null },
                    onExport = triggerExport
                )
            } else {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> DashboardScreen(
                            viewModel = telemetryViewModel
                        )
                        NavTab.HISTORY -> SessionHistoryScreen(
                            viewModel = historyViewModel,
                            onOpenSessionDetails = { id -> viewingSessionId = id },
                            onExportSession = triggerExport
                        )
                    }
                }
            }
        }
    }
}
