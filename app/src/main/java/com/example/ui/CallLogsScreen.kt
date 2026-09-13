package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Removed private enum to use global CallDirection
private data class CallLogEntry(
    val direction: CallDirection,
    val title: String,
    val subtitle: String,
    val timeTop: String,
    val timeBottom: String? = null,
    val expanded: Boolean = false,
    val count: Int = 1,
    val number: String,
    val id: Int
)

private fun lookupContactName(context: android.content.Context, number: String): String? {
    var name: String? = null
    try {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (idx != -1) {
                    name = cursor.getString(idx)
                }
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return name
}

@Composable
fun CallLogsScreen(
    viewModel: PipelineViewModel? = null,
    onNavigateToRecordings: () -> Unit = {},
    onOpenDialer: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.example.models.ScamDatabase.getDatabase(context) }
    val logs by (viewModel?.allLogs ?: db.callLogDao().getAllLogs()).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val contactNamesMap = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(logs) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            logs.forEach { log ->
                if (!contactNamesMap.containsKey(log.callerNumber)) {
                    val name = lookupContactName(context, log.callerNumber)
                    if (name != null) contactNamesMap[log.callerNumber] = name
                }
            }
        }
    }

    val groupedLogs = remember(logs, searchQuery) {
        val grouped = mutableListOf<CallLogEntry>()
        if (logs.isEmpty()) return@remember grouped

        var currentGroupCount = 1
        var currentLog = logs.first()

        for (i in 1 until logs.size) {
            val log = logs[i]
            if (log.callerNumber == currentLog.callerNumber && log.wasBlocked == currentLog.wasBlocked && log.direction == currentLog.direction) {
                currentGroupCount++
            } else {
                val direction = when (currentLog.direction) {
                    0 -> CallDirection.INCOMING
                    1 -> CallDirection.OUTGOING
                    2 -> CallDirection.MISSED
                    else -> CallDirection.UNKNOWN
                }
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val timeString = sdf.format(java.util.Date(currentLog.timestamp))
                val durationStr = if (currentLog.durationSeconds > 0) "${currentLog.durationSeconds}s" else "Not Received"
                val subTitle = if (currentLog.isScam) "Scam Blocked (Score: ${currentLog.riskScore})" else "Call, $durationStr"

                val contactName = contactNamesMap[currentLog.callerNumber]
                val baseTitle = contactName ?: currentLog.callerNumber
                val title = if (currentGroupCount > 1) "$baseTitle ($currentGroupCount)" else baseTitle

                if (searchQuery.isBlank() || baseTitle.contains(searchQuery, ignoreCase = true) || currentLog.callerNumber.contains(searchQuery)) {
                    grouped.add(CallLogEntry(direction, title, subTitle, timeString, count = currentGroupCount, number = currentLog.callerNumber, id = currentLog.id))
                }

                currentLog = log
                currentGroupCount = 1
            }
        }

        val direction = when (currentLog.direction) {
            0 -> CallDirection.INCOMING
            1 -> CallDirection.OUTGOING
            2 -> CallDirection.MISSED
            else -> CallDirection.UNKNOWN
        }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val timeString = sdf.format(java.util.Date(currentLog.timestamp))
        val durationStr = if (currentLog.durationSeconds > 0) "${currentLog.durationSeconds}s" else "Not Received"
        val subTitle = if (currentLog.isScam) "Scam Blocked (Score: ${currentLog.riskScore})" else "Call, $durationStr"
        val contactName = contactNamesMap[currentLog.callerNumber]
        val baseTitle = contactName ?: currentLog.callerNumber
        val title = if (currentGroupCount > 1) "$baseTitle ($currentGroupCount)" else baseTitle
        
        if (searchQuery.isBlank() || baseTitle.contains(searchQuery, ignoreCase = true) || currentLog.callerNumber.contains(searchQuery)) {
            grouped.add(CallLogEntry(direction, title, subTitle, timeString, count = currentGroupCount, number = currentLog.callerNumber, id = currentLog.id))
        }

        grouped
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AppHeader() }
                item { SearchBar(query = searchQuery, onQueryChange = { searchQuery = it }) }
                item {
                    TabSwitcher(
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            if (it == 1) onNavigateToRecordings()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    Text(
                        text = "ALL LOGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray500,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (groupedLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No call logs available.", color = Gray500)
                        }
                    }
                } else {
                    items(items = groupedLogs, key = { it.id }) { entry ->
                        var isExpanded by remember { mutableStateOf(false) }
                        CallLogCard(entry = entry.copy(expanded = isExpanded), onClick = { isExpanded = !isExpanded })
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            FloatingDialerButton(
                onClick = onOpenDialer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun CallLogCard(entry: CallLogEntry, onClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (icon, tint) = when (entry.direction) {
        CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Gray700
        CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to Gray700
        CallDirection.MISSED -> Icons.Filled.PhoneDisabled to RedEndCall
        CallDirection.UNKNOWN -> Icons.Filled.Call to Gray700
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (entry.direction == CallDirection.MISSED) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (entry.direction == CallDirection.MISSED) RedEndCall else Gray800
                    )
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.timeTop,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Gray700
                    )
                    entry.timeBottom?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray400
                        )
                    }
                }
            }

            if (entry.expanded) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:${entry.number}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Primary)
                    }
                    IconButton(onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, entry.number)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Contact", tint = Gray700)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CallLogsScreenPreview() {
    CallLogsScreen()
}
