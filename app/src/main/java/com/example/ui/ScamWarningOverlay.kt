package com.example.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScamWarningOverlay(
    scamStatus: ScamStatus,
    scamScore: Float
) {
    // Only show the overlay if the status is SUSPICIOUS or SCAM
    val isVisible = scamStatus == ScamStatus.SUSPICIOUS || scamStatus == ScamStatus.SCAM

    val containerColor = when (scamStatus) {
        ScamStatus.SCAM -> Color(0xFFD32F2F) // High-contrast Red
        ScamStatus.SUSPICIOUS -> Color(0xFFFBC02D) // High-contrast Yellow
        else -> Color(0xFF388E3C) // Safe Green
    }

    val icon = when (scamStatus) {
        ScamStatus.SCAM -> Icons.Filled.Error
        ScamStatus.SUSPICIOUS -> Icons.Filled.Warning
        else -> Icons.Filled.CheckCircle
    }

    val warningText = when (scamStatus) {
        ScamStatus.SCAM -> "CRITICAL THREAT: SCAM DETECTED"
        ScamStatus.SUSPICIOUS -> "WARNING: SUSPICIOUS ACTIVITY"
        else -> "CALL IS SECURE"
    }
    
    val textColor = if (scamStatus == ScamStatus.SUSPICIOUS) Color.Black else Color.White

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = warningText,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "AI Threat Score: ${scamScore.toInt()}%",
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
