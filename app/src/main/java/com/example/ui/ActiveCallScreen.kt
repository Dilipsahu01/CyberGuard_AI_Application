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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
    phoneNumber: String = "+91 7622365663",
    callDuration: String = "01:24",
    isScamDetected: Boolean = false,
    liveTranscript: String = "",
    onEndCall: () -> Unit = {}
) {
    // States to manage the advanced tools
    var showCaptions by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

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
                    imageVector = Icons.Filled.CallMade,
                    contentDescription = null,
                    tint = Gray500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = callDuration, color = Gray500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            if (isScamDetected) {
                Spacer(Modifier.height(32.dp))
                // ---- SCAM ALERT BANNER ----
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedBg),
                    border = BorderStroke(1.dp, AlertRedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = AlertRedBorder,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HIGH RISK SCAM DETECTED",
                                color = AlertRedBorder,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "AI analysis indicates financial coercion. Hang up immediately.",
                                color = Gray800,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
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

                Text(
                    text = phoneNumber,
                    color = Gray800,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(Modifier.height(32.dp))

                // ---- Basic Call Controls ----
                Row(
                    modifier = Modifier.width(260.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CallControlButton(Icons.Filled.Bluetooth, "Bluetooth")
                    CallControlButton(Icons.Filled.MicOff, "Mute")
                    CallControlButton(Icons.Filled.VolumeUp, "Speaker")
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
                        .clickable(onClick = onEndCall),
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
                        .clickable(onClick = onEndCall),
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
private fun CallControlButton(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Gray50)
                .border(width = 1.dp, color = BorderGray, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Gray700, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, color = Gray700, fontSize = 14.sp)
    }
}

@Composable
private fun ToolTogglePill(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
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
