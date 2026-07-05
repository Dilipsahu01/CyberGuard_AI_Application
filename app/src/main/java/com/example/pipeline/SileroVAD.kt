/**
 * VAD (Voice Activity Detection) - Stage 1
 * LEFT NODE (Input): Raw PCM 16-bit audio stream (from Mic/Telephony).
 * RIGHT NODE (Output): Boolean (Speech Active) & Float (Probability 0.0 - 1.0).
 * PURPOSE: Filters out silence to save battery before passing to ASR.
 */
package com.example.pipeline

/**
 * SileroVAD.kt
 * 
 * PURPOSE: 
 * Voice Activity Detection (VAD) model wrapper.
 * 
 * WHY IT EXISTS:
 * Evaluates 100ms audio chunks to detect if a human is speaking. If there is silence, 
 * it prevents the massive ASR and NLP models from running, saving immense amounts 
 * of battery life on budget phones.
 */
import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

class SileroVAD(context: Context) {
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isModelLoaded = false

    // State tensor for RNN
    private var vadState: Array<Array<FloatArray>> = Array(2) { Array(1) { FloatArray(128) } }

    // Trailing silence and utterance state tracking
    private var speechActive = false
    private var consecutiveSilenceChunks = 0
    var utteranceEnded = false
        private set

    init {
        try {
            env = OrtEnvironment.getEnvironment()
            val bytes = getModelBytes(context)
            if (bytes != null) {
                session = env?.createSession(bytes)
                isModelLoaded = true
                Log.d(TAG, "Silero VAD ONNX successfully loaded via byte array!")
                Log.d(TAG, "Silero VAD expected inputs: ${session?.inputNames}")
            } else {
                Log.w(TAG, "Silero VAD ONNX model bytes were null. Fallback activated.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ONNX failed to load Silero VAD, utilizing dynamic acoustic-energy fallback: ${e.message}")
        }
    }

    private fun evaluateSpeech(audioChunk: FloatArray): Boolean {
        if ((!isModelLoaded) || (env == null) || (session == null)) {
            // High-fidelity fallback: Signal Energy Root Mean Square (RMS) speech detection
            var sum = 0f
            for (sample in audioChunk) {
                sum += sample * sample
            }
            val rms = kotlin.math.sqrt((sum / audioChunk.size).toDouble())
            // Dynamic voice threshold
            return rms > 0.012
        }

        return try {
            val envLocal = env ?: return true
            val sessionLocal = session ?: return true

            // Create input tensor float32[1, 1600]
            val inputShape = longArrayOf(1, audioChunk.size.toLong())
            val audioBuffer = FloatBuffer.wrap(audioChunk)
            val inputTensor = OnnxTensor.createTensor(envLocal, audioBuffer, inputShape)

            // Sr input (sample rate) as int64[1]
            val srBuffer = LongBuffer.wrap(longArrayOf(16000))
            val srTensor = OnnxTensor.createTensor(envLocal, srBuffer, longArrayOf(1))

            val stateTensor = OnnxTensor.createTensor(envLocal, vadState)

            // Map inputs
            val inputs = mapOf(
                "input" to inputTensor,
                "sr" to srTensor,
                "state" to stateTensor
            )

            val outputs = sessionLocal.run(inputs)
            val outputTensor = outputs[0] as OnnxTensor
            @Suppress("UNCHECKED_CAST")
            val outputFloat = ((outputTensor.value as? Array<FloatArray>)?.get(0)?.get(0)) ?: 0f

            val nextState = outputs[1] as OnnxTensor
            @Suppress("UNCHECKED_CAST")
            val nextStateVal = nextState.value as? Array<Array<FloatArray>>
            if (nextStateVal != null) vadState = nextStateVal

            outputs.close()
            inputTensor.close()
            srTensor.close()
            stateTensor.close()

            // Return speech probability
            outputFloat > 0.5f

        } catch (e: Exception) {
            Log.e(TAG, "VAD Inference exception: ${e.message}, returning fallback.")
            // Sound energy backup on inference failure
            var sum = 0f
            for (sample in audioChunk) {
                sum += sample * sample
            }
            val rms = kotlin.math.sqrt((sum / audioChunk.size).toDouble())
            rms > 0.015
        }
    }

    fun isSpeech(audioChunk: FloatArray): Boolean {
        val currentIsSpeech = evaluateSpeech(audioChunk)
        
        if (currentIsSpeech) {
            speechActive = true
            consecutiveSilenceChunks = 0
            utteranceEnded = false
        } else {
            if (speechActive) {
                consecutiveSilenceChunks++
                if (consecutiveSilenceChunks >= 4) { // ~400ms of consecutive silence signals end of active utterance
                    speechActive = false
                    utteranceEnded = true
                } else {
                    utteranceEnded = false
                }
            } else {
                utteranceEnded = false
            }
        }
        return currentIsSpeech
    }

    fun reset() {
        vadState = Array(2) { Array(1) { FloatArray(128) } }
        speechActive = false
        consecutiveSilenceChunks = 0
        utteranceEnded = false
    }

    fun close() {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing SileroVAD: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SileroVAD"
        @Volatile private var modelBytes: ByteArray? = null
        private val lock = Any()

        private fun getModelBytes(context: Context): ByteArray? {
            synchronized(lock) {
                if (modelBytes == null) {
                    try {
                        modelBytes = context.assets.open("models/silero_vad.ort").use { it.readBytes() }
                        Log.i(TAG, "Loaded silero_vad.ort into shared static memory.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load silero_vad.ort: ${e.message}")
                    }
                }
                return modelBytes
            }
        }
    }
}
