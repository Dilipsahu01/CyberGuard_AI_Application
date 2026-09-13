package com.example

import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.ActiveCallViewModel
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import android.telecom.Call
import android.telephony.SmsManager
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import com.example.ui.ActiveCallLayout
import com.example.ui.CallDirection

class IncomingCallActivity : ComponentActivity() {
    private val TAG = "IncomingCallActivity"

    private var score by mutableIntStateOf(0)
    private var transcript by mutableStateOf("")
    private var hitWord by mutableStateOf("")
    private var callerNumber by mutableStateOf("Unknown Caller")
    private var activeStage by mutableStateOf("VAD")
    private var isScamScenario by mutableStateOf(false)
    private var hasVibrated = false
    private var isAnswered by mutableStateOf(false)
    private var isIncomingCall by mutableStateOf(true)
    private lateinit var activeCallViewModel: ActiveCallViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        activeCallViewModel = androidx.lifecycle.ViewModelProvider(this)[ActiveCallViewModel::class.java]
        
        val callDirectionInt = intent.getIntExtra("call_direction", -1)
        val initialDirection = when (callDirectionInt) {
            0 -> CallDirection.INCOMING
            1 -> CallDirection.OUTGOING
            else -> CallDirection.MISSED
        }
        isIncomingCall = callDirectionInt == 0
        activeCallViewModel.setCallDirection(initialDirection)
        
        // The UI now entirely relies on activeCallsFlow to render state.
        // We no longer manually register callbacks here.

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

        val threshold = getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
            .getInt("alert_threshold", 70)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.utils.CallStateBroadcaster.callEndedFlow.collect {
                    Log.d(TAG, "Call finished flow event collected. Closing Call Screen.")
                    if (isScamScenario || score >= threshold) {
                        val reviewIntent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                            putExtra("EXTRA_SCAM_NUMBER", callerNumber)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(reviewIntent)
                    }
                    finish()
                }
            }
        }

        setContent {
            MyApplicationTheme {
                if (activeStage == "COMPROMISED") {
                    Surface(color = Color(0xFFFEE2E2), modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("SYSTEM COMPROMISED", color = Color(0xFFEF4444), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text("The application detected unusual behaviour or malware in the device. We can't load the CyberGuard AI.", color = Color(0xFFEF4444), fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    val activeCalls by CyberGuardInCallService.activeCallsFlow.collectAsStateWithLifecycle()

                    // Logic for choosing which call to render prominently
                    val primaryCall = activeCalls.firstOrNull { it.state == com.example.services.CallUiState.ACTIVE }
                        ?: activeCalls.firstOrNull { it.state == com.example.services.CallUiState.HELD }
                        ?: activeCalls.firstOrNull { it.state == com.example.services.CallUiState.DIALING }
                        ?: activeCalls.firstOrNull { it.state == com.example.services.CallUiState.RINGING }
                    
                    val secondaryRingingCall = activeCalls.firstOrNull { it.state == com.example.services.CallUiState.RINGING && it.call != primaryCall?.call }
                    val secondaryHeldCall = activeCalls.firstOrNull { it.state == com.example.services.CallUiState.HELD && it.call != primaryCall?.call }
                    
                    if (primaryCall == null) {
                        // Empty state, finishing
                        androidx.compose.runtime.LaunchedEffect(Unit) { finish() }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                            if (primaryCall.state == com.example.services.CallUiState.ACTIVE || primaryCall.state == com.example.services.CallUiState.DIALING || primaryCall.state == com.example.services.CallUiState.HELD) {
                                LaunchedEffect(primaryCall.state) {
                                    activeCallViewModel.setIsOnHold(primaryCall.state == com.example.services.CallUiState.HELD)
                                    if (primaryCall.state == com.example.services.CallUiState.ACTIVE) {
                                        activeCallViewModel.startTimer()
                                    } else {
                                        activeCallViewModel.stopTimer()
                                    }
                                }
                                val isScamDetectedDerived by androidx.compose.runtime.remember(threshold) {
                                    androidx.compose.runtime.derivedStateOf { score >= threshold }
                                }
                                
                                Box(modifier = Modifier.fillMaxSize()) {
                                ActiveCallScreen(
                                    viewModel = activeCallViewModel,
                                    phoneNumber = primaryCall.rawNumber,
                                    callDuration = if (primaryCall.state == com.example.services.CallUiState.HELD) "On Hold" else if (primaryCall.state == com.example.services.CallUiState.DIALING) "Dialing..." else "Live Call",
                                    isScamDetected = isScamDetectedDerived,
                                    liveTranscript = transcript,
                                    onEndCall = {
                                        CyberGuardInCallService.disconnectCall(primaryCall.call)
                                        if (score >= threshold) {
                                            val intent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                                                putExtra("EXTRA_SCAM_NUMBER", primaryCall.rawNumber)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            }
                                            startActivity(intent)
                                        }
                                    },
                                    showSwapButton = (secondaryHeldCall != null),
                                    onSwap = {
                                        secondaryHeldCall?.let {
                                            CyberGuardInCallService.holdCall(true, primaryCall.call)
                                            CyberGuardInCallService.holdCall(false, it.call)
                                        }
                                    },
                                    onToggleHold = {
                                        val isCurrentlyHeld = primaryCall.state == com.example.services.CallUiState.HELD
                                        CyberGuardInCallService.holdCall(!isCurrentlyHeld, primaryCall.call)
                                    }
                                )
                                
                                if (secondaryRingingCall != null) {
                                    CallWaitingBanner(
                                        phoneNumber = secondaryRingingCall.rawNumber,
                                        onAccept = {
                                            CyberGuardInCallService.holdCall(true, primaryCall.call)
                                            CyberGuardInCallService.answerCall(secondaryRingingCall.call)
                                        },
                                        onDecline = {
                                            CyberGuardInCallService.disconnectCall(secondaryRingingCall.call)
                                        }
                                    )
                                }
                            }
                        } else {
                            RingingScreen(
                                phoneNumber = primaryCall.rawNumber,
                                isScamDetected = (score >= threshold),
                                onAccept = {
                                    CyberGuardInCallService.answerCall(primaryCall.call)
                                },
                                onDecline = {
                                    CyberGuardInCallService.disconnectCall(primaryCall.call)
                                    if (score >= threshold) {
                                        val intent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                                            putExtra("EXTRA_SCAM_NUMBER", primaryCall.rawNumber)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        startActivity(intent)
                                    }
                                },
                                onReply = { message ->
                                    try {
                                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            this@IncomingCallActivity.getSystemService(SmsManager::class.java)
                                        } else {
                                            SmsManager.getDefault()
                                        }
                                        smsManager.sendTextMessage(primaryCall.rawNumber, null, message, null, null)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to send SMS", e)
                                    }
                                    CyberGuardInCallService.disconnectCall(primaryCall.call)
                                    if (score >= threshold) {
                                        val intent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                                            putExtra("EXTRA_SCAM_NUMBER", primaryCall.rawNumber)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        startActivity(intent)
                                    }
                                }
                            )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CallWaitingBanner(
        phoneNumber: String,
        onAccept: () -> Unit,
        onDecline: () -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 48.dp) // Below status bar
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Incoming Call", color = Color.Gray, fontSize = 12.sp)
                    Text(phoneNumber, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier.background(Color(0xFFEF4444), CircleShape)
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.background(Color(0xFF10B981), CircleShape)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RingingScreen(
        phoneNumber: String,
        isScamDetected: Boolean,
        onAccept: () -> Unit,
        onDecline: () -> Unit,
        onReply: (String) -> Unit
    ) {
        val context = LocalContext.current
        var contactName by remember { mutableStateOf<String?>(null) }
        var showReplySheet by remember { mutableStateOf(false) }

        LaunchedEffect(phoneNumber) {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx != -1) {
                        contactName = cursor.getString(idx)
                    }
                }
            }
        }

        Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Incoming Call",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(Modifier.height(24.dp))
                
                if (isScamDetected) {
                    com.example.ui.ScamWarningOverlay(
                        scamStatus = com.example.ui.ScamStatus.SCAM,
                        scamScore = score.toFloat()
                    )
                    Spacer(Modifier.height(32.dp))
                }
                
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_anim"
                )

                val avatarBg = if (isScamDetected) Color(0xFFFEE2E2) else Color(0xFFE0E7FF)
                val avatarTint = if (isScamDetected) Color(0xFFEF4444) else Color(0xFF4F46E5)

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Caller",
                        tint = avatarTint,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                if (contactName != null) {
                    Text(
                        text = contactName ?: "",
                        color = Color.Black,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = phoneNumber,
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = phoneNumber,
                        color = Color.Black,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.weight(1f))

                SwipeToAnswerSlider(
                    onAccept = onAccept,
                    onDecline = onDecline
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Reply Button (Smaller, text-only or subtle icon)
                TextButton(onClick = { showReplySheet = true }) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Reply", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("Reply with Message", color = Color.Gray)
                }
            }

            if (showReplySheet) {
                ModalBottomSheet(onDismissRequest = { showReplySheet = false }) {
                    val prefs = context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
                    val quickResponses = prefs.getStringSet("quick_responses", setOf(
                        "Can't talk now. What's up?",
                        "I'll call you right back.",
                        "I'm in a meeting."
                    ))?.toList() ?: emptyList()

                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Quick Responses", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        quickResponses.forEach { response ->
                            TextButton(
                                onClick = {
                                    showReplySheet = false
                                    onReply(response)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(response, fontSize = 16.sp, color = Color.Black)
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }

@Suppress("DEPRECATION")
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
fun SwipeToAnswerSlider(
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val trackWidth = 300.dp
    val trackWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { trackWidth.toPx() }
    val thumbSize = 64.dp
    val thumbSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbSize.toPx() }
    val maxDragPx = (trackWidthPx / 2f) - (thumbSizePx / 2f) - 16f
    
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(80.dp)
            .clip(CircleShape)
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Answer Call") { onAccept(); true },
                    CustomAccessibilityAction("Decline Call") { onDecline(); true }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.Gray, modifier = Modifier.size(28.dp))
            Text("Slide to Answer", color = Color.Gray, fontSize = 16.sp)
            Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.Gray, modifier = Modifier.size(28.dp))
        }

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    val velocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { _ -> velocityTracker.resetTracking() },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            coroutineScope.launch {
                                if (offsetX.value > maxDragPx * 0.7f || velocity > 1000f) {
                                    offsetX.animateTo(maxDragPx)
                                    onAccept()
                                } else if (offsetX.value < -maxDragPx * 0.7f || velocity < -1000f) {
                                    offsetX.animateTo(-maxDragPx)
                                    onDecline()
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val iconTint = when {
                offsetX.value > 0 -> Color(0xFF10B981)
                offsetX.value < 0 -> Color(0xFFEF4444)
                else -> Color.Black
            }
            val icon = when {
                offsetX.value > 10f -> Icons.Filled.Call
                offsetX.value < -10f -> Icons.Filled.CallEnd
                else -> Icons.Filled.Call
            }
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
        }
    }
}
