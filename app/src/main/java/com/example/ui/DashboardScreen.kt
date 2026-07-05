package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Reusing your established theme colors, plus a green for safe/active status
val SwarmGreen = Color(0xFF16A34A)
val SwarmGreenBg = Color(0xFFDCFCE7)

@Composable
fun DashboardScreen(
    onNavigateToDialer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {}
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Shared App Header (from Components.kt)
                AppHeader(
                    onSettingsClick = onNavigateToSettings,
                    onLogsClick = onNavigateToLogs
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    
                    // ---- Swarm Status Indicator (Animated) ----
                    SwarmActiveBadge()
                    
                    Spacer(Modifier.height(24.dp))

                    // ---- Hero Stat Card ----
                    ThreatHeroCard()

                    Spacer(Modifier.height(32.dp))

                    // ---- Analytics Breakdown ----
                    Text(
                        text = "THREAT BREAKDOWN",
                        color = Gray500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                    
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ThreatProgressRow(label = "Financial Coercion", percentage = 0.65f, count = "18", color = RedEndCall)
                            Spacer(Modifier.height(16.dp))
                            ThreatProgressRow(label = "Tech Support Scam", percentage = 0.20f, count = "5", color = Primary)
                            Spacer(Modifier.height(16.dp))
                            ThreatProgressRow(label = "Romance / Trust", percentage = 0.15f, count = "4", color = Gray500)
                        }
                    }
                    
                    Spacer(Modifier.height(100.dp)) // FAB clearance
                }
            }

            // Standard Floating Dialer Button (from Components.kt)
            FloatingDialerButton(
               onClick = onNavigateToDialer,
               modifier = Modifier
                   .align(Alignment.BottomCenter)
                   .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SwarmActiveBadge() {
    // Pulsing animation for the green dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SwarmGreenBg)
            .border(1.dp, SwarmGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(SwarmGreen.copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Swarm Intelligence Network Active",
            color = SwarmGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ThreatHeroCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Threats Neutralized",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "27",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 48.sp
                    )
                    Text(
                        text = " this month",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.GppGood,
                contentDescription = "Shield",
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun ThreatProgressRow(label: String, percentage: Float, count: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Gray800)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Gray200,
            strokeCap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DashboardScreenPreview() {
    DashboardScreen()
}
