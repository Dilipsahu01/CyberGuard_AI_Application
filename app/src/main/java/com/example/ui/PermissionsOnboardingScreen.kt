package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

@Composable
fun PermissionsOnboardingScreen(
    onGrantPermissions: () -> Unit = {}
) {
    var hasAcceptedDisclosure by remember { mutableStateOf(false) }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        if (!hasAcceptedDisclosure) {
            ProminentDisclosureScreen(
                onAccept = { hasAcceptedDisclosure = true },
                onDecline = { exitProcess(0) }
            )
        } else {
            SystemPermissionsScreen(onGrantPermissions)
        }
    }
}

@Composable
private fun ProminentDisclosureScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Important Notice",
            tint = Primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Data Privacy & Usage Disclosure",
            color = Gray800,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray50, RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "To protect you from scams, CyberGuard AI collects and processes the following data:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Gray800,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DisclosureItem(
                    title = "1. Microphone Audio",
                    description = "CyberGuard AI collects audio from your phone calls to run real-time AI voice and intent analysis even when the app is closed or not in use."
                )
                DisclosureItem(
                    title = "2. Contacts Data",
                    description = "CyberGuard AI accesses your contacts list to automatically bypass AI scanning for trusted family and friends."
                )
                DisclosureItem(
                    title = "3. Phone State",
                    description = "CyberGuard AI reads your phone state to detect when a call starts and ends, allowing the service to activate only when needed."
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "100% On-Device Processing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Your call audio and contacts NEVER leave your device. All AI analysis is performed strictly on your local hardware.",
                    fontSize = 14.sp,
                    color = Gray700,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("I Agree and Accept", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onDecline) {
            Text("Decline and Exit", color = Gray500, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DisclosureItem(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray800)
        Text(description, fontSize = 14.sp, color = Gray700, lineHeight = 20.sp)
    }
}

@Composable
private fun SystemPermissionsScreen(onGrantPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = "Security Shield",
            tint = Primary,
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Grant System Permissions",
            color = Gray800,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "CyberGuard AI needs the following permissions to activate the defense shield.",
            color = Gray500,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(48.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            PermissionRow(icon = Icons.Filled.Mic, title = "Microphone", subtitle = "Required for AI voice analysis.")
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            PermissionRow(icon = Icons.Filled.Phone, title = "Phone/Dialer", subtitle = "Required to intercept scam calls.")
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            PermissionRow(icon = Icons.Filled.Contacts, title = "Contacts", subtitle = "Used for the trusted whitelist.")
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onGrantPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Grant System Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Gray50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray800)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = Gray500, lineHeight = 18.sp)
        }
    }
}

@Preview
@Composable
fun PreviewDisclosureScreen() {
    PermissionsOnboardingScreen()
}
