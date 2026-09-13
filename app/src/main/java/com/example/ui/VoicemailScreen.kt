package com.example.ui

import android.content.ContentUris
import android.database.Cursor
import android.media.MediaPlayer
import android.provider.VoicemailContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class VoicemailItem(val id: Long, val number: String, val duration: Long, val date: Long, val uri: android.net.Uri)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicemailScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    var voicemails by remember { mutableStateOf<List<VoicemailItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            voicemails = getVoicemails(context)
        }
    }
    var currentlyPlaying by remember { mutableStateOf<VoicemailItem?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentlyPlaying) {
        while (currentlyPlaying != null && mediaPlayer?.isPlaying == true) {
            progress = mediaPlayer?.currentPosition?.toFloat() ?: 0f
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voicemail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (voicemails.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No voicemails", color = Gray500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(voicemails, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                deleteVoicemail(context, item.id)
                                voicemails = getVoicemails(context)
                                if (currentlyPlaying?.id == item.id) {
                                    mediaPlayer?.release()
                                    currentlyPlaying = null
                                }
                                true
                            } else false
                        }
                    )
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(RedEndCall)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        content = {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item.number, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                java.text.SimpleDateFormat("MMM dd, h:mm a").format(java.util.Date(item.date)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Gray500
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Badge(containerColor = Blue100, contentColor = Primary) {
                                                Text("${item.duration}s", modifier = Modifier.padding(4.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (currentlyPlaying?.id == item.id) {
                                                        mediaPlayer?.pause()
                                                        currentlyPlaying = null
                                                    } else {
                                                        mediaPlayer?.release()
                                                        mediaPlayer = MediaPlayer.create(context, item.uri)
                                                        mediaPlayer?.start()
                                                        mediaPlayer?.setOnCompletionListener {
                                                            currentlyPlaying = null
                                                            progress = 0f
                                                        }
                                                        currentlyPlaying = item
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    if (currentlyPlaying?.id == item.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play/Pause"
                                                )
                                            }
                                        }
                                    }
                                    if (currentlyPlaying?.id == item.id) {
                                        val total = mediaPlayer?.duration?.toFloat() ?: 1f
                                        LinearProgressIndicator(
                                            progress = { if (total > 0) progress / total else 0f },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun getVoicemails(context: android.content.Context): List<VoicemailItem> {
    val list = mutableListOf<VoicemailItem>()
    try {
        val cursor: Cursor? = context.contentResolver.query(
            VoicemailContract.Voicemails.CONTENT_URI,
            arrayOf(
                VoicemailContract.Voicemails._ID,
                VoicemailContract.Voicemails.NUMBER,
                VoicemailContract.Voicemails.DURATION,
                VoicemailContract.Voicemails.DATE
            ),
            null, null, "${VoicemailContract.Voicemails.DATE} DESC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(VoicemailContract.Voicemails._ID)
            val numIdx = it.getColumnIndexOrThrow(VoicemailContract.Voicemails.NUMBER)
            val durIdx = it.getColumnIndexOrThrow(VoicemailContract.Voicemails.DURATION)
            val dateIdx = it.getColumnIndexOrThrow(VoicemailContract.Voicemails.DATE)

            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val uri = ContentUris.withAppendedId(VoicemailContract.Voicemails.CONTENT_URI, id)
                list.add(
                    VoicemailItem(
                        id = id,
                        number = it.getString(numIdx) ?: "Unknown",
                        duration = it.getLong(durIdx),
                        date = it.getLong(dateIdx),
                        uri = uri
                    )
                )
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
    return list
}

private fun deleteVoicemail(context: android.content.Context, id: Long) {
    val uri = ContentUris.withAppendedId(VoicemailContract.Voicemails.CONTENT_URI, id)
    context.contentResolver.delete(uri, null, null)
}
