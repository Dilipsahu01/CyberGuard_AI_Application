package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class WhitelistEntry(
    val name: String,
    val number: String,
    val initiallyWhitelisted: Boolean
)

private val mockWhitelistContacts = listOf(
    WhitelistEntry("Amisha", "+91 7622365663", false),
    WhitelistEntry("Bunty", "+91 7622365663", false),
    WhitelistEntry("Mom", "+91 9876543210", true),
    WhitelistEntry("Dad", "+91 9876543211", true),
    WhitelistEntry("Chirag Bansal", "+91 7622365663", false)
)

@Composable
fun WhitelistScreen(
    onBackClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Standardized Top Bar
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Gray800,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Trusted Contacts",
                        style = MaterialTheme.typography.titleLarge,
                        color = Gray800,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Info Banner
            Surface(
                color = BlueBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Calls from whitelisted contacts completely bypass the AI pipeline to save battery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray700,
                        lineHeight = 18.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "YOUR CONTACTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray500,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                items(mockWhitelistContacts) { contact ->
                    WhitelistContactCard(contact)
                }
            }
        }
    }
}

@Composable
private fun WhitelistContactCard(entry: WhitelistEntry) {
    var isWhitelisted by remember { mutableStateOf(entry.initiallyWhitelisted) }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Blue100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                Text(
                    text = entry.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }

            Switch(
                checked = isWhitelisted,
                onCheckedChange = { isWhitelisted = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Primary)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WhitelistScreenPreview() {
    WhitelistScreen()
}
