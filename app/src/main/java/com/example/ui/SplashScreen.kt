package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAppReady: () -> Unit = {}
) {
    // Define the sequence of backend startup processes
    val bootSequence = listOf(
        "Initializing Silero VAD (Voice Activity)...",
        "Loading Fast Conformer CTC...",
        "Booting NLP Intent Engine...",
        "Securing Local Memory Buffer...",
        "Swarm Intelligence Handshake... OK"
    )

    // State to hold which steps are currently visible
    var visibleSteps by remember { mutableStateOf(listOf<String>()) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Coroutine to simulate the model loading delays
    LaunchedEffect(Unit) {
        delay(500) // Initial pause
        
        bootSequence.forEachIndexed { index, step ->
            visibleSteps = visibleSteps + step
            progress = (index + 1f) / bootSequence.size
            
            // Randomize the delay slightly to feel like real CPU work
            val waitTime = when (index) {
                1 -> 800L  // Large model takes longer
                2 -> 600L
                else -> 400L
            }
            delay(waitTime)
        }
        
        delay(600) // Final pause before navigating away
        onAppReady()
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Spacer(Modifier.weight(1f))

            // ---- App Logo / Icon ----
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "CyberGuard AI",
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // ---- Wordmark ----
            Text(
                text = "CYBERGUARD-AI",
                color = TitleBrown,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "On-Device Scam Protection",
                color = Gray500,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(48.dp))

            // ---- Progress Bar ----
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = Gray200,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.weight(1f))

            // ---- Terminal/Console Output ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray50, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SYSTEM BOOT LOG",
                    color = Gray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(Modifier.height(4.dp))

                bootSequence.forEach { step ->
                    val isVisible = visibleSteps.contains(step)
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { 20 })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Done",
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = step,
                                color = Gray700,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace // Gives it that raw code feel
                            )
                        }
                    }
                }
                
                // Keep the box a fixed height even when empty so the UI doesn't jump
                if (visibleSteps.isEmpty()) {
                    Spacer(Modifier.height(120.dp))
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenPreview() {
    SplashScreen()
}
