package com.example.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

import kotlinx.coroutines.channels.BufferOverflow

data class TelemetryUpdate(
    val score: Int,
    val transcript: String,
    val hitWord: String,
    val callerNumber: String,
    val stage: String,
    val urgency: Int,
    val financial: Int,
    val coercion: Int,
    val intimacy: Int,
    val trust: Int,
    val isScamScenario: Boolean
)

object CallStateBroadcaster {
    private val _telemetryFlow = MutableSharedFlow<TelemetryUpdate>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val telemetryFlow: SharedFlow<TelemetryUpdate> = _telemetryFlow.asSharedFlow()

    private val _callEndedFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val callEndedFlow: SharedFlow<Unit> = _callEndedFlow.asSharedFlow()

    fun updateTelemetry(update: TelemetryUpdate) {
        _telemetryFlow.tryEmit(update)
    }

    fun endCall() {
        _callEndedFlow.tryEmit(Unit)
    }
}
