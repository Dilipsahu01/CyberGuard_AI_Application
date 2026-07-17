package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RecordingEntry(
    val title: String,
    val subtitle: String,
    val duration: String,
    val progress: Float? = null
)

@Composable
fun CallRecordingsScreen(
    onNavigateToCallLogs: () -> Unit = {},
    onOpenDialer: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.example.models.ScamDatabase.getDatabase(context) }
    val logs by db.callLogDao().getAllLogs().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(1) }

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
                            if (it == 0) onNavigateToCallLogs()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    Text(
                        text = "ALL RECORDINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray500,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                val recordedLogs = logs.filter { it.durationSeconds > 0 }
                items(recordedLogs) { log ->
                    val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                    val timeString = sdf.format(java.util.Date(log.timestamp))
                    val mins = log.durationSeconds / 60
                    val secs = log.durationSeconds % 60
                    val durStr = String.format("%02d:%02d", mins, secs)
                    RecordingCard(RecordingEntry(log.callerNumber, "Call, $timeString", durStr))
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
private fun RecordingCard(entry: RecordingEntry) {
    val isPlaying = entry.progress != null

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMade,
                contentDescription = null,
                tint = Gray700,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }

            if (isPlaying) {
                // Responsive Progress Bar
                LinearProgressIndicator(
                    progress = { entry.progress ?: 0f },
                    modifier = Modifier
                        .width(60.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Primary,
                    trackColor = Gray200,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = entry.duration,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) Primary else Gray700
            )

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = { /* Play/Pause */ }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Primary else Gray700,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CallRecordingsScreenPreview() {
    CallRecordingsScreen()
}
