/**
 * ASR (Automatic Speech Recognition) - Stage 2
 * LEFT NODE (Input): VAD-approved PCM 16-bit audio frames.
 * RIGHT NODE (Output): String (Raw transcribed text in Hindi/English).
 * PURPOSE: Transcribes human speech to text using Sherpa-ONNX Fast Conformer.
 */
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

class StreamingASR(private val context: Context) {
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

            // 1. Decrypt the model to a secure directory
            val decryptedModelFile = com.example.security.ModelCryptoManager.decryptModelToCache(
                context,
                "$modelDirName/model.int8.onnx",
                "model.int8.onnx"
            )

            // 2. Copy tokens.txt to the same directory
            val secureDir = java.io.File(context.noBackupFilesDir, "secure_models")
            val tokensFile = java.io.File(secureDir, "tokens.txt")
            if (!tokensFile.exists()) {
                context.assets.open("$modelDirName/tokens.txt").use { input ->
                    tokensFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val config = getOnlineRecognizerConfig(
                modelDir = secureDir.absolutePath,
                type = modelType,
                numThreads = 2,
            )
            config.modelConfig.provider = "nnapi"

            // Initialize engine
            recognizer = OnlineRecognizer(assetManager = null, config = config)
            stream = recognizer?.createStream()
            Log.d(tag, "Sherpa-ONNX Engine Loaded Successfully!")

            // MANDATE: IP Protection (V1.1_Updates Section 1)
            // Immediately purge the decrypted weights from disk after loading into RAM.
            // This closes the window for extraction on rooted devices.
            com.example.security.ModelCryptoManager.purgeModelCache(context)

        } catch (e: Exception) {
            Log.e(tag, "Failed to load Sherpa-ONNX: ${e.message}")
        }
    }



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

    fun close() {
        stream?.release()
        try {
            recognizer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Failed to release recognizer: ${e.message}")
        }
        // Final sweep for any remaining cache
        com.example.security.ModelCryptoManager.purgeModelCache(context)
    }
}
