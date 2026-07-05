package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mock data for the whitelist
private data class WhitelistEntry(
    val name: String,
    val number: String,
    val initiallyWhitelisted: Boolean
)

private val mockWhitelistContacts = listOf(
    WhitelistEntry("Amisha", "+91 7622365663", false),
    WhitelistEntry("Bunty", "+91 7622365663", false),
    WhitelistEntry("Mom", "+91 9876543210", true), // Whitelisted by default
    WhitelistEntry("Dad", "+91 9876543211", true), // Whitelisted by default
    WhitelistEntry("Chirag Bansal", "+91 7622365663", false)
)

@Composable
fun WhitelistScreen(
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
                    text = "Trusted Contacts",
                    color = Gray800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = BorderGray, thickness = 1.dp)

            // ---- Info Banner ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlueBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Shield",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Calls from whitelisted contacts completely bypass the AI pipeline, saving device battery and CPU.",
                    color = Gray700,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            
            // ---- Search Bar (from Components.kt) ----
            // Uncomment if SearchBar() is defined in your Components.kt
            // SearchBar()
            
            // ---- Contacts List ----
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "YOUR CONTACTS",
                        color = Gray800,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                
                items(mockWhitelistContacts) { contact ->
                    WhitelistContactRow(contact)
                }
                
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
private fun WhitelistContactRow(entry: WhitelistEntry) {
    // Local state to manage the switch toggle per contact
    var isWhitelisted by remember { mutableStateOf(entry.initiallyWhitelisted) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Blue100)
                .border(width = 1.dp, color = Blue100, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Contact Details
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Medium, 
                color = Gray800
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.number, 
                fontSize = 13.sp, 
                color = MutedForeground
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Custom Material 3 Switch matching Advancedsettingsscreen.kt
        Switch(
            checked = isWhitelisted,
            onCheckedChange = { isWhitelisted = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Gray50,
                uncheckedBorderColor = BorderGray
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WhitelistScreenPreview() {
    WhitelistScreen()
}
