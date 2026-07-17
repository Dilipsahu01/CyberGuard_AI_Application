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

enum class CallDirection { INCOMING, OUTGOING, MISSED }

class ActiveCallViewModel : ViewModel() {
    private val _callDirection = MutableStateFlow(CallDirection.OUTGOING)
    val callDirection: StateFlow<CallDirection> = _callDirection.asStateFlow()

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

    fun attachScamDetectionService(service: ScamDetectionService) {
        viewModelScope.launch {
            service.scoreFlow.collect { score ->
                _scamScore.value = score.toFloat()
                _scamStatus.value = when {
                    score >= 70 -> ScamStatus.SCAM
                    score >= 40 -> ScamStatus.SUSPICIOUS
                    else -> ScamStatus.SAFE
                }
            }
        }
    }

    fun toggleMute() {
        val newState = !_isMuted.value
        _isMuted.value = newState
        CyberGuardInCallService.instance?.setMuted(newState)
    }

    fun toggleHold() {
        val newState = !_isOnHold.value
        _isOnHold.value = newState
        CyberGuardInCallService.holdCall(newState)
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
}

enum class ScamStatus {
    SAFE, SUSPICIOUS, SCAM
}
