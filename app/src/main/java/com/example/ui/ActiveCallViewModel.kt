package com.example.ui

import android.telecom.CallAudioState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.CyberGuardInCallService
import com.example.services.ScamDetectionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallDirection { INCOMING, OUTGOING, MISSED, UNKNOWN }

class ActiveCallViewModel : ViewModel() {
    private val _callDirection = MutableStateFlow<CallDirection?>(null)
    val callDirection: StateFlow<CallDirection?> = _callDirection.asStateFlow()

    fun setCallDirection(direction: CallDirection) {
        _callDirection.value = direction
    }

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _scamStatus = MutableStateFlow(ScamStatus.SAFE)
    val scamStatus: StateFlow<ScamStatus> = _scamStatus.asStateFlow()

    private val _scamScore = MutableStateFlow(0f)
    val scamScore: StateFlow<Float> = _scamScore.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(true)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    fun dismissOverlayAndSetFeedback(isScam: Boolean) {
        val feedback = if (isScam) "CONFIRMED_SCAM" else "FALSE_POSITIVE"
        CyberGuardInCallService.setUserFeedback(feedback)
        _isOverlayVisible.value = false
    }

    fun attachScamDetectionService(service: ScamDetectionService) {
        viewModelScope.launch {
            service.scoreFlow.collect { score ->
                _scamScore.value = score.toFloat()
                
                // Only change status if overlay wasn't explicitly dismissed
                if (_isOverlayVisible.value) {
                    _scamStatus.value = when {
                        score >= 70 -> ScamStatus.SCAM
                        score >= 40 -> ScamStatus.SUSPICIOUS
                        else -> ScamStatus.SAFE
                    }
                }
            }
        }
    }

    fun toggleMute() {
        val newState = !_isMuted.value
        _isMuted.value = newState
        CyberGuardInCallService.instance?.setMuted(newState)
    }

    fun setIsOnHold(held: Boolean) {
        _isOnHold.value = held
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        val route = if (newState) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        CyberGuardInCallService.setAudioRoute(route)
    }

    fun disconnect() {
        CyberGuardInCallService.disconnectCall()
    }

    private val _callElapsedSeconds = MutableStateFlow(0)
    val callElapsedSeconds: StateFlow<Int> = _callElapsedSeconds.asStateFlow()

    private val _callElapsedFormatted = MutableStateFlow("00:00")
    val callElapsedFormatted: StateFlow<String> = _callElapsedFormatted.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _callElapsedSeconds.value += 1
                val mins = _callElapsedSeconds.value / 60
                val secs = _callElapsedSeconds.value % 60
                _callElapsedFormatted.value = String.format("%02d:%02d", mins, secs)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private val _isBluetoothOn = MutableStateFlow(false)
    val isBluetoothOn: StateFlow<Boolean> = _isBluetoothOn.asStateFlow()

    fun toggleBluetooth() {
        val newState = !_isBluetoothOn.value
        _isBluetoothOn.value = newState
        val route = if (newState) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE
        CyberGuardInCallService.setAudioRoute(route)
    }
}

enum class ScamStatus {
    SAFE, SUSPICIOUS, SCAM
}
