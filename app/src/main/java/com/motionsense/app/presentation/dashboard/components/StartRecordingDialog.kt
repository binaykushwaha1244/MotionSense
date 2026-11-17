package com.motionsense.app.presentation.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionsense.app.presentation.theme.StatusErrorColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartRecordingDialog(
    onDismiss: () -> Unit,
    onConfirm: (sessionName: String) -> Unit
) {
    val defaultName = remember {
        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date())
        "Session $dateStr"
    }
    var sessionName by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Recording Session",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "High-frequency motion telemetry will be buffered and persisted into the local Room database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(sessionName) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusErrorColor
                )
            ) {
                Text("Start Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
