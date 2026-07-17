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
import com.example.security.EnvironmentGuard
import com.example.security.DeviceHealthManager
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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.channels.Channel

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
    @Volatile private var audioRecord: AudioRecord? = null
    private var currentCaller = "Unknown"

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
    // Native Obfuscation (V1.1_Updates Section 2): URL is fetched from encrypted C++ memory
    private val serverBaseUrl = try {
        com.example.security.NativeSecrets.getServerBaseUrl()
    } catch (e: UnsatisfiedLinkError) {
        "http://10.0.2.2:8080/api/v1" // Fallback for local JVM tests
    }

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
        serviceStartTime = System.currentTimeMillis()
        Log.e("AI_ENGINE", "==========================================================")
        Log.e("AI_ENGINE", "BOOT SEQUENCE INITIATED: ScamDetectionService onStartCommand")
        Log.e("AI_ENGINE", "TARGET CALLER: $currentCaller")
        Log.e("AI_ENGINE", "==========================================================")

        // Only promote to Foreground Service if RECORD_AUDIO is granted
        val hasMicPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            val notification = buildNotification("CyberGuard AI Active", "CyberGuard AI is actively scanning this call.")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(notificationId, notification)
                }
            } catch (e: Exception) {
                Log.e(tag, "Foreground start failed – ignoring to prevent crash", e)
            }
        } else {
            Log.w(tag, "RECORD_AUDIO missing. Skipping startForeground to prevent SecurityException crash.")
        }

        // V1.1_Updates Section 3: The Tripwire
        if (EnvironmentGuard.isDeviceCompromised()) {
            Log.e(tag, "SECURITY ALERT: Compromised environment detected. Refusing to boot AI pipeline.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Permission check
        // Verify required permissions for call detection
        val missingPermissions = mutableListOf<String>()
        if (!hasMicPermission && !(intent?.getBooleanExtra("is_scam_scenario", false) ?: false)) {
            missingPermissions.add("RECORD_AUDIO")
        }
        if (missingPermissions.isNotEmpty()) {
            Log.e(tag, "Missing permissions: ${missingPermissions.joinToString()}. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }
        // Verify Call Screening role (Dev Mode bypass allowed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                Log.w(tag, "Call screening role not held. Allowing anyway for Dev Mode.")
            }
        }

        // Caller info
        currentCaller = intent?.getStringExtra(Constants.EXTRA_CALLER_NUMBER) ?: "Unknown"

        serviceStartTime = System.currentTimeMillis()

        // --- STAGE 1: SOS / EMERGENCY BYPASS ---
        val emergencyNumbers = listOf("100", "101", "102", "112", "911", "999")
        val cleanNumber = currentCaller.replace(Regex("[^0-9]"), "")
        val isEmergency = emergencyNumbers.any { emergency ->
            cleanNumber == emergency || (cleanNumber.endsWith(emergency) && cleanNumber.length <= emergency.length + 4)
        }
        if (isEmergency) {
            Log.e(tag, "EMERGENCY SOS NUMBER DETECTED ($currentCaller). Bypassing AI processing completely to guarantee zero latency.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Wake‑lock to keep CPU alive during recording
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CyberGuard:AudioWake").apply { acquire(10 * 60 * 1000L) }

        // Load AI pipeline asynchronously
        serviceScope.launch {
            try {
                // Check DoT Blacklist before anything else
                val db = ScamDatabase.getDatabase(this@ScamDetectionService)
                val dotScammer = db.dotScammerDao().getScammer(PhoneNumberUtils.normalize(currentCaller))
                if (dotScammer != null && dotScammer.severityScore >= 80) {
                    Log.w(tag, "DoT Blacklist hit for $currentCaller! Dropping call instantly.")
                    val dummyResult = RiskResult(
                        score = dotScammer.severityScore,
                        transcript = "Blocked by Department of Telecommunications Blacklist",
                        isRoboVoice = false,
                        hitWord = "DoT_Blacklist",
                        intents = IntentScores(100, 100, 100, 0, 0)
                    )
                    endCallAndNotify(dummyResult)
                    stopSelf()
                    return@launch
                }

                // Check Local Whitelist (Trusted Contact)
                val localContact = db.localContactDao().getContact(PhoneNumberUtils.normalize(currentCaller))
                if (localContact != null && localContact.isEmergencyGuardian) {
                    Log.w(tag, "Trusted Guardian Contact ($currentCaller). Bypassing AI processing.")
                    stopSelf()
                    return@launch
                }

                // Load ContactMemory from SQLite
                val numberHash = PhoneNumberUtils.hash(currentCaller)
                val memoryEntity = db.contactMemoryDao().getMemory(numberHash)
                val contactMemory = if (memoryEntity != null) ContactMemory.fromBytes(memoryEntity.memory) else ContactMemory()

                // Update basic call metrics
                contactMemory.totalCalls = (contactMemory.totalCalls + 1).coerceAtMost(255)

                val mgr = com.example.pipeline.PipelineSingleton.getInstance(this@ScamDetectionService)
                mgr.reset()

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

    private suspend fun isWhitelisted(number: String): Boolean {
        val normalized = PhoneNumberUtils.normalize(number)
        if (serverWhitelist.contains(normalized)) return true
        val settingsPrefs = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
        val localWhitelist = settingsPrefs.getStringSet("whitelist", null) ?: emptySet()
        return localWhitelist.any { PhoneNumberUtils.normalize(it) == normalized }
    }

    private suspend fun isTrusted(number: String): Boolean {
        val normalized = PhoneNumberUtils.normalize(number)
        val db = ScamDatabase.getDatabase(this)
        val contact = db.localContactDao().getContact(normalized)
        if (contact != null) return true
        val settingsPrefs = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
        val trustedContacts = settingsPrefs.getStringSet("trusted_contacts", null) ?: emptySet()
        return trustedContacts.contains(normalized)
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
            audioRecord = null
            // 1. The Audio Routing Fix (VOICE_COMMUNICATION)
            val source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            try {
                @SuppressLint("MissingPermission")
                val rec = AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord = rec
                    // Explicitly inject AcousticEchoCanceler and NoiseSuppressor
                    if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                        android.media.audiofx.AcousticEchoCanceler.create(rec.audioSessionId)?.enabled = true
                    }
                    if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                        android.media.audiofx.NoiseSuppressor.create(rec.audioSessionId)?.enabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e("TELECOM_DEBUG", "Failed to initialize AudioRecord with VOICE_COMMUNICATION: ${e.message}", e)
            }

            if (audioRecord == null) {
                Log.e(tag, "No usable audio source – aborting")
                _statusFlow.value = "MIC_UNAVAILABLE"
                isRecording = false
                return@Thread
            }
            // --- PIPELINE SAFETY CHECK ---
            if (pipelineManager == null) {
                Log.e("AudioLoop", "PipelineManager is null! Wait for 'Pipeline loaded successfully' before recording.")
                return@Thread
            }

            // --- 1. SET UP THE DEDICATED SPACE (Ring Buffer Object Pool) ---
            val secondsToRecord = 3
            val maxSamples = sampleRate * secondsToRecord // 48,000 samples

            val emptyBuffers = ConcurrentLinkedQueue<FloatArray>()
            emptyBuffers.add(FloatArray(maxSamples))
            emptyBuffers.add(FloatArray(maxSamples))
            emptyBuffers.add(FloatArray(maxSamples))

            val readyQueue = Channel<FloatArray>(Channel.UNLIMITED)

            var currentBuffer = emptyBuffers.poll() ?: FloatArray(maxSamples)
            var currentSampleCount = 0
            val tempShortBuffer = ShortArray(bufferSize)

            Log.d("AudioLoop", "Starting 3-second Fixed-Window Recording Loop with Zero-Alloc Queue")
            try {
                Log.e("AI_ENGINE", "MICROPHONE_TRAP: Requesting hardware access (VOICE_COMMUNICATION)...")
                audioRecord?.startRecording()
                Log.e("AI_ENGINE", "MICROPHONE_TRAP: Hardware access GRANTED. Timestamp: ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e("TELECOM_DEBUG", "startRecording() FAILED! Microphone access denied or route stolen: ${e.message}", e)
                _statusFlow.value = "MIC_UNAVAILABLE"
                isRecording = false
                return@Thread
            }
            _statusFlow.value = "RECORDING"


            // --- 2. CONSUMER COROUTINE (Dispatchers.Default) ---
            serviceScope.launch(Dispatchers.Default) {
                val sliceSize = 512
                val sliceBuffer = FloatArray(sliceSize) // Allocate ONCE

                for (chunkForAI in readyQueue) {
                    if (!isRecording) break

                    Log.e("AI_ENGINE", "INFERENCE_TRAP: Incoming buffer detected. Starting Whisper/ONNX processing...")
                    val startTime = System.currentTimeMillis()

                    val pipeline = pipelineManager
                    if (pipeline != null) {
                        var lastResult: com.example.models.RiskResult? = null

                        for (i in chunkForAI.indices step sliceSize) {
                            val end = minOf(i + sliceSize, chunkForAI.size)
                            val length = end - i

                            val slice = if (length == sliceSize) {
                                System.arraycopy(chunkForAI, i, sliceBuffer, 0, sliceSize)
                                sliceBuffer
                            } else {
                                chunkForAI.copyOfRange(i, end) // Minor edge case fallback
                            }

                            // ADPF Thermal Check
                            val mode = DeviceHealthManager.getOperationalMode(applicationContext)

                            when (mode) {
                                DeviceHealthManager.OperationalMode.NOMINAL -> {
                                    // Device is cool. Run full inference.
                                    lastResult = pipeline.processChunk(slice)
                                }
                                DeviceHealthManager.OperationalMode.THROTTLED -> {
                                    Log.w("ScamDetection", "THERMAL WARNING: Shedding NLP load.")
                                    // Skip heavy NLP processing, rely on VAD/ASR/Regex
                                    lastResult = pipeline.processChunk(slice, skipNlp = true)
                                }
                                DeviceHealthManager.OperationalMode.CRITICAL -> {
                                    Log.e("SAFETY", "Critical heat detected, AI shutdown")
                                    pipeline.close()
                                    _statusFlow.value = "AI_SUSPENDED"
                                    break 
                                }
                            }
                        }

                        val inferenceTime = System.currentTimeMillis() - startTime
                        Log.e("AI_ENGINE", "INFERENCE_TRAP: Inference COMPLETE in ${inferenceTime}ms. Score: ${lastResult?.score}")

                        Log.d("AudioLoop", "Finished processing 3-second block through VAD/ASR.")
                        val result = lastResult
                        if (result != null) {
                            _scoreFlow.value = result.score
                            _transcriptFlow.value = result.transcript
                            when {
                                result.isRoboVoice -> broadcastRoboWarning()
                                result.score >= 70 -> {
                                    endCallAndNotify(result)
                                    dispatchGuardianAlert(currentCaller)
                                }
                                isTrusted(currentCaller) -> { /* trusted */ }
                                isWhitelisted(currentCaller) -> { /* whitelisted */ }
                                else -> if (result.score > 40) broadcastRiskUpdate(result)
                            }
                        }
                    } else {
                        Log.e("AudioLoop", "PipelineManager became null during processing!")
                    }

                    // Return the processed buffer to the object pool to stop GC Thrashing
                    emptyBuffers.offer(chunkForAI)
                }
            }

            // --- 3. PRODUCER LOOP (I/O Thread) ---
            while (isRecording && !Thread.currentThread().isInterrupted) {
                val currentAudioRecord = audioRecord ?: break
                val readSize = try {
                    currentAudioRecord.read(tempShortBuffer, 0, tempShortBuffer.size)
                } catch (e: Exception) {
                    0
                }

                if (readSize > 0 && isRecording) {
                    for (i in 0 until readSize) {
                        if (currentSampleCount < maxSamples) {
                            currentBuffer[currentSampleCount] = tempShortBuffer[i] / 32768.0f
                            currentSampleCount++
                        }
                    }

                    if (currentSampleCount >= maxSamples) {
                        Log.d("AudioLoop", "3 Seconds of audio captured! Pushing to Channel Queue.")

                        // Push full buffer to consumer, pull empty one from pool (Zero Alloc)
                        readyQueue.trySend(currentBuffer)
                        currentBuffer = emptyBuffers.poll() ?: FloatArray(maxSamples)
                        currentSampleCount = 0
                    }
                }
            }
            readyQueue.close()
            // AudioRecord cleanup is handled exclusively by onDestroy() to prevent double-release
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
        Log.i(tag, "Service onDestroy() - Initiating asynchronous teardown.")

        isRecording = false
        serviceJob.cancel()

        val currentAudioRecord = audioRecord
        val currentWakeLock = wakeLock
        val appContext = applicationContext
        val currentPipeline = pipelineManager
        val caller = currentCaller
        val startTime = serviceStartTime

        // Safe Asynchronous Hardware & DB Teardown
        // We use GlobalScope here because the serviceScope is cancelled, and we MUST ensure
        // hardware release and DB logging complete.
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Background Teardown: Stopping and releasing AudioRecord...")
                currentAudioRecord?.stop()
                currentAudioRecord?.release()
                Log.d(tag, "Background Teardown: AudioRecord released.")
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: AudioRecord release failed", e)
            }

            // Save Call Log & Memory
            try {
                val finalResult = currentPipeline?.getLatestResult()
                val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val score = finalResult?.score ?: 0
                val settings = appContext.getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
                val threshold = settings.getInt("alert_threshold", 70)
                val isScam = score >= threshold

                val db = ScamDatabase.getDatabase(appContext)
                val log = com.example.models.CallLog(
                    callerNumber = caller,
                    timestamp = System.currentTimeMillis(),
                    riskScore = score,
                    isScam = isScam,
                    transcript = finalResult?.transcript ?: "",
                    hitKeywords = finalResult?.hitWord ?: "",
                    durationSeconds = durationSeconds,
                    wasBlocked = isScam
                )
                db.callLogDao().insertLog(log)

                currentPipeline?.contactMemory?.let { memory ->
                    val numberHash = PhoneNumberUtils.hash(caller)
                    db.contactMemoryDao().saveMemory(ContactMemoryEntity(numberHash, memory.toBytes(), System.currentTimeMillis()))
                }
                Log.d(tag, "Background Teardown: Call logs and memory saved.")
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: Database logging failed", e)
            }

            // Release WakeLock
            try {
                if (currentWakeLock?.isHeld == true) {
                    currentWakeLock.release()
                    Log.d(tag, "Background Teardown: WakeLock released.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: WakeLock release failed", e)
            }

            // Clear ONNX/Singleton
            com.example.pipeline.PipelineSingleton.clear()
            Log.d(tag, "Background Teardown: Pipeline cleared.")
        }

        // Notify UI immediately on Main Thread
        com.example.utils.CallStateBroadcaster.endCall()

        super.onDestroy()
    }
}
