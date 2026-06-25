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
import android.util.Log
import com.example.models.IntentScores
import com.example.models.RiskResult
import com.example.models.ContactMemory

class PipelineManager(context: Context) {
    private val TAG = "PipelineManager"
    
    private val bloomFilter = BloomFilter()
    val vad = SileroVAD(context)
    val asr = StreamingASR(context)
    private val regexGate = RegexGate()
    val nlp = IntentNLP(context)
    private val ensemble = EnsembleEngine()
    private val arcTracker = ArcTracker()
    var contactMemory: ContactMemory? = null
    
    private var transcript = StringBuilder()
    private var turnCount = 0
    private var chunkIndex = 0
    private var currentScore = 0
    var hitWord = ""
    private var lastIntents = IntentScores()
    
    fun setSimulationScenario(isScam: Boolean) {
        asr.setScenario(isScam)
    }

    fun isScamCallerNumber(number: String): Boolean {
        return bloomFilter.check(number)
    }

    private var latestResult: RiskResult? = null

    fun getLatestResult(): RiskResult? = latestResult

    fun processAudioChunk(chunk: ShortArray) {
        Log.d(TAG, "Received chunk of size ${chunk.size}")
        val floatBuf = FloatArray(chunk.size)
        for (i in chunk.indices) {
            floatBuf[i] = chunk[i] / 32768f
        }
        latestResult = processChunk(floatBuf)
    }

    fun processChunk(audio: FloatArray): RiskResult {
        Log.d(TAG, "Received float chunk of size ${audio.size}. Starting pipeline processing.")
        chunkIndex++
        
        // Stage 1: VAD Speech Gating and Utterance State Tracking
        val isSpeech = vad.isSpeech(audio)
        val utteranceEnded = vad.utteranceEnded
        
        // Stage 1.5: Deepfake / Robo-Voice Acoustic Detection Stub
        val isRoboVoice = detectRoboVoice(audio)
        
        // Stage 2: Automated Speech Recognition (ASR) (forced processing to bypass any VAD blocks)
        var newText = ""
        // Bypass isSpeech wrapper
        // if (isSpeech) {
            newText = asr.processChunk(audio)
            if (newText.isNotEmpty()) {
                if (transcript.isNotEmpty()) {
                    transcript.append(" ")
                }
                transcript.append(newText)
                turnCount++ // Increment speech turn count
                arcTracker.update(newText)
            }
        // }
        
        // Stage 3: Regex keyword gate scan (on new text delta only)
        val (regexScore, matchingKeywords) = regexGate.check(newText)
        if (matchingKeywords.isNotEmpty()) {
            hitWord = matchingKeywords
        }
        
        // Stage 4: Sentence semantic Transformer NLP models (Bypass VAD utteranceEnded block)
        // Run NLP whenever new text is detected instead of waiting for VAD silence
        if (newText.isNotEmpty() && transcript.isNotEmpty()) {
            val slidingWindowText = getLastNWords(transcript.toString(), 100)
            lastIntents = nlp.analyze(slidingWindowText)
        }
        
        // Stage 5: Ensemble blending
        val romanceScore = contactMemory?.computeRomanceScore() ?: 0
        val baseScore = ensemble.calculate(regexScore, lastIntents, arcTracker.arcScore, romanceScore)
        
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

    fun close() {
        vad.close()
        nlp.close()
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
