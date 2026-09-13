package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.ScamCallEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Pre-allocate formatter to avoid object creation in LazyColumn (Zero-Allocation compliant)
private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
private val dateBuffer = Date()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamHistoryScreen(
    viewModel: ScamHistoryViewModel,
    onBackClick: () -> Unit
) {
    val scamHistory by viewModel.scamHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scam Threat History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Clear History", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF3F4F6) // Light Gray Background
    ) { paddingValues ->
        if (scamHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No threat history found.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Task 3: items with key
                items(scamHistory, key = { it.id }) { call ->
                    ScamHistoryItem(call = call)
                }
            }
        }
    }
}

@Composable
fun ScamHistoryItem(call: ScamCallEntity) {
    // Task 2: Color Coding
    val (statusColor, statusIcon, statusText) = when {
        call.threatScore >= 70 -> Triple(Color(0xFFD32F2F), Icons.Filled.Warning, "CRITICAL RISK")
        call.threatScore >= 40 -> Triple(Color(0xFFFBC02D), Icons.Filled.Warning, "SUSPICIOUS")
        else -> Triple(Color(0xFF10B981), Icons.Filled.CheckCircle, "SAFE")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.phoneNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = statusColor
                    )
                    Text(
                        text = " • Score: ${call.threatScore.toInt()}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Timestamp
            Text(
                text = formatTimestamp(call.timestamp),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    dateBuffer.time = timestamp
    return dateFormatter.format(dateBuffer)
}
