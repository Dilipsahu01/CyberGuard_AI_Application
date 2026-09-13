package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AlertRedBg = Color(0xFFFEE2E2)
val AlertRedBorder = Color(0xFFEF4444)

@Composable
fun ActiveCallScreen(
    viewModel: ActiveCallViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    phoneNumber: String = "+91 7622365663",
    callDuration: String = "01:24",
    isScamDetected: Boolean = false,
    liveTranscript: String = "",
    showSwapButton: Boolean = false,
    onSwap: () -> Unit = {},
    onToggleHold: () -> Unit = {},
    onEndCall: () -> Unit = {}
) {
    val direction by viewModel.callDirection.collectAsStateWithLifecycle()

    when (direction) {
        CallDirection.INCOMING -> ActiveCallLayout(
            phoneNumber = phoneNumber,
            callDuration = callDuration,
            isScamDetected = isScamDetected,
            liveTranscript = liveTranscript,
            showSwapButton = showSwapButton,
            onSwap = onSwap,
            onToggleHold = onToggleHold,
            onEndCall = onEndCall,
            isIncoming = true,
            viewModel = viewModel
        )
        CallDirection.OUTGOING -> ActiveCallLayout(
            phoneNumber = phoneNumber,
            callDuration = callDuration,
            isScamDetected = isScamDetected,
            liveTranscript = liveTranscript,
            showSwapButton = showSwapButton,
            onSwap = onSwap,
            onToggleHold = onToggleHold,
            onEndCall = onEndCall,
            isIncoming = false,
            viewModel = viewModel
        )
        else -> {
            // No UI for missed calls here
        }
    }
}

@Composable
fun ActiveCallLayout(
    phoneNumber: String,
    callDuration: String,
    isScamDetected: Boolean,
    liveTranscript: String,
    showSwapButton: Boolean,
    onSwap: () -> Unit,
    onToggleHold: () -> Unit,
    onEndCall: () -> Unit,
    isIncoming: Boolean,
    viewModel: ActiveCallViewModel
) {
    val currentOnSwap by androidx.compose.runtime.rememberUpdatedState(onSwap)
    val currentOnToggleHold by androidx.compose.runtime.rememberUpdatedState(onToggleHold)
    val currentOnEndCall by androidx.compose.runtime.rememberUpdatedState(onEndCall)
    // States to manage the advanced tools
    var showCaptions by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    var notesText by rememberSaveable { mutableStateOf("") }
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // States for 6-Button Dialer Grid
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val isOnHold by viewModel.isOnHold.collectAsStateWithLifecycle()
    val isBluetoothOn by viewModel.isBluetoothOn.collectAsStateWithLifecycle()
    var showKeypad by rememberSaveable { mutableStateOf(false) }

    val formattedDuration by viewModel.callElapsedFormatted.collectAsStateWithLifecycle()

    var contactName by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(phoneNumber) {
        // Query contact name using ContactsContract on IO Thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
    }



    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Call duration pill ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallMade,
                    contentDescription = null,
                    tint = Gray500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = if (isIncoming) "Incoming..." else formattedDuration, color = Gray500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing/status indicator dot
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(1000),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "alphaPulse"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .graphicsLayer { this.alpha = alpha }
                        .background(if (isScamDetected) AlertRedBorder else Color(0xFF10B981))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isScamDetected) "AI Fraud Sequence Identified" else "AI Active Scanning: Regex + NLP", 
                    color = if (isScamDetected) AlertRedBorder else Primary, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }

            val isOverlayVisible by viewModel.isOverlayVisible.collectAsStateWithLifecycle()
            val scamStatus by viewModel.scamStatus.collectAsStateWithLifecycle()
            val scamScore by viewModel.scamScore.collectAsStateWithLifecycle()
            
            if (isScamDetected && isOverlayVisible) {
                Spacer(Modifier.height(32.dp))
                ScamWarningOverlay(
                    scamStatus = scamStatus,
                    scamScore = scamScore,
                    isOverlayVisible = isOverlayVisible,
                    onFeedback = { isScam ->
                        viewModel.dismissOverlayAndSetFeedback(isScam)
                    }
                )
            } else {
                Spacer(Modifier.weight(0.5f))
            }

            if (isScamDetected) Spacer(Modifier.weight(0.5f))

            // ---- Avatar + number ----
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val avatarBg = if (isScamDetected) AlertRedBg else Blue100
                val avatarTint = if (isScamDetected) AlertRedBorder else Color.White
                
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(avatarBg)
                        .border(width = 4.dp, color = avatarBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Caller avatar",
                        tint = avatarTint,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                if (showSwapButton) {
                    androidx.compose.material3.Button(
                        onClick = currentOnSwap,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Swap Calls") // Using Dialpad icon as generic "switch" placeholder or any standard icon
                        Spacer(Modifier.width(8.dp))
                        Text("Swap Call")
                    }
                }

                if (contactName != null) {
                    Text(
                        text = contactName ?: "",
                        color = Gray800,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = phoneNumber,
                        color = Gray600,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = phoneNumber,
                        color = Gray800,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(Modifier.height(32.dp))

                // ---- Standard 6-Button Dialer Grid ----
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.width(280.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CallControlButton(Icons.Filled.MicOff, "Mute", isActive = isMuted) { viewModel.toggleMute() }
                        CallControlButton(Icons.Filled.Dialpad, "Keypad", isActive = showKeypad) { showKeypad = !showKeypad }
                        CallControlButton(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", isActive = isSpeakerOn) { viewModel.toggleSpeaker() }
                    }
                    Row(
                        modifier = Modifier.width(280.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CallControlButton(
                            icon = Icons.Filled.Add,
                            label = "Add Call",
                            isActive = false,
                            onClick = {
                                // TODO(backend): Implement conference calling via Telecom API
                                android.widget.Toast.makeText(context, "Conference calling coming soon", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        CallControlButton(Icons.Filled.Pause, "Hold", isActive = isOnHold) { currentOnToggleHold() }
                        CallControlButton(Icons.Filled.Bluetooth, "Bluetooth", isActive = isBluetoothOn) { viewModel.toggleBluetooth() }
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // ---- Advanced Security Tools Toggles ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Live Captions Toggle
                    ToolTogglePill(
                        icon = Icons.Filled.ClosedCaption,
                        label = "Captions",
                        isActive = showCaptions,
                        onClick = { 
                            showCaptions = !showCaptions
                            if (showCaptions) showNotes = false // Mutually exclusive for screen space
                        }
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    // Evidence Pad Toggle
                    ToolTogglePill(
                        icon = Icons.Filled.EditNote,
                        label = "Take Note",
                        isActive = showNotes,
                        onClick = { 
                            showNotes = !showNotes
                            if (showNotes) showCaptions = false
                        }
                    )
                }
            }

            Spacer(Modifier.weight(0.5f))

            // ---- Live Transcription Window ----
            AnimatedVisibility(
                visible = showCaptions,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                TranscriptionCard(liveTranscript)
            }

            // ---- Evidence Pad Window ----
            AnimatedVisibility(
                visible = showNotes,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                EvidencePadCard(
                    text = notesText,
                    onTextChange = { notesText = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ---- End call button ----
            if (isScamDetected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RedEndCall)
                        .clickable(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            currentOnEndCall()
                        }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "End Call Now",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(RedEndCall)
                        .clickable(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            currentOnEndCall()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(icon: ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit = {}) {
    val bgColor = if (isActive) Color(0xFFD1E4FF) else Gray50
    val tintColor = if (isActive) Color(0xFF004B71) else Gray700
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(width = 1.dp, color = if (isActive) Color(0xFF004B71) else BorderGray, shape = CircleShape)
                .clickable(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, color = tintColor, fontSize = 14.sp)
    }
}

@Composable
private fun ToolTogglePill(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp) // Ensure minimum touch target size
            .clip(CircleShape)
            .background(if (isActive) Primary else Color.White)
            .border(1.dp, if (isActive) Primary else BorderGray, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else Gray700,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else Gray700,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TranscriptionCard(liveTranscript: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Gray50),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "LIVE AI TRANSCRIPT",
                    color = Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = liveTranscript.ifEmpty { "Awaiting audio input frames..." }, 
                fontSize = 15.sp, 
                lineHeight = 22.sp, 
                color = Gray800
            )
        }
    }
}

@Composable
private fun EvidencePadCard(text: String, onTextChange: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SCAM EVIDENCE PAD",
                color = Gray500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Jot down fake badge IDs, reference numbers, or URLs...", color = Gray400, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Gray800)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActiveCallScreenPreview() {
    ActiveCallScreen(isScamDetected = false)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScamAlertActiveCallScreenPreview() {
    ActiveCallScreen(isScamDetected = true)
}
