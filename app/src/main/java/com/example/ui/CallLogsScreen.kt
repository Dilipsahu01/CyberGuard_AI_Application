package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NOTE ON FIDELITY: the source Figma thumbnail shows two expanded rows using
 * slightly different secondary actions ("Add Contact" with a plus icon vs. an
 * edit icon labeled "Add Contact" again) -- almost certainly an inconsistency
 * in the source file rather than intentional. I standardized on Call / Add
 * Contact / All Call Logs for every expanded row here, since these are calls
 * from numbers not yet saved as contacts.
 */

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
    // We can just use the db direct state or the viewModel state
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
        
        // Add the last group
        val direction = if (currentLog.wasBlocked) CallDirection.MISSED else CallDirection.INCOMING
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val timeString = sdf.format(java.util.Date(currentLog.timestamp))
        val durationStr = if (currentLog.durationSeconds > 0) "${currentLog.durationSeconds}s" else "Not Received"
        val subTitle = if (currentLog.isScam) "Scam Blocked (Score: ${currentLog.riskScore})" else "Call, $durationStr"
        val title = if (currentGroupCount > 1) "${currentLog.callerNumber} ($currentGroupCount)" else currentLog.callerNumber
        grouped.add(CallLogEntry(direction, title, subTitle, timeString, count = currentGroupCount))
        
        grouped
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { AppHeader() }
                item { SearchBar() }
                item {
                    TabSwitcher(
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            if (it == 1) onNavigateToRecordings()
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item { SectionHeader("All Logs") }

                items(groupedLogs) { entry ->
                    CallLogRow(entry)
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
            FloatingDialerButton(
                onClick = onOpenDialer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        color = Gray800,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun CallLogRow(entry: CallLogEntry) {
    val (icon, tint) = when (entry.direction) {
        CallDirection.OUTGOING -> Icons.Filled.CallMade to Gray700
        CallDirection.INCOMING -> Icons.Filled.CallReceived to Gray700
        CallDirection.MISSED -> Icons.Filled.PhoneDisabled to Gray700
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (entry.expanded) it.background(Gray100) else it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderGray)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title, 
                    fontSize = 15.sp, 
                    fontWeight = if (entry.direction == CallDirection.MISSED) FontWeight.Bold else FontWeight.Medium, 
                    color = if (entry.direction == CallDirection.MISSED) AlertRedBorder else Gray800
                )
                Text(entry.subtitle, fontSize = 13.sp, color = MutedForeground)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(entry.timeTop, fontSize = 15.sp, color = Gray700)
                entry.timeBottom?.let {
                    Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Gray400)
                }
            }
        }
        if (entry.expanded) {
            ActionRow(
                primaryLabel = "Call",
                primaryIcon = Icons.Filled.Call,
                onPrimaryClick = {},
                secondaryLabel = "Add Contact",
                secondaryIcon = Icons.Filled.PersonAdd,
                onSecondaryClick = {},
                onAllLogsClick = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CallLogsScreenPreview() {
    CallLogsScreen()
}
