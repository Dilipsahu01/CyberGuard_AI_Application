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

class CyberGuardInCallService : InCallService() {
    private val tag = "CyberGuardInCall"

    private var receiverRegistered = false

    companion object {
        var activeCall: Call? = null
        
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
        Log.d(tag, "onCallAdded intercepted. Transitioning state.")
        activeCall = call

        val handle = call.details.handle
        val rawNumber = handle?.schemeSpecificPart ?: "Unknown Caller"

        call.registerCallback(
            object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    super.onStateChanged(call, state)
                    Log.d(tag, "Call State Changed: $state")
                    
                    when (state) {
                        Call.STATE_ACTIVE -> {
                            Log.d(tag, "Call Answered. Booting voice recognition stream listener...")
                            startScamDetectionService(rawNumber)
                        }
                        Call.STATE_DISCONNECTED -> {
                            Log.d(tag, "Call ended. Terminating speech-monitors...")
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
        Log.d(tag, "onCallRemoved intercepted. Cleaning locks.")
        if (activeCall == call) {
            activeCall = null
        }
        stopScamDetectionService()
    }

    private fun startScamDetectionService(number: String) {
        val intent = Intent(this, ScamDetectionService::class.java).apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, number)
            putExtra("is_scam_scenario", false)
        }
        if (true) {
            startForegroundService(intent)
        } else {
            startService(intent)
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
        super.onDestroy()
    }
}
