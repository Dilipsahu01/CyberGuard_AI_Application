package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RecordingEntry(
    val title: String,
    val subtitle: String,
    val duration: String,
    val progress: Float? = null // 0f..1f when currently "playing"; null otherwise
)



@Composable
fun CallRecordingsScreen(
    onNavigateToCallLogs: () -> Unit = {},
    onOpenDialer: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.example.models.ScamDatabase.getDatabase(context) }
    // We only show recordings for logs that have duration > 0
    val logs by db.callLogDao().getAllLogs().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(1) }

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
                            if (it == 0) onNavigateToCallLogs()
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item { SectionHeader("All Recordings") }
                val recordedLogs = logs.filter { it.durationSeconds > 0 }
                items(recordedLogs) { log ->
                    val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                    val timeString = sdf.format(java.util.Date(log.timestamp))
                    val mins = log.durationSeconds / 60
                    val secs = log.durationSeconds % 60
                    val durStr = String.format("%02d:%02d", mins, secs)
                    RecordingRow(RecordingEntry(log.callerNumber, "Call, $timeString", durStr))
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
private fun RecordingRow(entry: RecordingEntry) {
    val isPlaying = entry.progress != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderGray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.CallMade, contentDescription = null, tint = Gray700, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Text(entry.subtitle, fontSize = 13.sp, color = MutedForeground)
        }

        Icon(
            imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (isPlaying) Primary else Gray700,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(8.dp))

        if (isPlaying) {
            // Mini progress bar for the currently-playing recording
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(Slate200, RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(entry.progress ?: 0f)
                        .background(Blue300, RoundedCornerShape(4.dp))
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = entry.duration,
            fontSize = 15.sp,
            color = if (isPlaying) Primary else Gray700
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CallRecordingsScreenPreview() {
    CallRecordingsScreen()
}
