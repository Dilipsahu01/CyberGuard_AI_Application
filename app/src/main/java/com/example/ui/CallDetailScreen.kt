package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CallLog
import com.example.models.ScamDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallDetailScreen(
    phoneNumber: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { ScamDatabase.getDatabase(context) }
    
    var contactName by remember { mutableStateOf(phoneNumber) }
    var logs by remember { mutableStateOf<List<CallLog>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(phoneNumber) {
        withContext(Dispatchers.IO) {
            // Lookup contact name
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }

            // Get logs
            db.callLogDao().getAllLogs().collect { allLogs ->
                logs = allLogs.filter { it.callerNumber == phoneNumber }.sortedByDescending { it.timestamp }
            }
        }
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Gray800)
                }
                Spacer(Modifier.width(16.dp))
                Text("Call Details", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Gray800)
            }

            // Header Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Blue100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(text = contactName, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Gray800)
                Text(text = phoneNumber, fontSize = 16.sp, color = Gray500)
                
                Spacer(Modifier.height(24.dp))
                
                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GreenCall.copy(alpha = 0.1f))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call", tint = GreenCall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Call", fontSize = 12.sp, color = Gray700)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("sms:$phoneNumber"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.1f))
                                .size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", tint = Primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Message", fontSize = 12.sp, color = Gray700)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val blockedNumber = com.example.models.BlockedNumberEntity(phoneNumber = phoneNumber, reason = "User Blocked")
                                    db.blockedNumberDao().block(blockedNumber)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(RedEndCall.copy(alpha = 0.1f))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = "Block", tint = RedEndCall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Block", fontSize = 12.sp, color = Gray700)
                    }
                }
            }

            HorizontalDivider(color = BorderGray)

            Text(
                text = "CALL HISTORY",
                style = MaterialTheme.typography.labelLarge,
                color = Gray500,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { log ->
                    val isMissed = log.wasBlocked || log.durationSeconds == 0
                    val icon = if (isMissed) Icons.Filled.PhoneDisabled else Icons.AutoMirrored.Filled.CallReceived
                    val tint = if (isMissed) RedEndCall else Gray700
                    
                    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    val timeStr = sdf.format(Date(log.timestamp))
                    
                    val durationMin = log.durationSeconds / 60
                    val durationSec = log.durationSeconds % 60
                    val durationStr = if (isMissed) "Missed/Blocked" else String.format(Locale.getDefault(), "%02d:%02d", durationMin, durationSec)

                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = timeStr, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Gray800)
                                Text(text = durationStr, fontSize = 14.sp, color = Gray500)
                            }
                            if (log.isScam) {
                                Badge(containerColor = RedEndCall) {
                                    Text("Scam ${log.riskScore}", color = Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete History", color = RedEndCall, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Call History") },
            text = { Text("Are you sure you want to delete all call logs for this number?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    coroutineScope.launch(Dispatchers.IO) {
                        logs.forEach { log ->
                            db.callLogDao().deleteLog(log)
                        }
                        withContext(Dispatchers.Main) {
                            onBackClick()
                        }
                    }
                }) {
                    Text("Delete", color = RedEndCall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Gray800)
                }
            }
        )
    }
}
