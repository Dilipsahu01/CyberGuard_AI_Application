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
                IncomingCallScreen(
                    number = callerNumber,
                    score = score,
                    transcript = transcript,
                    hitWord = hitWord,
                    stage = activeStage,
                    threshold = threshold,
                    onDecline = {
                        CyberGuardInCallService.disconnectCall()
                        finish()
                    },
                    onAnswer = {
                        CyberGuardInCallService.answerCall()
                        isAnswered = true
                    },
                    isAnswered = isAnswered
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

@Composable
fun IncomingCallScreen(
    number: String,
    score: Int,
    transcript: String,
    hitWord: String,
    stage: String,
    threshold: Int,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
    isAnswered: Boolean
) {
    val isScamDanger = score >= threshold

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Border alpha animation
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Concentric pulsing avatar rings animations
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha2"
    )

    // Score counter animation
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "scoreCounter"
    )

    // Warning background overlay pulse alpha
    val overlayPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlayPulse"
    )

    val backColor = if (isScamDanger) Color(0xFF160101) else Color(0xFF030307)
    val accentColor = if (isScamDanger) Color(0xFFEF5350) else Color(0xFF00F0FF)

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backColor)
                .border(
                    width = 4.dp,
                    color = accentColor.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(0.dp)
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Security Engine Header & Live Risk badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CYBERGUARD NETWORK AI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RISK: $animatedScore%",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // MID: Caller Info + Pulsing Avatar Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    // Pulsing Outer Concentric Rings
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale1)
                            .alpha(pulseAlpha1)
                            .border(3.dp, accentColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale2)
                            .alpha(pulseAlpha2)
                            .border(1.5.dp, accentColor, CircleShape)
                    )
                    
                    // Core Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(2.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScamDanger) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = "Ringing avatar",
                            tint = accentColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isScamDanger) "⚠️ HIGH PROBABILITY PHISHING ALERT" else if (isAnswered) "CALL IN PROGRESS - ANALYZING SPEECH..." else "RINGING - ANALYZING SIGNALS...",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp),
                    fontFamily = FontFamily.Monospace
                )
            }

            // TRANSCRIPT CONSOLE BOX
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "REAL-TIME TRANSCRIPT DECODER (5G stream):",
                    color = Color(0xFF888899),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0A12))
                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (transcript.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterHorizontally),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ringing... Listening for acoustic VAD speech activation...",
                                color = Color(0xFF555566),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = transcript,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (hitWord.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x15EF5350))
                                        .border(1.dp, Color(0x30EF5350), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "KEYWORDS TRIGGERED: $hitWord",
                                        color = Color(0xFFEF5350),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM: Call Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RED DECLINE
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF5350))
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Hang up",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // GREEN ANSWER (Hide if answered)
                if (!isAnswered) {
                    IconButton(
                        onClick = onAnswer,
                        enabled = !isScamDanger,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isScamDanger) Color(0x44222222) else Color(0xFF00FF44))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer Call",
                            tint = if (isScamDanger) Color.DarkGray else Color(0xFF020205),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Active Red Overlay Warning pulse overlay (renders only when high danger detected)
        if (isScamDanger) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEF5350).copy(alpha = overlayPulseAlpha))
                    .border(
                        width = 6.dp,
                        color = Color(0xFFEF5350).copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}
