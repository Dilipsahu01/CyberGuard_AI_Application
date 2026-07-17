package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
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

private enum class CallDirection { OUTGOING, INCOMING, MISSED }

private data class CallLogEntry(
    val direction: CallDirection,
    val title: String,
    val subtitle: String,
    val timeTop: String,
    val timeBottom: String? = null,
    val expanded: Boolean = false,
    val count: Int = 1
)

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

    val groupedLogs = remember(logs) {
        val grouped = mutableListOf<CallLogEntry>()
        if (logs.isEmpty()) return@remember grouped

        var currentGroupCount = 1
        var currentLog = logs.first()

        for (i in 1 until logs.size) {
            val log = logs[i]
            if (log.callerNumber == currentLog.callerNumber && log.wasBlocked == currentLog.wasBlocked) {
                currentGroupCount++
            } else {
                val direction = if (currentLog.wasBlocked) CallDirection.MISSED else CallDirection.INCOMING
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val timeString = sdf.format(java.util.Date(currentLog.timestamp))
                val durationStr = if (currentLog.durationSeconds > 0) "${currentLog.durationSeconds}s" else "Not Received"
                val subTitle = if (currentLog.isScam) "Scam Blocked (Score: ${currentLog.riskScore})" else "Call, $durationStr"

                val title = if (currentGroupCount > 1) "${currentLog.callerNumber} ($currentGroupCount)" else currentLog.callerNumber
                grouped.add(CallLogEntry(direction, title, subTitle, timeString, count = currentGroupCount))

                currentLog = log
                currentGroupCount = 1
            }
        }

        val direction = if (currentLog.wasBlocked) CallDirection.MISSED else CallDirection.INCOMING
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val timeString = sdf.format(java.util.Date(currentLog.timestamp))
        val durationStr = if (currentLog.durationSeconds > 0) "${currentLog.durationSeconds}s" else "Not Received"
        val subTitle = if (currentLog.isScam) "Scam Blocked (Score: ${currentLog.riskScore})" else "Call, $durationStr"
        val title = if (currentGroupCount > 1) "${currentLog.callerNumber} ($currentGroupCount)" else currentLog.callerNumber
        grouped.add(CallLogEntry(direction, title, subTitle, timeString, count = currentGroupCount))

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
                item { SearchBar() }
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

                items(groupedLogs) { entry ->
                    CallLogCard(entry)
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
private fun CallLogCard(entry: CallLogEntry) {
    val (icon, tint) = when (entry.direction) {
        CallDirection.OUTGOING -> Icons.Filled.CallMade to Gray700
        CallDirection.INCOMING -> Icons.Filled.CallReceived to Gray700
        CallDirection.MISSED -> Icons.Filled.PhoneDisabled to RedEndCall
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
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
                    IconButton(onClick = { /* Call */ }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Primary)
                    }
                    IconButton(onClick = { /* Add Contact */ }) {
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
