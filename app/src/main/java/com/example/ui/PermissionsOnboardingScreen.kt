package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionsOnboardingScreen(
    onGrantPermissions: () -> Unit = {}
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Header Icon
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = "Security Shield",
                tint = Primary,
                modifier = Modifier.size(72.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Protecting Your Calls",
                color = Gray800,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "CyberGuard AI needs the following permissions to provide real-time scam detection.",
                color = Gray500,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(48.dp))

            // Permissions List
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                
                PermissionRow(
                    icon = Icons.Filled.Mic,
                    title = "Microphone",
                    subtitle = "Required to run on-device AI voice analysis. Audio never leaves your device."
                )
                
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                
                PermissionRow(
                    icon = Icons.Filled.Phone,
                    title = "Phone/Dialer",
                    subtitle = "Required to manage incoming calls and display caller information."
                )
                
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                
                PermissionRow(
                    icon = Icons.Filled.Contacts,
                    title = "Contacts",
                    subtitle = "Used to bypass AI scanning for trusted family and friends."
                )
                
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
            }

            Spacer(Modifier.weight(1f))

            // Action Button
            Button(
                onClick = onGrantPermissions,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Grant Permissions",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray800
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Gray500,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionsOnboardingScreenPreview() {
    PermissionsOnboardingScreen()
}
