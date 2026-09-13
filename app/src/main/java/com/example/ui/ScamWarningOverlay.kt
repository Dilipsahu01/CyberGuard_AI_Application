package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*

@Composable
fun ScamWarningOverlay(
    scamStatus: ScamStatus,
    scamScore: Float,
    isOverlayVisible: Boolean = true,
    onFeedback: (Boolean) -> Unit = {}
) {
    // Only show the overlay if the status is SUSPICIOUS or SCAM and it hasn't been dismissed
    val isVisible = isOverlayVisible && (scamStatus == ScamStatus.SUSPICIOUS || scamStatus == ScamStatus.SCAM)

    var hasClickedYes by remember { mutableStateOf(false) }

    val containerColor = when {
        hasClickedYes -> Color(0xFF121212)
        scamStatus == ScamStatus.SCAM -> Color(0xFFD32F2F) // High-contrast Red
        scamStatus == ScamStatus.SUSPICIOUS -> Color(0xFFFBC02D) // High-contrast Yellow
        else -> Color(0xFF388E3C) // Safe Green
    }

    val icon = when {
        hasClickedYes -> Icons.Filled.Warning
        scamStatus == ScamStatus.SCAM -> Icons.Filled.Error
        scamStatus == ScamStatus.SUSPICIOUS -> Icons.Filled.Warning
        else -> Icons.Filled.CheckCircle
    }

    val warningText = when {
        hasClickedYes -> "PLEASE END CALL NOW - HARMFUL CALLER"
        scamStatus == ScamStatus.SCAM -> "CRITICAL THREAT: SCAM DETECTED"
        scamStatus == ScamStatus.SUSPICIOUS -> "WARNING: SUSPICIOUS ACTIVITY"
        else -> "CALL IS SECURE"
    }
    
    val textColor = if (!hasClickedYes && scamStatus == ScamStatus.SUSPICIOUS) Color.Black else Color.White

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 500)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 500)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(containerColor, shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (hasClickedYes) Color(0xFFFF5252) else textColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = warningText,
                            color = if (hasClickedYes) Color(0xFFFF5252) else textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (!hasClickedYes) {
                            Text(
                                text = "AI Threat Score: ${scamScore.toInt()}%",
                                color = textColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                if (!hasClickedYes) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Is this a scam?",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onFeedback(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No, Safe", color = textColor)
                        }
                        Button(
                            onClick = { 
                                hasClickedYes = true
                                onFeedback(true) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes, Scam", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
