package com.example.pipeline

/**
 * StreamingASR.kt
 * 
 * PURPOSE: 
 * Wraps the Sherpa-ONNX Fast Conformer model for local Speech-to-Text.
 * 
 * WHY IT EXISTS:
 * Processes raw 16kHz audio waves into text strings (Hinglish supported) in real-time. 
 * This enables privacy-first transcription without ever sending the user's voice to a cloud API.
 */
import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.getOnlineRecognizerConfig

class StreamingASR(context: Context) {
    private val tag = "StreamingASR"
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var fullTranscript = ""
    private var lastProcessedLength = 0

    init {
        Log.d(tag, "Initializing REAL Sherpa-ONNX ASR Engine...")
        try {
            val assetManager = context.assets
            val assetList = assetManager.list("") ?: emptyArray()
            val modelDirName = assetList.find { it.startsWith("sherpa-onnx") }
                ?: "sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8"
            
            val modelType = when {
                modelDirName.contains("whisper", ignoreCase = true) -> "whisper"
                modelDirName.contains("zipformer", ignoreCase = true) -> "zipformer2"
                else -> "nemo_ctc"
            }
            
            Log.i(tag, "Dynamic ASR model discovery selected: $modelDirName (Type: $modelType)")
            
            val config = getOnlineRecognizerConfig(
                modelDir = modelDirName,
                type = modelType,
                numThreads = 2,
            )
            recognizer = OnlineRecognizer(context.assets, config)
            stream = recognizer?.createStream()
            Log.d(tag, "Sherpa-ONNX Engine Loaded Successfully!")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load Sherpa-ONNX: ${e.message}")
        }
    }

    // Ignore the demo parameter now, we are doing real inference!
    fun setScenario(@Suppress("UNUSED_PARAMETER") isScam: Boolean) {}

    fun processChunk(audio: FloatArray): String {
        val recognizerLocal = recognizer ?: return ""
        val streamLocal = stream ?: return ""

        // Feed the raw PCM audio from the InCallService straight into the C++ engine!
        streamLocal.acceptWaveform(audio, sampleRate = 16000)

        while (recognizerLocal.isReady(streamLocal)) {
            recognizerLocal.decode(streamLocal)
        }

        val accumulatedText = recognizerLocal.getResult(streamLocal).text
        
        var delta = ""
        val currentLen = accumulatedText.length
        if (currentLen > lastProcessedLength) {
            delta = accumulatedText.substring(lastProcessedLength, currentLen).trim()
            lastProcessedLength = currentLen
            fullTranscript = accumulatedText
        }
        
        return delta
    }

    fun reset() {
        stream?.release()
        stream = recognizer?.createStream()
        fullTranscript = ""
        lastProcessedLength = 0
    }
}
