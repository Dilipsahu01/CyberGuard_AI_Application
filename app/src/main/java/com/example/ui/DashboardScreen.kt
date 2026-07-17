package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// Reusing your established theme colors, plus a green for safe/active status
val SwarmGreen = Color(0xFF16A34A)
val SwarmGreenBg = Color(0xFFDCFCE7)

@Composable
fun DashboardScreen(
    viewModel: PipelineViewModel? = null,
    onNavigateToDialer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {}
) {
    val scamLogs by viewModel?.scamLogs?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp) // Standardized Edge Padding
            ) {
                // Shared App Header (from Components.kt)
                AppHeader(
                    onSettingsClick = onNavigateToSettings,
                    onLogsClick = onNavigateToLogs
                )

                Spacer(Modifier.height(24.dp))

                // ---- Swarm Status Indicator (Animated) ----
                SwarmActiveBadge()

                Spacer(Modifier.height(24.dp))

                // ---- Hero Stat Card ----
                ThreatHeroCard(scamLogs.size.toString())

                Spacer(Modifier.height(32.dp))

                // ---- Analytics Breakdown ----
                Text(
                    text = "THREAT BREAKDOWN",
                    style = MaterialTheme.typography.labelLarge,
                    color = Gray500,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ThreatProgressRow(label = "Financial Coercion", percentage = 0.65f, count = "18", color = RedEndCall)
                        Spacer(Modifier.height(20.dp))
                        ThreatProgressRow(label = "Tech Support Scam", percentage = 0.20f, count = "5", color = Primary)
                        Spacer(Modifier.height(20.dp))
                        ThreatProgressRow(label = "Romance / Trust", percentage = 0.15f, count = "4", color = Gray500)
                    }
                }

                Spacer(Modifier.height(100.dp)) // Clearance for bottom Floating Button
            }

            // Standard Floating Dialer Button (from Components.kt)
            FloatingDialerButton(
               onClick = onNavigateToDialer,
               modifier = Modifier
                   .align(Alignment.BottomCenter)
                   .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SwarmActiveBadge() {
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

    Surface(
        color = SwarmGreenBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SwarmGreen.copy(alpha = alpha))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Swarm Intelligence Network Active",
                color = SwarmGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ThreatHeroCard(count: String = "27") {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Threats Neutralized",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = count,
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 56.sp
                    )
                    Text(
                        text = " this month",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.GppGood,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
private fun ThreatProgressRow(label: String, percentage: Float, count: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Gray800
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
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
