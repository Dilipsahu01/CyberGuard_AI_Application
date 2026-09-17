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
import com.example.database.AppDatabase
import com.example.database.ScamCallEntity
import com.example.database.ScamRepository
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
import com.example.utils.Constants
import com.example.utils.PhoneNumberUtils
import com.example.utils.TranscriptScrubber
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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
import kotlinx.coroutines.channels.BufferOverflow

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
    private var currentDirection = 0

    // Pre-allocated inference buffers (Memory Safety Audit)
    private val sliceBuffer = FloatArray(512)
    private val tempShortBuffer = ShortArray(8192)

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceStartTime = 0L
    private var lastTelemetryTime = 0L

    private val serverWhitelist = mutableSetOf<String>()

    private lateinit var swarmReporter: SwarmReporter

    // SharedPreferences for theme toggle
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    // ---------- OkHttp client ----------
    private val httpClient by lazy { SwarmReporter.getHttpClient() }
    // Native Obfuscation (V1.1_Updates Section 2): URL is fetched from encrypted C++ memory
    private val serverBaseUrl by lazy {
        try {
            com.example.security.NativeSecrets.getServerBaseUrl()
        } catch (t: Throwable) {
            "http://10.0.2.2:8080/api/v1" // Fallback for local JVM tests
        }
    }

    // ---------- Service lifecycle ----------
    override fun onBind(intent: Intent?): IBinder = LocalBinder()
    inner class LocalBinder : Binder() { fun getService() = this@ScamDetectionService }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize Swarm Reporter (V1.1 Secure Binary Telemetry)
        swarmReporter = SwarmReporter(this)

        // Pull latest whitelist from server (fire‑and‑forget)
        serviceScope.launch(Dispatchers.IO) { syncWhitelistFromServer() }
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
            _statusFlow.value = "COMPROMISED"
            com.example.utils.CallStateBroadcaster.updateTelemetry(com.example.utils.TelemetryUpdate(
                score = 0, transcript = "SYSTEM COMPROMISED. AI OFFLINE.", hitWord = "", callerNumber = currentCaller, stage = "COMPROMISED",
                urgency = 0, financial = 0, coercion = 0, intimacy = 0, trust = 0, isScamScenario = false
            ))
            // Keep service alive so UI can display this state, but don't boot AI.
            return START_STICKY
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
        currentDirection = intent?.getIntExtra("call_direction", 0) ?: 0

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
                    Log.w(tag, "DoT Blacklist hit for $currentCaller! Updating UI.")
                    val dummyResult = RiskResult(
                        score = dotScammer.severityScore,
                        transcript = "Blocked by Department of Telecommunications Blacklist",
                        isRoboVoice = false,
                        hitWord = "DoT_Blacklist",
                        intents = IntentScores(100, 100, 100, 0, 0)
                    )
                    broadcastRiskUpdate(dummyResult)
                    // We don't stop the call autonomously anymore, let the user decide.
                    // But we can stop the AI pipeline since we already know it's a scam.
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
                val numberEncrypted = PhoneNumberUtils.encrypt(currentCaller)
                val memoryEntity = db.contactMemoryDao().getMemory(numberEncrypted)
                val contactMemory = if (memoryEntity != null) ContactMemory.fromBytes(memoryEntity.memory) else ContactMemory()

                // Update basic call metrics
                contactMemory.totalCalls = (contactMemory.totalCalls + 1).coerceAtMost(255)

                val mgr = com.example.pipeline.PipelineSingleton.getInstance(this@ScamDetectionService)
                mgr.reset()

                // Generate telemetry CallContext (used for scoring and Swarm reporting)
                val notInContacts = (localContact == null)
                val rapidCallback = (contactMemory.totalCalls > 1 && contactMemory.daysKnown == 0) // Basic heuristic
                val callContext = com.example.models.CallContext(
                    totalCalls = contactMemory.totalCalls,
                    daysKnown = contactMemory.daysKnown,
                    notInContacts = notInContacts,
                    rapidCallback = rapidCallback,
                    isVoip = false // Placeholder for demo
                )

                mgr.contactMemory = contactMemory
                mgr.callContext = callContext
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

    // ---------- Telemetry removal (Insecure JSON Path) ----------
    // DELETED: pushTelemetry, enqueueTelemetry, processQueuePeriodically, flushQueue, hashString, isNetworkAvailable, sendLoRaPayload
    // These were removed to enforce binary-only telemetry and prevent PII leaks.

    // ---------- Theme toggle (SharedPreferences) ----------
    fun isDarkModeEnabled(): Boolean = prefs.getBoolean("dark_mode", true)

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        // Apply theme immediately (requires activity recreation)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    // ---------- LoRa fallback payload (DELETED) ----------

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

            val readyQueue = Channel<FloatArray>(capacity = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)

            var currentBuffer = emptyBuffers.poll() ?: FloatArray(maxSamples)
            var currentSampleCount = 0

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

                try {
                    for (chunkForAI in readyQueue) {
                        try {
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
                                        // Zero-allocation padded copy rather than copyOfRange
                                        sliceBuffer.fill(0f)
                                        System.arraycopy(chunkForAI, i, sliceBuffer, 0, length)
                                        sliceBuffer
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
                                            // Break out to finally block to handle closure
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

                                    com.example.utils.CallStateBroadcaster.updateTelemetry(
                                        com.example.utils.TelemetryUpdate(
                                            score = result.score,
                                            transcript = result.transcript,
                                            hitWord = result.hitWord,
                                            callerNumber = currentCaller,
                                            stage = _statusFlow.value,
                                            urgency = result.intents.urgency,
                                            financial = result.intents.financial,
                                            coercion = result.intents.coercion,
                                            intimacy = result.intents.intimacy,
                                            trust = result.intents.trust,
                                            isScamScenario = result.score >= 70
                                        )
                                    )

                                    // Initialize DB Singleton lazily or use a pre-initialized one
                                    val appDb = AppDatabase.getDatabase(applicationContext)
                                    val scamRepo = ScamRepository.getInstance(appDb)

                                    val settings = getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
                                    val threshold = settings.getInt("alert_threshold", 70)

                                    when {
                                        result.isRoboVoice -> broadcastRoboWarning()
                                        result.score >= threshold -> {
                                            broadcastRiskUpdate(result) // Let UI turn red, but DO NOT drop call
                                            dispatchGuardianAlert(currentCaller)
                                        }
                                        isTrusted(currentCaller) -> { /* trusted */ }
                                        isWhitelisted(currentCaller) -> { /* whitelisted */ }
                                        else -> if (result.score > 40) broadcastRiskUpdate(result)
                                    }

                                    // Database Write (Fire-and-forget in IO)
                                    if (result.score >= 40) {
                                        serviceScope.launch(Dispatchers.IO) {
                                            try {
                                                scamRepo.insertScam(
                                                    ScamCallEntity(
                                                        phoneNumber = currentCaller,
                                                        threatScore = result.score.toFloat(),
                                                        isFlagged = result.score >= 70
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                Log.e("AudioLoop", "Failed to insert scam call into database: ${e.message}", e)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.e("AudioLoop", "PipelineManager became null during processing!")
                            }
                        } catch (e: Exception) {
                            Log.e("AudioLoop", "CRITICAL AI ERROR: Inference threw exception on this chunk: ${e.message}", e)
                        } finally {
                            // Guarantee return of processed buffer to the object pool to stop GC Thrashing
                            emptyBuffers.offer(chunkForAI)
                        }
                    }
                } finally {
                    pipelineManager?.close()
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
    // Removed endCallAndNotify to stop autonomous hangups.

    private fun broadcastRiskUpdate(risk: RiskResult) {
        val intent = Intent("com.example.ACTION_RISK_UPDATE").apply {
            putExtra("score", risk.score)
            putExtra("transcript", risk.transcript)
            putExtra("hit_word", risk.hitWord)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun broadcastRoboWarning() {
        Log.w(tag, "Acoustic Deepfake / Robo-voice detected! Triggering UI Overlay alert.")
        _statusFlow.value = "ROBO_WARNING"
        val intent = Intent("com.example.ACTION_ROBO_WARNING")
        intent.setPackage(packageName)
        sendBroadcast(intent)
        // In IncomingCallActivity, this will turn the screen light red and play an audio warning
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun dispatchGuardianAlert(targetNumber: String) {
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
                val message = "[CyberGuard Alert] A high-risk scam call from $targetNumber was just intercepted on this device."
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
        recordThread?.interrupt()
        
        // Save these references BEFORE cancelling the scope
        val currentAudioRecord = audioRecord
        val currentWakeLock = wakeLock
        val appContext = applicationContext
        val currentPipeline = pipelineManager
        val caller = currentCaller
        val startTime = serviceStartTime

        // Now cancel the scope so the launch block stops waiting
        serviceJob.cancel()
        serviceScope.cancel()

        // Safe Asynchronous Hardware & DB Teardown
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                currentAudioRecord?.stop()
                currentAudioRecord?.release()
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: AudioRecord release failed", e)
            }

            try {
                val finalResult = currentPipeline?.getLatestResult()
                val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val score = finalResult?.score ?: 0
                val settings = appContext.getSharedPreferences("cyberguard_settings", MODE_PRIVATE)
                val threshold = settings.getInt("alert_threshold", 70)
                val isScam = score >= threshold

                // MANDATE: Scrub transcript for PII before saving to local Room DB (DPDP Act)
                val rawTranscript = finalResult?.transcript ?: ""
                val scrubbedTranscript = TranscriptScrubber.scrub(rawTranscript)

                val db = ScamDatabase.getDatabase(appContext)
                // CallLog database insertion has been migrated to CyberGuardInCallService
                // to correctly capture missed and rejected calls.

                // TRIGGER: Secured Binary Telemetry (10.25-byte Swarm Payload)
                if (isScam) {
                    val context = currentPipeline?.callContext ?: CallContext(
                        totalCalls = (currentPipeline?.contactMemory?.totalCalls ?: 0),
                        daysKnown = (currentPipeline?.contactMemory?.daysKnown ?: 0)
                    )
                    // Launch in a job that isn't cancelled by service stop
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch(Dispatchers.IO) {
                        swarmReporter.reportScam(caller, context, finalResult ?: RiskResult(score = score), false)
                    }
                }

                currentPipeline?.contactMemory?.let { memory ->
                    val numberEncrypted = PhoneNumberUtils.encrypt(caller)
                    db.contactMemoryDao().saveMemory(ContactMemoryEntity(numberEncrypted, memory.toBytes(), System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: Database logging failed", e)
            }

            try {
                if (currentWakeLock?.isHeld == true) {
                    currentWakeLock.release()
                }
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: WakeLock release failed", e)
            }

            try {
                // Safely clear the singleton without crashing the GlobalScope if it fails
                com.example.pipeline.PipelineSingleton.clear()
            } catch (e: Exception) {
                Log.e(tag, "Background Teardown: PipelineSingleton clear failed", e)
            }
        }

        com.example.utils.CallStateBroadcaster.endCall()

        try {
            pipelineManager?.close()
            pipelineManager = null
        } catch (e: Exception) {
            Log.e("ScamDetectionService", "Error during native cleanup: ${e.message}")
        } finally {
            super.onDestroy()
        }
    }
}
