package com.example.services

/**
 * CyberGuardInCallService.kt
 *
 * PURPOSE:
 * Integrates directly with the Android OS Telecom framework (`InCallService`).
 *
 * WHY IT EXISTS:
 * This is the only way Android allows an app to physically intercept and block a live
 * active phone call without user input. When the AI score hits 70%, this service
 * executes the autonomous call termination.
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CallUiState { RINGING, ACTIVE, HELD, DIALING }

class CyberGuardInCallService : InCallService() {
    private val tag = "CyberGuardInCall"

    private var receiverRegistered = false

    companion object {
        var activeCall: Call? = null
        var instance: CyberGuardInCallService? = null
        
        data class CallSession(
            val call: Call,
            val direction: Int,
            var hasReachedActive: Boolean = false,
            val startTime: Long = System.currentTimeMillis(),
            var state: CallUiState = CallUiState.RINGING,
            val rawNumber: String,
            var userFeedback: String? = null,
            var callback: Call.Callback? = null
        )
        val callSessions = java.util.concurrent.ConcurrentHashMap<Call, CallSession>()

        private val _activeCallsFlow = MutableStateFlow<List<CallSession>>(emptyList())
        val activeCallsFlow = _activeCallsFlow.asStateFlow()

        private fun updateActiveCallsFlow() {
            _activeCallsFlow.value = callSessions.values.toList()
        }

        fun setUserFeedback(feedback: String, call: Call? = activeCall) {
            call?.let {
                callSessions[it]?.userFeedback = feedback
                updateActiveCallsFlow()
            }
        }

        fun disconnectCall(call: Call? = activeCall) {
            call?.let {
                Log.d("CyberGuardInCall", "Requested Hangup. Disconnecting specific telecom line...")
                it.disconnect()
            }
        }

        fun answerCall(call: Call? = activeCall) {
            call?.let {
                Log.d("CyberGuardInCall", "Answering specific telecom call line...")
                it.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            }
        }

        fun holdCall(hold: Boolean, call: Call? = activeCall) {
            call?.let {
                if (hold) it.hold() else it.unhold()
            }
        }

        @Suppress("DEPRECATION")
        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }
    }

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_DISCONNECT_CALL) {
                disconnectCall()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ContextCompat.registerReceiver(
            this,
            disconnectReceiver,
            IntentFilter(Constants.ACTION_DISCONNECT_CALL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.e("TELECOM_DEBUG", "onCallAdded intercepted. Transitioning state.")
        activeCall = call

        val handle = call.details.handle
        val rawNumber = android.net.Uri.decode(handle?.schemeSpecificPart) ?: "Unknown Caller"

        val activeCallDirection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            call.details.callDirection
        } else {
            if (call.details.state == Call.STATE_DIALING || call.details.state == Call.STATE_CONNECTING) {
                android.telecom.Call.Details.DIRECTION_OUTGOING
            } else {
                android.telecom.Call.Details.DIRECTION_INCOMING
            }
        }
        val initialState = if (activeCallDirection == android.telecom.Call.Details.DIRECTION_OUTGOING) CallUiState.DIALING else CallUiState.RINGING
        callSessions[call] = CallSession(
            call = call,
            direction = activeCallDirection,
            state = initialState,
            rawNumber = rawNumber
        )
        updateActiveCallsFlow()

        // High-Speed Read Optimization: Instant threat check on a background coroutine
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.models.ScamDatabase.getDatabase(applicationContext)
                val dotScammer = db.dotScammerDao().getScammer(com.example.utils.PhoneNumberUtils.normalize(rawNumber))
                if (dotScammer != null) {
                    Log.e("TELECOM_DEBUG", "IMMEDIATE SCAM MATCH: Number $rawNumber found in offline Room DB! Category: ${dotScammer.threatCategory}")
                    com.example.utils.CallStateBroadcaster.updateTelemetry(com.example.utils.TelemetryUpdate(100, "KNOWN SCAMMER (DoT Blacklist Match)", "DoT_Blacklist", rawNumber, "BLOCKLIST", 100, 100, 100, 0, 0, true))
                } else {
                    val pipeline = com.example.pipeline.PipelineSingleton.getInstance(applicationContext)
                    val isInBloomFilter = pipeline.isScamCallerNumber(rawNumber)
                    if (isInBloomFilter) {
                        Log.e("TELECOM_DEBUG", "IMMEDIATE SCAM MATCH: Number $rawNumber flagged by in-memory Bloom Filter!")
                        com.example.utils.CallStateBroadcaster.updateTelemetry(com.example.utils.TelemetryUpdate(100, "KNOWN SCAMMER (Bloom Filter Match)", "Bloom_Filter", rawNumber, "BLOCKLIST", 100, 100, 100, 0, 0, true))
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed high-speed read optimization: ${e.message}")
            }
        }

        val callCallback = object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    super.onStateChanged(call, state)
                    Log.d(tag, "Call State Changed: $state")

                    when (state) {
                        Call.STATE_ACTIVE -> {
                            callSessions[call]?.let { session ->
                                session.hasReachedActive = true
                                session.state = CallUiState.ACTIVE
                            }
                            updateActiveCallsFlow()
                            Log.e("TELECOM_DEBUG", "Call Answered. Booting voice recognition stream listener...")
                            startScamDetectionService(rawNumber, callSessions[call]?.direction ?: -1)
                        }
                        Call.STATE_HOLDING -> {
                            callSessions[call]?.state = CallUiState.HELD
                            updateActiveCallsFlow()
                        }
                        Call.STATE_DISCONNECTED -> {
                            Log.e("TELECOM_DEBUG", "Call ended. Terminating speech-monitors and popping UI backstack...")
                            logCallToDatabase(rawNumber, call)
                            callSessions[call]?.callback?.let { call.unregisterCallback(it) }
                            callSessions.remove(call)
                            updateActiveCallsFlow()
                            
                            if (callSessions.isEmpty()) {
                                com.example.utils.CallStateBroadcaster.endCall()
                                stopScamDetectionService()
                            }
                        }
                    }
                }
            }
            
        callSessions[call]?.callback = callCallback
        call.registerCallback(callCallback)

        // Instantly display our high-performance call screen overlay activity
        val context = applicationContext
        val intent = Intent(context, com.example.IncomingCallActivity::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, rawNumber)
            putExtra("call_direction", activeCallDirection)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.e("TELECOM_DEBUG", "onCallRemoved intercepted. Cleaning locks.")
        if (activeCall == call) {
            activeCall = null
        }
        callSessions[call]?.callback?.let { call.unregisterCallback(it) }
        callSessions.remove(call)
        updateActiveCallsFlow()
        
        if (callSessions.isEmpty()) {
            com.example.utils.CallStateBroadcaster.endCall()
            stopScamDetectionService()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Telecom API 34")
    override fun onCallAudioStateChanged(audioState: android.telecom.CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        Log.e("TELECOM_DEBUG", "onCallAudioStateChanged: $audioState")
    }

    private fun startScamDetectionService(number: String, direction: Int) {
        val intent = Intent(this, ScamDetectionService::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, number)
            putExtra("is_scam_scenario", false)
            putExtra("call_direction", direction)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start ScamDetectionService: ${e.message}")
        }
    }

    private fun stopScamDetectionService() {
        stopService(Intent(this, ScamDetectionService::class.java))
    }

    private fun logCallToDatabase(rawNumber: String, call: Call) {
        val session = callSessions[call] ?: return
        val duration = if (session.hasReachedActive) ((System.currentTimeMillis() - session.startTime) / 1000).toInt() else 0
        val finalDirection = if (session.hasReachedActive) session.direction else 2 // 2 = MISSED

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.models.ScamDatabase.getDatabase(applicationContext)
                val mgr = com.example.pipeline.PipelineSingleton.getInstance(applicationContext)
                val finalResult = mgr.getLatestResult()
                val score = finalResult?.score ?: 0
                val settings = getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
                val threshold = settings.getInt("alert_threshold", 70)
                val isScam = score >= threshold
                val scrubbedTranscript = com.example.utils.TranscriptScrubber.scrub(finalResult?.transcript ?: "")

                val log = com.example.models.CallLog(
                    callerNumber = rawNumber,
                    timestamp = session.startTime,
                    riskScore = score,
                    isScam = isScam,
                    transcript = scrubbedTranscript,
                    hitKeywords = finalResult?.hitWord ?: "",
                    durationSeconds = duration,
                    wasBlocked = isScam,
                    direction = finalDirection,
                    userFeedback = session.userFeedback
                )
                db.callLogDao().insertLog(log)
            } catch (e: Exception) {
                Log.e(tag, "Failed to log call to database: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(disconnectReceiver)
            } catch (e: Exception) {
                Log.e(tag, "Error unregistering disconnect receiver: ${e.message}")
            }
            receiverRegistered = false
        }
        activeCall = null
        instance = null
        super.onDestroy()
    }
}
