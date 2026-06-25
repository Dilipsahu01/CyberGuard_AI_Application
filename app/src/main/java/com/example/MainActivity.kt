package com.example

/**
 * MainActivity.kt
 * 
 * PURPOSE: 
 * The central UI dashboard for the CyberGuard app.
 * 
 * WHY IT EXISTS:
 * Displays real-time threat telemetry, blocked call history, and allows the user 
 * to manually manage their whitelist. It observes the `CallLogRepository` flows 
 * to automatically update the UI without manual refreshes.
 */
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CallLog
import com.example.models.CallLogRepository
import com.example.models.ScamDatabase
import com.example.ui.PipelineViewModel
import com.example.ui.PipelineViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.Inter
import com.example.ui.theme.RobotoMono
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = PipelineViewModelFactory(application)

        setContent {
            MyApplicationTheme {
                val viewModel: PipelineViewModel = viewModel(factory = factory)
                
                // Refresh permissions state every time screen is shown
                LaunchedEffect(Unit) {
                    viewModel.checkAllPermissions()
                }

                MainScreen(
                    viewModel = viewModel,
                    onConfigurePermissions = {
                        startActivity(Intent(this@MainActivity, PermissionSetupActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    context = this@MainActivity,
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: PipelineViewModel,
    onConfigurePermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    context: Context,
) {
    val logs by viewModel.allLogs.collectAsState()
    val scamsBlocked = logs.count { it.isScam }
    val totalCalls = logs.size
    val safeCalls = totalCalls - scamsBlocked

    var selectedLogForDialog by remember { mutableStateOf<CallLog?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030307))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0F))
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CYBERGUARD AI",
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF44))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "5G NETWORK ACTIVE",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF131322))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Warning Banner if critical permissions are missing
                val hasCore = (viewModel.permissionStates["Record Audio"] == true) &&
                              (viewModel.permissionStates["Default Dialer"] == true)
                if (!hasCore) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1DEF5350))
                                .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(12.dp))
                                .clickable { onConfigurePermissions() }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "CRITICAL PRIVILEGES INACTIVE",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Tap to launch setup wizard and grant required permissions for active call protection.",
                                        color = Color(0xFFFFAAAA),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
                        // Debug button to manually start ScamDetectionService
                        item {
                            Button(
                                onClick = {
                                    val intent = Intent(context, com.example.services.ScamDetectionService::class.java).apply {
                                        putExtra(com.example.utils.Constants.EXTRA_CALLER_NUMBER, "1234567890")
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
                            ) {
                                Text("Start ScamDetectionService (Debug)", color = Color.White)
                            }
                        }
                // 2. LIVE ANALYSIS INTERACTIVE SIMULATOR (Great for judges presentation!)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "5G SIMULATOR PIPELINE",
                        color = Color(0xFF666688),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F0F14))
                            .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            if (!viewModel.isSimulating) {
                                // Passive Simulator state
                                Text(
                                    text = "Run Demo Simulation Tasks",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Demonstrate AI pipelines (VAD → ASR → NLP → Risk Score) instantly on this device without requiring outbound SIM connections.",
                                    color = Color(0xFF888899),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                                    lineHeight = 16.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.startMockCall(context.applicationContext, isScam = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = "Scam", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SCAM CALL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { viewModel.startMockCall(context.applicationContext, isScam = false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF44)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = "Safe", modifier = Modifier.size(16.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SAFE CALL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    }
                                }
                            } else {
                                // Active Simulator state (Ticks dynamically!)
                                val riskColor = when {
                                    viewModel.liveScore >= 70 -> Color(0xFFEF5350)
                                    viewModel.liveScore >= 40 -> Color(0xFFFFB74D)
                                    else -> Color(0xFF00FF44)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "LIVE SPEECH STREAM",
                                            color = Color(0xFF00F0FF),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = viewModel.activeCaller,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(riskColor.copy(alpha = 0.15f))
                                            .border(1.dp, riskColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "RISK: ${viewModel.liveScore}%",
                                            color = riskColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Active Dialog Text Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF060608))
                                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    if (viewModel.liveTranscript.isEmpty()) {
                                        Text(
                                            "Awaiting audio input frames...",
                                            color = Color(0xFF444455),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    } else {
                                        Text(
                                            text = viewModel.liveTranscript,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Category Intent Bars
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IntentBar("FINANCIAL", viewModel.financialScore, Color(0xFF00F0FF), Modifier.weight(1f))
                                    IntentBar("URGENCY", viewModel.urgencyScore, Color(0xFFFF9900), Modifier.weight(1f))
                                    IntentBar("COERCION", viewModel.coercionScore, Color(0xFFEF5350), Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.stopCall(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Hang Up")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DISCONNECT CALL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 3. TELEMETRY STACK MAP (VAD ✓, ASR ✓, Regex ✓, NLP ✓)
                item {
                    Text(
                        text = "DETECTION ENGINE PIPELINE STATUS",
                        color = Color(0xFF666688),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0F14))
                            .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PipelineStageItem("Voice Activity Detector (VAD)", "Detects silent pauses and human vocal energy rates", active = true, statusText = "Active (Silero RNN)")
                        PipelineStageItem("Speech Recognition (ASR)", "Extracts phonetics into plain text in real time", active = viewModel.isSimulating, statusText = if (viewModel.isSimulating) "Streaming..." else "Standby")
                        PipelineStageItem("Keyword Phrase Gate (Regex)", "Intercepts known blacklisted tele-marketing expressions", active = viewModel.isSimulating && viewModel.liveHitKeyword.isNotEmpty(), statusText = "Ready")
                        PipelineStageItem("Intent NLP Vector Classifier", "Calculates semantic cosine similarity on-device", active = (viewModel.isSimulating && (viewModel.financialScore > 0)), statusText = "Active (MiniLM)")
                        PipelineStageItem("Ensemble Scoring Blender", "Aggregates categorical scores based on non-linear safety curves", active = viewModel.isSimulating, statusText = "Ready (Non-linear)")
                    }
                }

                // 4. STATISTICAL SUMMARIES
                item {
                    Text(
                        text = "5G SHIELD NETWORK METRICS",
                        color = Color(0xFF666688),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard("Calls Screened", totalCalls.toString(), Color(0xFF00F0FF), Modifier.weight(1f))
                        StatCard("Scams Blocked", scamsBlocked.toString(), Color(0xFFEF5350), Modifier.weight(1f))
                        StatCard("Safe Clear", safeCalls.toString(), Color(0xFF00FF44), Modifier.weight(1f))
                    }
                }

                // 5. SETUP STATUS CHECKLIST Card
                item {
                    Text(
                        text = "CYBER PROTECTION ACCESS CONFIG",
                        color = Color(0xFF666688),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0F14))
                            .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        viewModel.permissionStates.forEach { (name, granted) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = name,
                                        tint = if (granted) Color(0xFF00FF44) else Color(0xFFEF5350),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = name, color = Color.White, fontSize = 13.sp)
                                }
                                Text(
                                    text = if (granted) "ENABLED" else "MISSING",
                                    color = if (granted) Color(0xFF00FF44) else Color(0xFFEF5350),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        HorizontalDivider(color = Color(0xFF1E1E2E), thickness = 1.dp)

                        Button(
                            onClick = onConfigurePermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x2A00F0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LAUNCH SETUP WIZARD", color = Color(0xFF00F0FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 6. HISTORIC LAUNCH STATS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURE LOG INCIDENT REPORT",
                            color = Color(0xFF666688),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (logs.isNotEmpty()) {
                            Text(
                                "Clear logs",
                                color = Color(0xFFFF5555),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { viewModel.clearAllLogs() }
                                    .padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (logs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F14))
                                .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "Shield", tint = Color(0x1F00F0FF), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No incoming security incidents recorded today.", color = Color(0xFF444455), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    items(
                        items = logs.take(15),
                        key = { it.id }
                    ) { log ->
                        CallLogCard(
                            log = log,
                            onClick = { selectedLogForDialog = log }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Expanded log detail dialog
        selectedLogForDialog?.let { log ->
            AlertDialog(
                onDismissRequest = { selectedLogForDialog = null },
                containerColor = Color(0xFF0E0E18),
                title = {
                    Column {
                        Text(
                            text = "SECURITY AUDIT: REPORT #${log.id}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.callerNumber, 
                            color = Color.White, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Risk score rating", color = Color(0xFF888899), fontSize = 11.sp)
                                Text("${log.riskScore}% Confidence", color = if (log.isScam) Color(0xFFEF5350) else Color(0xFF00FF44), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Call Duration", color = Color(0xFF888899), fontSize = 11.sp)
                                Text("${log.durationSeconds} seconds", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (log.hitKeywords.isNotEmpty()) {
                            Column {
                                Text("Keywords Filter Match", color = Color(0xFF888899), fontSize = 11.sp)
                                Text(log.hitKeywords, color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Column {
                            Text("Full Speech-To-Text deciphered Transcript", color = Color(0xFF888899), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF06060A))
                                    .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(6.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = log.transcript.ifEmpty { "No voice activity recorded during ringing phase." },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedLogForDialog = null }) {
                        Text("CLOSE DISMISS", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun IntentBar(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF888899), fontSize = 8.sp, fontFamily = RobotoMono)
            Text("$value%", color = color, fontSize = 8.sp, fontFamily = RobotoMono, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0x3F444455))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(color)
            )
        }
    }
}

@Composable
fun PipelineStageItem(name: String, desc: String, active: Boolean, statusText: String) {
    val isCompleted = statusText == "COMPLETED"
    val isRunning = active && statusText == "RUNNING"
    
    val rotation by animateFloatAsState(
        targetValue = if (isCompleted) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "rotation"
    )
    
    val cardBackground by animateColorAsState(
        targetValue = when {
            isRunning -> Color(0x1F00F0FF)
            isCompleted -> Color(0x1200FF44)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 400),
        label = "bgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackground)
            .padding(8.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (active) Color(0xFF00FF44) else Color(0xFF555566))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)
            }
            Text(desc, color = Color(0xFF888899), fontSize = 11.sp, modifier = Modifier.padding(start = 14.dp), fontFamily = Inter)
        }
        Text(
            text = statusText,
            color = if (active) Color(0xFF00F0FF) else Color(0xFF555566),
            fontSize = 11.sp,
            fontFamily = RobotoMono,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F14))
            .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color(0xFF888899), fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CallLogCard(log: CallLog, onClick: () -> Unit) {
    val accentColor = if (log.isScam) Color(0xFFEF5350) else Color(0xFF00FF44)
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F14))
            .border(1.dp, Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.isScam) Icons.Default.Warning else Icons.Default.Lock,
                    contentDescription = if (log.isScam) "Threat" else "Clean",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = log.callerNumber, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (log.isScam) "SCAM PATTERNS IDENTIFIED" else "CLEARED SAFE",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateString,
                    color = Color(0xFF555566),
                    fontSize = 10.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${log.riskScore}%",
                color = accentColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun MainScreen(viewModel: PipelineViewModel, onConfigurePermissions: () -> Unit, onOpenSettings: () -> Unit, context: Context) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0A0F)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Shield") },
                    label = { Text("Shield", fontFamily = FontFamily.Monospace) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00F0FF), 
                        selectedTextColor = Color(0xFF00F0FF), 
                        unselectedIconColor = Color.Gray, 
                        unselectedTextColor = Color.Gray, 
                        indicatorColor = Color(0x2200F0FF)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Dialpad") },
                    label = { Text("Dialpad", fontFamily = FontFamily.Monospace) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00FF44), 
                        selectedTextColor = Color(0xFF00FF44), 
                        unselectedIconColor = Color.Gray, 
                        unselectedTextColor = Color.Gray, 
                        indicatorColor = Color(0x2200FF44)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                DashboardScreen(viewModel, onConfigurePermissions, onOpenSettings, context)
            } else {
                DialpadScreen(context)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadScreen(context: Context) {
    var number by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030307))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        val displaySize = when {
            number.length > 12 -> 28.sp
            number.length > 9 -> 34.sp
            else -> 42.sp
        }

        Text(
            text = number, 
            color = Color.White, 
            fontSize = displaySize, 
            fontWeight = FontWeight.Bold, 
            fontFamily = FontFamily.Monospace,
            maxLines = 1, 
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), 
            textAlign = TextAlign.Center
        )
        
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )
        
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131322))
                            .combinedClickable(
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    number += digit 
                                },
                                onLongClick = {
                                    if (digit == "0") {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        number += "+"
                                    }
                                }
                            ), 
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = digit, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), 
            horizontalArrangement = Arrangement.SpaceEvenly, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(76.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FF44))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (number.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                            try {
                                context.startActivity(intent)
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Please grant Phone permissions first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.Black, modifier = Modifier.size(36.dp))
            }
            
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (number.isNotEmpty()) number = number.dropLast(1)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            number = ""
                        }
                    ), 
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Backspace", tint = Color(0xFF888899), modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
