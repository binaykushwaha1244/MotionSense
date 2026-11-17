package com.motionsense.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionsense.app.data.local.entity.SensorSessionEntity
import com.motionsense.app.data.repository.RecordingRepository
import com.motionsense.app.domain.model.SessionDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

class SessionHistoryViewModel(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val sessions: StateFlow<List<SensorSessionEntity>> = recordingRepository
        .getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSessionDetails = MutableStateFlow<SessionDetails?>(null)
    val selectedSessionDetails: StateFlow<SessionDetails?> = _selectedSessionDetails.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private val _pendingDeleteSession = MutableStateFlow<SensorSessionEntity?>(null)
    val pendingDeleteSession: StateFlow<SensorSessionEntity?> = _pendingDeleteSession.asStateFlow()

    fun loadSessionDetails(sessionId: Long) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            val details = recordingRepository.getSessionDetails(sessionId)
            _selectedSessionDetails.value = details
            _isLoadingDetails.value = false
        }
    }

    fun clearSessionDetails() {
        _selectedSessionDetails.value = null
    }

    fun promptDeleteSession(session: SensorSessionEntity) {
        _pendingDeleteSession.value = session
    }

    fun dismissDeletePrompt() {
        _pendingDeleteSession.value = null
    }

    fun confirmDeleteSession() {
        val session = _pendingDeleteSession.value ?: return
        viewModelScope.launch {
            recordingRepository.deleteSession(session.sessionId)
            _pendingDeleteSession.value = null
            if (_selectedSessionDetails.value?.session?.sessionId == session.sessionId) {
                _selectedSessionDetails.value = null
            }
        }
    }

    fun exportSession(sessionId: Long, outputStream: OutputStream, onComplete: () -> Unit) {
        viewModelScope.launch {
            recordingRepository.exportSessionToCsv(sessionId, outputStream)
            onComplete()
        }
    }
}
