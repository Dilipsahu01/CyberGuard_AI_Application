package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Using the green tokens we established in the Dashboard
private val SuccessGreen = Color(0xFF16A34A)
private val SuccessGreenBg = Color(0xFFDCFCE7)

@Composable
fun PostCallReviewScreen(
    blockedNumber: String = "+91 7622365663",
    onContributeHash: () -> Unit = {},
    onReturnToDialer: () -> Unit = {}
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ---- Hero Shield Indicator ----
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SuccessGreenBg)
                    .border(4.dp, SuccessGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GppGood,
                    contentDescription = "Threat Neutralized",
                    tint = SuccessGreen,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ---- Title & Subtitle ----
            Text(
                text = "Threat Neutralized",
                color = SuccessGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "The call from $blockedNumber was securely terminated based on localized AI threat analysis.",
                color = Gray500,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(48.dp))

            // ---- AI Pipeline Analytics Card ----
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "DETECTION CONFIDENCE",
                        color = Gray500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // High risk parameters
                    ScoreRow(label = "Financial Intent", score = 0.92f, scoreText = "92/100", color = RedEndCall)
                    Spacer(Modifier.height(16.dp))
                    ScoreRow(label = "Urgency / Coercion", score = 0.85f, scoreText = "85/100", color = RedEndCall)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Low risk parameter
                    ScoreRow(label = "Deepfake Probability", score = 0.12f, scoreText = "12/100", color = SuccessGreen)
                }
            }

            Spacer(Modifier.weight(1f))

            // ---- Primary Action: Swarm Network ----
            Button(
                onClick = onContributeHash,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Contribute Hash to Swarm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---- Secondary Action: Dismiss ----
            OutlinedButton(
                onClick = onReturnToDialer,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray800),
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Back to Dialer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float, scoreText: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Text(text = scoreText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { score },
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
private fun PostCallReviewScreenPreview() {
    PostCallReviewScreen()
}
