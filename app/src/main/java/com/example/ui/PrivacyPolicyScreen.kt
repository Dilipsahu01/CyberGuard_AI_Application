package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ---- Top Bar (matching Advancedsettingsscreen.kt) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Gray800,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBackClick)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Privacy Policy",
                    color = Gray800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = BorderGray, thickness = 1.dp)

            // ---- Content ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Key Highlights Box
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Gray50), // Subtle off-white from Theme.kt
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OUR COMMITMENT TO TRUST",
                            color = Gray500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        HighlightRow("100% On-Device Processing")
                        Spacer(Modifier.height(8.dp))
                        HighlightRow("No PII is Stored")
                        Spacer(Modifier.height(8.dp))
                        HighlightRow("Zero Audio Sent to Cloud")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Standard Typography Content
                Text(
                    text = "Data Collection & Usage",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "CyberGuard AI is built on a privacy-first architecture. Because we use localized TinyML models (like Silero VAD) installed directly onto your device, the analysis of call audio streams happens securely within your phone's memory state.\n\nNo audio recording, transcription, or voice snippet is ever transmitted to remote servers. When a call concludes, the active memory buffer analyzing the audio is instantly flushed.",
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = Gray700
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Threat Intelligence Sharing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If Swarm Threat Intelligence is enabled in your Advanced Settings, the application will solely share anonymized 82-bit metadata hashes related to known scam caller patterns. This process mathematically strips any personally identifiable information (PII) before leaving your device.",
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = Gray700
                )
                
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun HighlightRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle, 
            contentDescription = null, 
            tint = Primary, 
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Gray800,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PrivacyPolicyScreenPreview() {
    PrivacyPolicyScreen()
}
