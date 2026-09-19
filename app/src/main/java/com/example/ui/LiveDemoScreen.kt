package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.core.content.ContextCompat
import com.example.models.RiskResult
import com.example.pipeline.PipelineSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.ui.ScamStatus
import com.example.ui.ScamWarningOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDemoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isRecording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("Ready to transcribe...") }
    var finThreat by remember { mutableFloatStateOf(0f) }
    var urgThreat by remember { mutableFloatStateOf(0f) }
    
    var recordJob by remember { mutableStateOf<Job?>(null) }
    var isOverlayVisible by remember { mutableStateOf(true) }
    
    // Simulation Settings
    var isTrustedContact by remember { mutableStateOf(false) }
    var isBlockedContact by remember { mutableStateOf(false) }
    var sensitivityThreshold by remember { mutableFloatStateOf(70f) }
    var manualText by remember { mutableStateOf("") }
    
    val scamScore = maxOf(finThreat, urgThreat) * 100
    val scamStatus = when {
        isTrustedContact -> ScamStatus.SAFE
        isBlockedContact -> ScamStatus.SCAM
        scamScore > sensitivityThreshold -> ScamStatus.SCAM
        scamScore > 40 -> ScamStatus.SUSPICIOUS
        else -> ScamStatus.SAFE
    }
    
    val displayScore = if (isBlockedContact) 100f else scamScore
    
    // Using mutableStateOf without custom getter/setter so Compose tracks it properly
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Threat Simulator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(
                text = "Speak into your microphone to simulate a live phone call. The ASR and NLP engine will process your voice in real-time just like a real call.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Simulation Control Panel
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulation Context Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Caller is Trusted Contact", modifier = Modifier.weight(1f))
                        Switch(
                            checked = isTrustedContact,
                            onCheckedChange = { 
                                isTrustedContact = it
                                if (it) isBlockedContact = false // Cannot be both trusted and blocked
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Caller is on Blocklist", modifier = Modifier.weight(1f))
                        Switch(
                            checked = isBlockedContact,
                            onCheckedChange = { 
                                isBlockedContact = it
                                if (it) isTrustedContact = false 
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI Sensitivity Threshold: ${sensitivityThreshold.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = sensitivityThreshold,
                        onValueChange = { sensitivityThreshold = it },
                        valueRange = 0f..100f
                    )
                }
            }
            
            // Trusted Contact Badge
            if (isTrustedContact) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Scanning Bypassed (Trusted Contact)", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }

            ScamWarningOverlay(
                scamStatus = scamStatus,
                scamScore = displayScore,
                isOverlayVisible = isOverlayVisible,
                onFeedback = { isScam ->
                    if (!isScam) {
                        isOverlayVisible = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Threat Visualizers
            ThreatBar(label = "Financial Threat", probability = finThreat, color = Color(0xFFEF4444))
            Spacer(modifier = Modifier.height(16.dp))
            ThreatBar(label = "Urgency/Coercion", probability = urgThreat, color = Color(0xFFF59E0B))
            
            Spacer(modifier = Modifier.height(32.dp))

            // Transcript Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                )
            }

            // DEBUG STATS
            var debugAmp by remember { mutableStateOf("Amp: N/A") }
            var debugVad by remember { mutableStateOf("VAD: N/A") }
            var debugAsr by remember { mutableStateOf("ASR: N/A") }
            
            LaunchedEffect(transcript) {
                val pm = PipelineSingleton.getInstance(context)
                debugVad = "VAD: ${pm.vad.speechActive} (Prob: ${pm.vad.lastProb})"
                debugAsr = "ASR Stream Ready: ${pm.asr.isStreamReady()}"
                debugAmp = "Mic Last Amp: ${pm.lastAmplitude}"
            }

            Text(text = "$debugAmp | $debugVad | $debugAsr", fontSize = 10.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // ASR Override / Manual Injection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    label = { Text("ASR Override (Type to Simulate)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (manualText.isNotBlank()) {
                            val pm = PipelineSingleton.getInstance(context)
                            val result = pm.processManualText(manualText)
                            finThreat = result.intents.financial.toFloat() / 100f
                            urgThreat = result.intents.urgency.toFloat() / 100f
                            transcript = result.transcript
                            isOverlayVisible = true
                            manualText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp) // Match height of OutlinedTextField
                ) {
                    Text("Inject")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Record Button
            FloatingActionButton(
                onClick = {
                    if (!hasPermission) {
                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                        return@FloatingActionButton
                    }

                    if (isRecording) {
                        isRecording = false
                        recordJob?.cancel()
                    } else {
                        isRecording = true
                        transcript = "Initializing Staged Boot... Please Wait..."
                        finThreat = 0f
                        urgThreat = 0f
                        isOverlayVisible = true
                        
                        recordJob = coroutineScope.launch(Dispatchers.IO) {
                            startLiveInference(
                                context = context,
                                onResult = { result ->
                                    finThreat = result.intents.financial.toFloat() / 100f
                                    urgThreat = result.intents.urgency.toFloat() / 100f
                                    transcript = result.transcript
                                },
                                isRecordingProvider = { isRecording }
                            )
                        }
                    }
                },
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Record",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Stop Simulation" else "Start Live Simulation",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        recordJob?.cancel()
                    } else {
                        isRecording = true
                        transcript = "Initializing Staged Boot... Please Wait..."
                        finThreat = 0f
                        urgThreat = 0f
                        isOverlayVisible = true
                        
                        recordJob = coroutineScope.launch(Dispatchers.IO) {
                            startSimulationInference(
                                context = context,
                                onResult = { result ->
                                    finThreat = result.intents.financial.toFloat() / 100f
                                    urgThreat = result.intents.urgency.toFloat() / 100f
                                    transcript = result.transcript
                                },
                                isRecordingProvider = { isRecording }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (isRecording) "Stop Audio Simulation" else "Simulate Audio File (Scam Demo)")
            }
        }
    }
}

@Composable
fun ThreatBar(label: String, probability: Float, color: Color) {
    val animatedProb by animateFloatAsState(targetValue = probability, label = "probAnim")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Text(text = "${(animatedProb * 100).toInt()}%", fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProb)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLiveInference(
    context: Context,
    onResult: (RiskResult) -> Unit,
    isRecordingProvider: () -> Boolean
) {
    // This will trigger the Staged Boot if not loaded, just like a real call!
    val pipeline = PipelineSingleton.getInstance(context)
    pipeline.reset() // Clear any old text from a previous call/demo

    val sampleRate = 16000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    val bufferSize = minBuf.coerceAtLeast(3200)
    
    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.UNPROCESSED,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize
    )

    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        return
    }

    val sliceSize = 512
    val shortBuffer = ShortArray(sliceSize)
    val floatBuffer = FloatArray(sliceSize)

    audioRecord.startRecording()

    while (isRecordingProvider()) {
        val read = audioRecord.read(shortBuffer, 0, sliceSize)
        if (read > 0) {
            // Convert to FloatArray [-1.0, 1.0]
            for (i in 0 until read) {
                floatBuffer[i] = shortBuffer[i].toFloat() / 32768.0f
            }
            
            // Process exact chunk
            val exactSlice = if (read == sliceSize) floatBuffer else floatBuffer.copyOfRange(0, read)
            val result = pipeline.processChunk(exactSlice)
            
            onResult(result)
        } else {
            android.util.Log.e("LiveDemoScreen", "AudioRecord read returned <= 0: $read")
        }
    }

    audioRecord.stop()
    audioRecord.release()
}

@SuppressLint("MissingPermission")
private suspend fun startSimulationInference(
    context: Context,
    onResult: (RiskResult) -> Unit,
    isRecordingProvider: () -> Boolean
) {
    val pipeline = PipelineSingleton.getInstance(context)
    pipeline.reset()

    try {
        context.assets.open("scam_test.wav").use { inputStream ->
            // Skip 44 bytes of WAV header
            inputStream.skip(44)
            
            val sliceSize = 512
            val byteBuffer = ByteArray(sliceSize * 2) // 16-bit = 2 bytes per sample
            val floatBuffer = FloatArray(sliceSize)
            
            while (isRecordingProvider() && kotlinx.coroutines.currentCoroutineContext().isActive) {
                val bytesRead = inputStream.read(byteBuffer)
                if (bytesRead <= 0) break
                
                val samplesRead = bytesRead / 2
                val shortBuf = java.nio.ByteBuffer.wrap(byteBuffer, 0, bytesRead)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    
                for (i in 0 until samplesRead) {
                    floatBuffer[i] = shortBuf.get(i).toFloat() / 32768.0f
                }
                
                val exactSlice = if (samplesRead == sliceSize) floatBuffer else floatBuffer.copyOfRange(0, samplesRead)
                // Pass true to bypass VAD for simulation, guaranteeing ASR processes it
                val result = pipeline.processChunk(exactSlice, skipNlp = false, isSimulation = true)
                
                onResult(result)
                
                // Sleep for exactly 32ms (512 samples at 16kHz) to simulate real-time stream
                kotlinx.coroutines.delay(32)
            }
            
            // Simulation finished, force flush the ASR to get the final words
            val finalResult = pipeline.processChunk(FloatArray(0), skipNlp = false, isSimulation = true).copy(
                transcript = pipeline.asr.forceEndpoint()
            )
            // Just append the forced endpoint text directly via manual injection
            if (finalResult.transcript.isNotBlank()) {
                onResult(pipeline.processManualText(finalResult.transcript))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("LiveDemoScreen", "Failed to simulate audio: ${e.message}")
    }
}
