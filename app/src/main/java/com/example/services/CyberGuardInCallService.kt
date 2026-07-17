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

class CyberGuardInCallService : InCallService() {
    private val tag = "CyberGuardInCall"

    private var receiverRegistered = false

    companion object {
        var activeCall: Call? = null
        var instance: CyberGuardInCallService? = null

        fun disconnectCall() {
            activeCall?.let {
                Log.d("CyberGuardInCall", "Requested Hangup. Disconnecting active telecom line...")
                it.disconnect()
            }
        }

        fun answerCall() {
            activeCall?.let {
                Log.d("CyberGuardInCall", "Answering active telecom call line...")
                it.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            }
        }

        fun holdCall(hold: Boolean) {
            activeCall?.let {
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

        // High-Speed Read Optimization: Instant threat check on a background coroutine
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.models.ScamDatabase.getDatabase(applicationContext)
                val dotScammer = db.dotScammerDao().getScammer(com.example.utils.PhoneNumberUtils.normalize(rawNumber))
                if (dotScammer != null) {
                    Log.e("TELECOM_DEBUG", "IMMEDIATE SCAM MATCH: Number $rawNumber found in offline Room DB! Category: ${dotScammer.threatCategory}")
                } else {
                    val pipeline = com.example.pipeline.PipelineSingleton.getInstance(applicationContext)
                    val isInBloomFilter = pipeline.isScamCallerNumber(rawNumber)
                    if (isInBloomFilter) {
                        Log.e("TELECOM_DEBUG", "IMMEDIATE SCAM MATCH: Number $rawNumber flagged by in-memory Bloom Filter!")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed high-speed read optimization: ${e.message}")
            }
        }

        call.registerCallback(
            object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    super.onStateChanged(call, state)
                    Log.d(tag, "Call State Changed: $state")

                    when (state) {
                        Call.STATE_ACTIVE -> {
                            Log.e("TELECOM_DEBUG", "Call Answered. Booting voice recognition stream listener...")
                            startScamDetectionService(rawNumber)
                        }
                        Call.STATE_DISCONNECTED -> {
                            Log.e("TELECOM_DEBUG", "Call ended. Terminating speech-monitors and popping UI backstack...")
                            com.example.utils.CallStateBroadcaster.endCall()
                            stopScamDetectionService()
                        }
                    }
                }
            }
        )

        // Instantly display our high-performance call screen overlay activity
        val context = applicationContext
        val intent = Intent(context, com.example.IncomingCallActivity::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, rawNumber)
            putExtra("is_incoming", true)
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
        com.example.utils.CallStateBroadcaster.endCall()
        stopScamDetectionService()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Telecom API 34")
    override fun onCallAudioStateChanged(audioState: android.telecom.CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        Log.e("TELECOM_DEBUG", "onCallAudioStateChanged: $audioState")
    }

    private fun startScamDetectionService(number: String) {
        val intent = Intent(this, ScamDetectionService::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, number)
            putExtra("is_scam_scenario", false)
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
