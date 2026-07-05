package com.example

/**
 * IncomingCallActivity.kt
 * 
 * PURPOSE: 
 * The visual warning overlay that appears *during* a live phone call.
 * 
 * WHY IT EXISTS:
 * Listens to broadcasts from `ScamDetectionService`. If the AI score crosses 40%, 
 * this Activity pushes a massive Yellow or Red warning onto the screen to break 
 * the user's psychological trance and warn them of the scam.
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.CyberGuardInCallService
import com.example.utils.Constants
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.example.ui.ActiveCallScreen

class IncomingCallActivity : ComponentActivity() {
    private val TAG = "IncomingCallActivity"

    private var score by mutableStateOf(0)
    private var transcript by mutableStateOf("")
    private var hitWord by mutableStateOf("")
    private var callerNumber by mutableStateOf("Unknown Caller")
    private var activeStage by mutableStateOf("VAD")
    private var isScamScenario by mutableStateOf(false)
    private var hasVibrated = false
    private var isAnswered by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        callerNumber = intent.getStringExtra(Constants.EXTRA_CALLER_NUMBER) ?: "+91 98765 43210"
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.utils.CallStateBroadcaster.telemetryFlow.collect { update ->
                    score = update.score
                    transcript = update.transcript
                    hitWord = update.hitWord
                    callerNumber = update.callerNumber
                    activeStage = update.stage
                    isScamScenario = update.isScamScenario
                    
                    val threshold = getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
                        .getInt("alert_threshold", 70)
                    if (score >= threshold) {
                        if (!hasVibrated) {
                            vibrateAlert()
                            hasVibrated = true
                        }
                    } else {
                        hasVibrated = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.utils.CallStateBroadcaster.callEndedFlow.collect {
                    Log.d(TAG, "Call finished flow event collected. Closing Call Screen.")
                    finish()
                }
            }
        }

        val threshold = getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
            .getInt("alert_threshold", 70)

        setContent {
            MyApplicationTheme {
                ActiveCallScreen(
                    phoneNumber = callerNumber,
                    callDuration = "Live Call",
                    isScamDetected = (score >= threshold),
                    liveTranscript = transcript,
                    onEndCall = {
                        CyberGuardInCallService.disconnectCall()
                        finish()
                    }
                )
            }
        }
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
