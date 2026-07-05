package com.example.services

/**
 * ScamDetectionService.kt
 * 
 * PURPOSE: 
 * This Foreground Service is the beating heart of the Android application.
 * It intercepts the phone's microphone natively, manages the 3-second audio recording loop,
 * and controls the UI Overlay (Red/Yellow screens).
 * 
 * WHY IT EXISTS:
 * 1. Background Execution: Keeps the AI pipeline running even if the app is minimized.
 * 2. Hardware Access: Manages WakeLocks and Microphone buffers safely without memory leaks.
 * 3. Database Sync: Loads and saves the caller's 9-byte ContactMemory across multiple sessions.
 */
import android.app.Service
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.preference.PreferenceManager
import com.example.MainActivity
import com.example.models.*
import com.example.pipeline.*
import com.example.database.TelemetryDatabase
import com.example.database.TelemetryQueueItem
import com.example.utils.Constants
import com.example.utils.PhoneNumberUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.Request
import okhttp3.OkHttpClient
import okio.Buffer
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.security.MessageDigest
import kotlin.math.pow
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.*
import kotlinx.coroutines.isActive
import android.app.role.RoleManager
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core foreground service that records audio, runs the AI pipeline, and communicates with the cloud server.
 */
class ScamDetectionService : Service() {
    private val tag = "ScamDetectionService"
    private val notificationId = 2026
    private val notificationChannelId = "cyberguard_scam_detection"

    // UI‑visible state via StateFlow
    private val _scoreFlow = MutableStateFlow(0)
    val scoreFlow = _scoreFlow.asStateFlow()
    private val _transcriptFlow = MutableStateFlow("")
    val transcriptFlow = _transcriptFlow.asStateFlow()
    private val _statusFlow = MutableStateFlow("IDLE")
    val statusFlow = _statusFlow.asStateFlow()

    @Volatile
    private var pipelineManager: PipelineManager? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var isRecording = false
    private var recordThread: Thread? = null
    private var currentCaller = "Unknown"
    private var isScamDemo = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceStartTime = 0L
    private var lastTelemetryTime = 0L

    private val serverWhitelist = mutableSetOf<String>()

    // Room DB for telemetry queue
    private lateinit var telemetryDb: TelemetryDatabase
    // SharedPreferences for theme toggle
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    // ---------- OkHttp client ----------
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    // Using local LAN IP for hackathon testing since Vercel does not support persistent SQLite Go servers
    private val serverBaseUrl = "http://10.164.57.223:8080"

    // ---------- Service lifecycle ----------
    override fun onBind(intent: Intent?): IBinder = LocalBinder()
    inner class LocalBinder : Binder() { fun getService() = this@ScamDetectionService }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Pull latest whitelist from server (fire‑and‑forget)
        serviceScope.launch { syncWhitelistFromServer() }
        // Initialize Room DB
        telemetryDb = Room.databaseBuilder(applicationContext, TelemetryDatabase::class.java, "telemetry_queue.db").build()
        // Start background queue processor
        serviceScope.launch { processQueuePeriodically() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Permission check
        // Verify required permissions for call detection
        val missingPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add("RECORD_AUDIO")
        }
        if (missingPermissions.isNotEmpty()) {
            Log.e(tag, "Missing permissions: ${missingPermissions.joinToString()}. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }
        // Verify Call Screening role
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                Log.e(tag, "Call screening role not held. Stopping service.")
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Caller info
        currentCaller = intent?.getStringExtra(Constants.EXTRA_CALLER_NUMBER) ?: "Unknown"
        isScamDemo = intent?.getBooleanExtra("is_scam_scenario", false) ?: false
        serviceStartTime = System.currentTimeMillis()

        // --- STAGE 1: SOS / EMERGENCY BYPASS ---
        val emergencyNumbers = listOf("100", "101", "102", "112", "911", "999")
        if (emergencyNumbers.contains(currentCaller.replace(Regex("[^0-9]"), ""))) {
            Log.e(tag, "🚨 EMERGENCY SOS NUMBER DETECTED ($currentCaller). Bypassing AI processing completely to guarantee zero latency.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Foreground service with proper type handling
        val notification = buildNotification("CyberGuard AI Active", "CyberGuard AI is actively scanning this call.")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(tag, "Foreground start failed – fallback", e)
            startForeground(notificationId, notification)
        }

        // Wake‑lock to keep CPU alive during recording
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CyberGuard:AudioWake").apply { acquire(10 * 60 * 1000L) }

        // Load AI pipeline asynchronously
        serviceScope.launch {
            try {
                // Load ContactMemory from SQLite
                val db = ScamDatabase.getDatabase(this@ScamDetectionService)
                val numberHash = PhoneNumberUtils.hash(currentCaller)
                val memoryEntity = db.contactMemoryDao().getMemory(numberHash)
                val contactMemory = if (memoryEntity != null) ContactMemory.fromBytes(memoryEntity.memory) else ContactMemory()
                
                // Update basic call metrics
                contactMemory.totalCalls = (contactMemory.totalCalls + 1).coerceAtMost(255)

                val mgr = com.example.pipeline.PipelineSingleton.getInstance(this@ScamDetectionService)
                mgr.reset()
                mgr.setSimulationScenario(isScamDemo)
                mgr.contactMemory = contactMemory
                pipelineManager = mgr
                Log.i(tag, "Pipeline loaded successfully")
                
                if (!isRecording) {
                    startInferencePipeline()
                    startNotificationTick()
                }
            } catch (e: Exception) {
                Log.e(tag, "Pipeline init failed: ${e.message}")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    // ---------- Whitelist synchronization ----------
    private suspend fun syncWhitelistFromServer() {
        try {
            val request = Request.Builder()
                .url("$serverBaseUrl/api/whitelist")
                .get()
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    json.keys().forEach { number ->
                        // Add each number to the in‑memory whitelist
                        addWhitelist(number)
                    }
                    Log.i(tag, "Whitelist sync successful (size=${json.length()})")
                } else {
                    Log.w(tag, "Whitelist sync failed: ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Whitelist sync exception: ${e.message}")
        }
    }

    private fun addWhitelist(number: String) {
        serverWhitelist.add(PhoneNumberUtils.normalize(number))
    }

    private fun isWhitelisted(number: String): Boolean {
        val normalized = PhoneNumberUtils.normalize(number)
        if (serverWhitelist.contains(normalized)) return true
        val settingsPrefs = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
        val localWhitelist = settingsPrefs.getStringSet("whitelist", null) ?: emptySet()
        return localWhitelist.any { PhoneNumberUtils.normalize(it) == normalized }
    }

    private fun isTrusted(number: String): Boolean {
        val settingsPrefs = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
        val trustedContacts = settingsPrefs.getStringSet("trusted_contacts", null) ?: emptySet()
        return trustedContacts.contains(PhoneNumberUtils.normalize(number))
    }

    // ---------- Telemetry push ----------
    private fun pushTelemetry(result: RiskResult) {
        if (isNetworkAvailable()) {
            // Normal HTTP JSON telemetry with hashed caller
            val hashedCaller = hashString(currentCaller)
            val payload = JSONObject().apply {
                put("caller", hashedCaller)
                put("score", result.score)
                put("transcript", result.transcript)
                put("intent_scores", org.json.JSONArray(listOf(
                    result.intents.urgency,
                    result.intents.financial,
                    result.intents.coercion,
                    result.intents.intimacy,
                    result.intents.trust
                )))
                put("timestamp", System.currentTimeMillis())
            }
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), payload.toString())
            val request = Request.Builder()
                .url("$serverBaseUrl/api/telemetry")
                .post(requestBody)
                .build()
            // Retry up to 3 times with exponential back‑off
            serviceScope.launch {
                var attempt = 0
                var success = false
                while (attempt < 2 && !success) {
                    try {
                        httpClient.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                success = true
                            } else {
                                Log.w(tag, "Telemetry rejected (code ${resp.code}), attempt ${attempt + 1}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Telemetry error on attempt ${attempt + 1}: ${e.message}")
                    }
                    if (!success) {
                        attempt++
                        delay(5000L)
                    }
                }
                if (!success) {
                    Log.w(tag, "Telemetry failed after $attempt attempts – queuing for later")
                    // Queue for later transmission
                    enqueueTelemetry(payload)
                    // Notify user via Toast (must run on UI thread)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@ScamDetectionService, "Telemetry failed – data queued for later upload", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // No network – queue telemetry for later and optionally send LoRa fallback
            enqueueTelemetry(JSONObject().apply {
                put("caller", hashString(currentCaller))
                put("score", result.score)
                put("transcript", result.transcript)
                put("intent_scores", org.json.JSONArray(listOf(
                    result.intents.urgency,
                    result.intents.financial,
                    result.intents.coercion,
                    result.intents.intimacy,
                    result.intents.trust
                )))
                put("timestamp", System.currentTimeMillis())
            })
            // Also send LoRa fallback as immediate low‑bandwidth path
            sendLoRaPayload(result)
        }
    }

    // Enqueue telemetry JSON payload into Room DB
    private fun enqueueTelemetry(json: JSONObject) {
        val item = TelemetryQueueItem(
            callerHash = json.getString("caller"),
            score = json.getInt("score"),
            transcript = json.getString("transcript"),
            intentScores = json.getJSONArray("intent_scores").toString(),
            timestamp = json.getLong("timestamp")
        )
        serviceScope.launch {
            telemetryDb.telemetryQueueDao().insert(item)
        }
    }

    // Periodically process queued telemetry when network is available
    private suspend fun processQueuePeriodically() {
        while (coroutineContext.isActive) {
            delay(60_000L) // every minute
            if (isNetworkAvailable()) {
                flushQueue()
            }
        }
    }

    private suspend fun flushQueue() {
        val dao = telemetryDb.telemetryQueueDao()
        val items = dao.getAll()
        for (item in items) {
            // Build request from queued item
            val payload = JSONObject().apply {
                put("caller", item.callerHash)
                put("score", item.score)
                put("transcript", item.transcript)
                put("intent_scores", org.json.JSONArray(item.intentScores))
                put("timestamp", item.timestamp)
            }
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), payload.toString())
            val request = Request.Builder()
                .url("$serverBaseUrl/api/telemetry")
                .post(requestBody)
                .build()
            try {
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        dao.deleteById(item.id)
                    } else {
                        Log.w(tag, "Failed to send queued telemetry (code ${resp.code})")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error sending queued telemetry: ${e.message} - aborting queue flush")
                break // Stop trying to send the rest of the queue if the server is unreachable
            }
        }
    }

    // Simple SHA‑256 hashing utility (hex string)
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    // ---------- Theme toggle (SharedPreferences) ----------
    fun isDarkModeEnabled(): Boolean = prefs.getBoolean("dark_mode", true)

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        // Apply theme immediately (requires activity recreation)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    // ---------- Network utilities ----------
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // ---------- LoRa fallback payload ----------
    private fun sendLoRaPayload(result: RiskResult) {
        // 9‑byte binary payload: 4‑byte caller hash, 1‑byte score, 4‑byte timestamp (seconds)
        val callerHash = currentCaller.hashCode().toLong() and 0xffffffffL
        val timestampSec = (System.currentTimeMillis() / 1000L) and 0xffffffffL
        val buffer = Buffer()
        buffer.writeIntLe(callerHash.toInt())
        buffer.writeByte(result.score)
        buffer.writeIntLe(timestampSec.toInt())
        val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), buffer.readByteArray())
        val request = Request.Builder()
            .url("$serverBaseUrl/api/lora")
            .post(requestBody)
            .build()
        serviceScope.launch {
            try {
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) Log.w(tag, "LoRa payload rejected: ${resp.code}")
                }
            } catch (e: Exception) {
                Log.w(tag, "LoRa send error: ${e.message}")
            }
        }
    }

    // ---------- Inference pipeline (with telemetry) ----------
    private fun startInferencePipeline() {
        isRecording = true
        recordThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBuf.coerceAtLeast(3200)
            var audioRecord: AudioRecord? = null
            // Try MIC then VOICE_RECOGNITION as fallbacks
            val sources = listOf(MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_RECOGNITION)
            for (src in sources) {
                try {
                    @SuppressLint("MissingPermission")
                    val rec = AudioRecord(src, sampleRate, channelConfig, audioFormat, bufferSize)
                    if (rec.state == AudioRecord.STATE_INITIALIZED) { audioRecord = rec; break }
                } catch (e: Exception) { /* try next */ }
            }
            if (audioRecord == null && !isScamDemo) {
                Log.e(tag, "No usable audio source – aborting")
                _statusFlow.value = "MIC_UNAVAILABLE"
                isRecording = false
                return@Thread
            }
            // --- PIPELINE SAFETY CHECK ---
            // Do not even turn on the microphone until the AI is actually loaded
            if (pipelineManager == null) {
                Log.e("AudioLoop", "PipelineManager is null! Wait for 'Pipeline loaded successfully' before recording.")
                return@Thread
            }

            // --- 1. SET UP THE DEDICATED SPACE ---
            val secondsToRecord = 3
            val maxSamples = sampleRate * secondsToRecord // 48,000 samples

            val dedicatedBuffer = FloatArray(maxSamples)
            var currentSampleCount = 0

            // Small hardware buffer for pulling from the mic
            val tempShortBuffer = ShortArray(bufferSize)

            Log.d("AudioLoop", "Starting 3-second Fixed-Window Recording Loop")
            audioRecord?.startRecording()
            _statusFlow.value = "RECORDING"
            var simIdx = 0

            // --- 2. THE RECORDING LOOP ---
            while (isRecording) {
                var readSize = 0
                if (audioRecord != null) {
                    readSize = audioRecord.read(tempShortBuffer, 0, tempShortBuffer.size)
                }

                if (readSize <= 0 && isScamDemo) {
                    readSize = 512
                    for (i in 0 until readSize) {
                        tempShortBuffer[i] = (sin(2.0 * PI * 440.0 * simIdx / sampleRate) * 3276.0).toInt().toShort()
                        simIdx++
                    }
                    Thread.sleep(100) // simulation pacing
                }

                if (readSize > 0) {
                    // Normalize 16-bit PCM to Float (-1.0 to 1.0) and fill the dedicated space
                    for (i in 0 until readSize) {
                        if (currentSampleCount < maxSamples) {
                            dedicatedBuffer[currentSampleCount] = tempShortBuffer[i] / 32768.0f
                            currentSampleCount++
                        }
                    }

                    // --- 3. THE 3-SECOND TRIGGER ---
                    if (currentSampleCount >= maxSamples) {
                        Log.d("AudioLoop", "3 Seconds of audio captured! Sending to AI Slicer.")
                        
                        // Make a quick copy so we can instantly reuse the main buffer
                        val chunkForAI = dedicatedBuffer.copyOf()
                        currentSampleCount = 0 // Ping-pong: reset instantly to keep recording the next 3 seconds
                        
                        // --- 4. THE SLICER & AI PROCESSING (Background Thread) ---
                        serviceScope.launch {
                            val pipeline = pipelineManager
                            if (pipeline != null) {
                                val sliceSize = 512
                                var lastResult: com.example.models.RiskResult? = null
                                
                                // Chop the 3 seconds into tiny slices so Silero VAD doesn't crash
                                for (i in chunkForAI.indices step sliceSize) {
                                    val end = minOf(i + sliceSize, chunkForAI.size)
                                    val slice = chunkForAI.copyOfRange(i, end)
                                    
                                    lastResult = pipeline.processChunk(slice)
                                }
                                
                                Log.d("AudioLoop", "Finished processing 3-second block through VAD/ASR.")
                                
                                val result = lastResult
                                if (result != null) {
                                    // Update UI StateFlow values
                                    _scoreFlow.value = result.score
                                    _transcriptFlow.value = result.transcript
                                    // Decision logic
                                    when {
                                        result.isRoboVoice -> broadcastRoboWarning()
                                        result.score >= 70 -> {
                                            endCallAndNotify(result)
                                            dispatchGuardianAlert(currentCaller)
                                        }
                                        isTrusted(currentCaller) -> { /* trusted – alert only */ }
                                        isWhitelisted(currentCaller) -> { /* whitelisted – no auto‑drop */ }
                                        else -> if (result.score > 40) broadcastRiskUpdate(result)
                                    }
                                    // Telemetry generation is strictly deferred to the Post-Call Feedback UI.
                                    // It only sends SwarmPayloads on False Positives / False Negatives to save bandwidth.
                                }
                                
                            } else {
                                Log.e("AudioLoop", "PipelineManager became null during processing!")
                            }
                        }
                    }
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
        }
        recordThread?.start()
    }

    // ---------- Helper methods ----------
    private fun endCallAndNotify(risk: RiskResult) {
        CyberGuardInCallService.disconnectCall()
        val intent = Intent("com.example.ACTION_SCAM_DETECTED").apply {
            putExtra("score", risk.score)
            putExtra("hit_word", risk.hitWord)
        }
        sendBroadcast(intent)
    }

    private fun broadcastRiskUpdate(risk: RiskResult) {
        val intent = Intent("com.example.ACTION_RISK_UPDATE").apply {
            putExtra("score", risk.score)
            putExtra("transcript", risk.transcript)
            putExtra("hit_word", risk.hitWord)
        }
        sendBroadcast(intent)
    }

    private fun broadcastRoboWarning() {
        Log.w(tag, "Acoustic Deepfake / Robo-voice detected! Triggering UI Overlay alert.")
        _statusFlow.value = "ROBO_WARNING"
        val intent = Intent("com.example.ACTION_ROBO_WARNING")
        sendBroadcast(intent)
        // In IncomingCallActivity, this will turn the screen light red and play an audio warning
    }

    private fun dispatchGuardianAlert(scammerNumber: String) {
        val settingsPrefs = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
        // If Guardian Protection is configured, send the background SMS alert
        val guardianNumber = settingsPrefs.getString("guardian_number", null)
        if (!guardianNumber.isNullOrEmpty()) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val message = "[CyberGuard Alert] A high-risk scam call from $scammerNumber was just intercepted on this device."
                smsManager.sendTextMessage(guardianNumber, null, message, null, null)
                Log.i(tag, "Guardian Alert SMS dispatched to $guardianNumber")
            } catch (e: Exception) {
                Log.e(tag, "Failed to dispatch Guardian Alert SMS: ${e.message}")
            }
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "CyberGuard Scam Detection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startNotificationTick() {
        serviceScope.launch {
            while (isActive) {
                delay(5000L)
                val notification = buildNotification(
                    "CyberGuard AI Active",
                    "CyberGuard AI is actively scanning this call."
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(notificationId, notification)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        recordThread?.join(500)
        
        // Save ContactMemory back to SQLite
        pipelineManager?.contactMemory?.let { memory ->
            CoroutineScope(Dispatchers.IO).launch {
                val db = ScamDatabase.getDatabase(this@ScamDetectionService)
                val numberHash = PhoneNumberUtils.hash(currentCaller)
                db.contactMemoryDao().saveMemory(ContactMemoryEntity(numberHash, memory.toBytes(), System.currentTimeMillis()))
            }
        }
        
        // Always release the WakeLock if held
        wakeLock?.let { if (it.isHeld) it.release() }
        
        // Explicitly close ONNX memory buffers and mapped byte buffers
        com.example.pipeline.PipelineSingleton.clear()
        
        serviceJob.cancel()
    }
}
