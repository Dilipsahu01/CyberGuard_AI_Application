package com.example.pipeline

/**
 * PipelineManager.kt
 *
 * PURPOSE:
 * This is the "Master Conductor" of the CyberGuard AI architecture.
 * It manages the real-time audio chunking and routes the data through the entire
 * AI pipeline (VAD -> Deepfake Check -> ASR -> NLP -> Regex -> ArcTracker -> Ensemble).
 *
 * WHY IT EXISTS:
 * To provide a clean, unified interface for the Android Background Service. Instead of
 * the Service managing 7 different AI models, it just calls `pipelineManager.processChunk()`.
 */
import android.content.Context
import android.os.PowerManager
import android.os.Build
import android.util.Log
import com.example.models.IntentScores
import com.example.models.RiskResult
import com.example.models.ContactMemory

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PipelineManager(context: Context) {
    private val TAG = "PipelineManager"
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val bloomFilter = BloomFilter()
    val vad: SileroVAD
    val asr: StreamingASR
    @Volatile var nlp: IntentNLP? = null
    
    init {
        val (v, a) = runBlocking(Dispatchers.IO) {
            val vDef = async { SileroVAD(context) }
            val aDef = async { StreamingASR(context) }
            
            // Background load NLP (Staged Boot)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    nlp = IntentNLP(context)
                    Log.i(TAG, "Background NLP load complete!")
                } catch (e: Exception) {
                    Log.e(TAG, "Background NLP load failed: ${e.message}")
                }
            }
            Pair(vDef.await(), aDef.await())
        }
        vad = v
        asr = a
    }

    private val regexGate = RegexGate()
    private val ensemble = EnsembleEngine()
    private val arcTracker = ArcTracker()
    var contactMemory: ContactMemory? = null
    var callContext: com.example.models.CallContext? = null

    private var transcript = StringBuilder()
    private var turnCount = 0
    private var chunkIndex = 0
    private var currentScore = 0
    var hitWord = ""
    private var lastIntents = IntentScores()
    private var lastLlmRunTurn = 0



    fun isScamCallerNumber(number: String): Boolean {
        return bloomFilter.check(number)
    }

    private var latestResult: RiskResult? = null

    fun getLatestResult(): RiskResult? = latestResult

    private val floatBuf = FloatArray(8192) // Class-level pre-allocated buffer

    fun processAudioChunk(chunk: ShortArray) {
        val size = chunk.size
        for (i in 0 until size) {
            floatBuf[i] = chunk[i] / 32768f
        }
        latestResult = processChunk(floatBuf.copyOf(size)) // We can further optimize by passing size to processChunk if we change its signature
    }

    fun processChunk(audio: FloatArray, skipNlp: Boolean = false): RiskResult {
        Log.d(TAG, "Received float chunk of size ${audio.size}. Starting pipeline processing. (skipNlp=$skipNlp)")
        chunkIndex++

        // Stage 1: VAD Speech Gating and Utterance State Tracking
        val isSpeech = vad.isSpeech(audio)
        val utteranceEnded = vad.utteranceEnded

        // Stage 1.5: Deepfake / Robo-Voice Acoustic Detection Stub
        val isRoboVoice = detectRoboVoice(audio)

        // Stage 2: ASR — only process audio when VAD confirms human speech is present.
        // This saves ~70% CPU by skipping the 132MB Sherpa-ONNX model during silence.
        var newText = ""
        if (isSpeech) {
            newText = asr.processChunk(audio)
            if (newText.isNotEmpty()) {
                if (transcript.isNotEmpty()) {
                    transcript.append(" ")
                }
                transcript.append(newText)
                turnCount++ // Increment speech turn count
                arcTracker.update(newText)
            }
        }

        // Stage 3: Regex keyword gate scan (on new text delta only)
        val (regexScore, matchingKeywords) = regexGate.check(newText)
        if (matchingKeywords.isNotEmpty()) {
            hitWord = matchingKeywords
            val hitLower = hitWord.lowercase()
            contactMemory?.let { mem ->
                if ("otp" in hitLower || "pin" in hitLower || "cvv" in hitLower || "password" in hitLower) mem.askedOtp = true
                if ("secret" in hitLower || "private" in hitLower || "don't tell anyone" in hitLower) mem.secrecyAsked = true
            }
        }

        // Stage 4: Semantic Context Analysis (ADPF THERMAL API + VAD GATING)

        // Mandate 1: Fast-Talker Exploit Check
        val isFastTalker = (turnCount - lastLlmRunTurn) >= 5
        val shouldWakeLlm = regexScore > 0 || utteranceEnded || isFastTalker

        // Mandate 2: ADPF Thermal API Check (Dynamic Precision Scaling)
        var thermalThrottling = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val headroom = powerManager.getThermalHeadroom(0)
            if (headroom >= 0.85f) {
                thermalThrottling = true
                Log.w(TAG, "ADPF THERMAL CRITICAL (Headroom: $headroom)! Degrading pipeline to Regex-only mode.")
            } else if (headroom >= 0.70f) {
                Log.i(TAG, "ADPF THERMAL WARNING (Headroom: $headroom). Skipping non-critical IntentNLP analysis.")
                // Set flag to skip heavy NLP but keep ASR active
            }
        }

        // Adaptive Orchestration: Only wake the LLM if silicon is cool enough.
        // If headroom > 0.70, we gracefully skip IntentNLP to save battery/reduce heat.
        val skipNLP = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager.getThermalHeadroom(0) > 0.70f
        } else false

        if (shouldWakeLlm && !thermalThrottling && !skipNLP && newText.isNotEmpty() && transcript.isNotEmpty()) {
            val slidingWindowText = getLastNWords(transcript.toString(), 100)
            
            val localNlp = nlp
            if (localNlp != null) {
                lastIntents = localNlp.analyze(slidingWindowText)
                lastLlmRunTurn = turnCount
                
                // Mutate Contact Memory Bits based on latest semantic context
                contactMemory?.let { mem ->
                    if (lastIntents.financial > 40) mem.askedMoney = true
                    if (lastIntents.urgency > 60) mem.urgencyUsed = true
                    
                    val currentIntimacy = (lastIntents.intimacy / 14).coerceIn(0, 7)
                    if (currentIntimacy > mem.intimacyLevel) mem.intimacyLevel = currentIntimacy
                    
                    val currentEmotion = (lastIntents.urgency / 14).coerceIn(0, 7)
                    if (currentEmotion > mem.emotionalIntensity) mem.emotionalIntensity = currentEmotion
                }
            } else {
                Log.i(TAG, "NLP model still loading in background. Text safely buffered.")
            }
        }

        // Stage 5: Ensemble blending
        val romanceScore = contactMemory?.computeRomanceScore() ?: 0
        val baseScore = ensemble.calculate(regexScore, lastIntents, arcTracker.arcScore, romanceScore, callContext)

        // *** BASELINE SUSPICION: same robustness as the Python web app ***
        // 5 pts per speech turn, capped at 25 pts. Ensures risk bars visibly move early on.
        val baselineSuspicion = (turnCount * 5).coerceAtMost(25)
        val finalScore = Math.min(baseScore + baselineSuspicion, 100)

        currentScore = finalScore

        Log.d(TAG, "Chunk #$chunkIndex | Turn: $turnCount | Base: $baseScore | Final: $finalScore | UtteranceEnded: $utteranceEnded")

        return RiskResult(
            score = finalScore,
            transcript = transcript.toString(),
            regexScore = regexScore,
            hitWord = hitWord,
            intents = lastIntents,
            stage = if (utteranceEnded) "ENSEMBLE_NLP_UPDATE" else "ENSEMBLE",
            isRoboVoice = isRoboVoice
        )
    }

    private fun detectRoboVoice(audio: FloatArray): Boolean {
        // Placeholder for acoustic deepfake / synthetic voice detection
        // Returns false in prototype unless activated by a simulation flag
        return false
    }

    fun reset() {
        transcript.clear()
        chunkIndex = 0
        turnCount = 0
        currentScore = 0
        hitWord = ""
        lastIntents = IntentScores()
        asr.reset()
        vad.reset()
        regexGate.reset()
        arcTracker.reset()
    }

    private val isClosed = java.util.concurrent.atomic.AtomicBoolean(false)

    @Synchronized
    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            vad.close()
            nlp?.close()
            asr.close()
        }
    }

    private fun getLastNWords(text: String, n: Int = 100): String {
        if (text.length <= 500) return text
        var spaceCount = 0
        var index = text.length - 1
        while (index >= 0) {
            if (text[index] == ' ') {
                spaceCount++
                if (spaceCount >= n) {
                    return text.substring(index + 1)
                }
            }
            index--
        }
        return text
    }
}
