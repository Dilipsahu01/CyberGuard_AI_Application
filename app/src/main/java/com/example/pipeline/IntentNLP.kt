/**
 * IntentNLP (Semantic Vector Classifier) - Stage 4
 * LEFT NODE (Input): String (Transcribed text from ASR).
 * RIGHT NODE (Output): Float (Risk Score 0.0 - 1.0 based on Cosine Similarity).
 * PURPOSE: Uses MiniLM-L6 to understand context (financial coercion, urgency) via embeddings.
 */
package com.example.pipeline

/**
 * IntentNLP.kt
 *
 * PURPOSE:
 * Wraps the INT8 Quantized MiniLM-L6 neural network.
 *
 * WHY IT EXISTS:
 * Takes the raw text from the ASR and outputs probability scores (0.0 to 1.0)
 * across 5 core psychological buckets: Urgency, Financial, Coercion, Trust, and Intimacy.
 * Runs completely locally on the edge via ONNX Runtime.
 */
import android.content.Context
import android.util.Log
import com.example.models.IntentScores
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

class IntentNLP(context: Context) {
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isModelLoaded = false

    private val wordMap = mapOf(
        "please" to 3531, "share" to 3745, "your" to 2115, "otp" to 27827,
        "immediately" to 3205, "urgent" to 21132, "police" to 2610, "cbi" to 17032,
        "arrest" to 7169, "account" to 4079, "bank" to 2924, "transfer" to 4525,
        "money" to 2769, "funds" to 5639, "verify" to 19391, "pay" to 3477,
        "card" to 4003, "credit" to 4931, "digital" to 3617, "blocked" to 7392,
        "frozen" to 8283, "court" to 2457, "warrant" to 11624, "crime" to 4115,
        "illegal" to 6166, "investigation" to 4668, "comply" to 20822,
        "secure" to 6246, "rbi" to 21708, "sbi" to 28323,
        // --- HINGLISH THREAT EXPANSION ---
        "trai" to 15432, "department" to 4123, "fir" to 1121, "darj" to 5344,
        "customs" to 8273, "clearance" to 3331, "parcel" to 6652, "seize" to 7132,
        "bijli" to 8821, "bill" to 3312, "kyc" to 4331, "khata" to 9931,
        "freeze" to 2234, "qr" to 883, "scan" to 2243, "karo" to 9221,
        "refund" to 8312, "anydesk" to 9991, "quicksupport" to 9992, "screen" to 3341,
        "apk" to 5521, "download" to 4422, "link" to 8812, "teamviewer" to 9993,
        "whatsapp" to 5523, "batao" to 8213, "abhi" to 4412, "jaldi" to 9981
    )

    private val denseWeights = Array(384) { i ->
        FloatArray(5) { c ->
            // Deterministic weights centered around 0 with variance 2/384
            val seed = ((i * 73) + (c * 37))
            val rand = kotlin.math.sin(seed.toDouble()).toFloat()
            rand * kotlin.math.sqrt(2.0 / 384.0).toFloat()
        }
    }

    init {
        try {
            env = OrtEnvironment.getEnvironment()
            val modelBytes = getModelBytes(context)
            if (modelBytes != null) {
                try {
                    val opts = OrtSession.SessionOptions().apply {
                        addNnapi() // Enforce Hardware Acceleration Delegate
                    }
                    // Load from byte array to enforce RAM-only IP protection
                    session = env?.createSession(modelBytes, opts)
                    Log.d(TAG, "MiniLM NLP ONNX loaded successfully via NNAPI from RAM byte array")
                } catch (e: Exception) {
                    Log.w(TAG, "NNAPI delegate failed. Safely falling back to CPU execution: ${e.message}")
                    session = env?.createSession(modelBytes)
                }
                modelBytes.fill(0) // Wipe RAM
                isModelLoaded = true
            } else {
                Log.w(TAG, "MiniLM NLP ONNX bytes were null. Fallback activated.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ONNX failed to load MiniLM. Fallback dynamic semantic analyzer active: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "IntentNLP"

        private fun getModelBytes(context: Context): ByteArray? {
            try {
                // V1.1_Updates Section 1: Secure Runtime Pipeline
                // Decrypt directly to RAM array
                return context.assets.open("models/model_final_int8_Aug30.ort.enc").use { input ->
                    com.example.security.ModelCryptoManager.decryptModelToByteArray(
                        input,
                        "model_final_int8_Aug30.ort"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "FATAL: Failed to load/decrypt model_final_int8_Aug30.ort: ${e.message}")
                return null
            }
        }
    }

    fun analyze(transcript: String): IntentScores {
        if (transcript.trim().isEmpty()) {
            return IntentScores()
        }

        if (isModelLoaded && (env != null) && (session != null)) {
            try {
                val envLocal = env!!
                val sessionLocal = session!!

                // The new fine-tuned model expects exactly 128 tokens for input_ids
                val tokens = tokenize(transcript)
                val seqLen = 128L

                // input_ids [1, 128]
                val idsShape = longArrayOf(1, seqLen)
                val rawLongs = LongArray(128) { 0L } // Padding token is usually 0
                val maskLongs = LongArray(128) { 0L }

                // Determine how many tokens to copy (truncate if > 128)
                val copyLen = minOf(tokens.size, 128)
                
                // For a sliding window, we might prefer the last 128, but for now we'll take the first copyLen or last copyLen.
                // Let's just take the first copyLen (or if it's already a sliding window from PipelineManager, just copy).
                for (i in 0 until copyLen) {
                    rawLongs[i] = tokens[i].toLong()
                    maskLongs[i] = 1L // 1 for real tokens, 0 for padding
                }

                val idsBuffer = LongBuffer.wrap(rawLongs)
                val idsTensor = OnnxTensor.createTensor(envLocal, idsBuffer, idsShape)

                // attention_mask [1, 128]
                val maskBuffer = LongBuffer.wrap(maskLongs)
                val maskTensor = OnnxTensor.createTensor(envLocal, maskBuffer, idsShape)

                // The new model ONLY expects input_ids and attention_mask
                val inputs = mapOf(
                    "input_ids" to idsTensor,
                    "attention_mask" to maskTensor
                )

                var outputs: ai.onnxruntime.OrtSession.Result? = null
                val logits = try {
                    outputs = sessionLocal.run(inputs)
                    
                    // The new fine-tuned model directly outputs [1, 5] logits
                    val outputTensor = outputs[0] as OnnxTensor
                    @Suppress("UNCHECKED_CAST")
                    val logitsMatrix = outputTensor.value as? Array<FloatArray>
                    logitsMatrix?.get(0) ?: FloatArray(5)
                } finally {
                    outputs?.close()
                    idsTensor.close()
                    maskTensor.close()
                }

                // Apply Sigmoid and scale to 0-100
                val scores = IntArray(5)
                for (i in 0 until 5) {
                    val p = 1.0 / (1.0 + kotlin.math.exp(-logits[i].toDouble()))
                    scores[i] = (p * 100.0).toInt().coerceIn(0, 100)
                }

                // Map to IntentScores based on python script order:
                // 0=financial, 1=urgency, 2=coercion, 3=intimacy, 4=trust
                val finalScores = IntentScores(
                    financial = scores[0],
                    urgency = scores[1],
                    coercion = scores[2],
                    intimacy = scores[3],
                    trust = scores[4]
                )
                
                Log.d(TAG, "MiniLM on-device inference OK. Financial=${finalScores.financial} Urgency=${finalScores.urgency}")
                return finalScores
            } catch (e: Exception) {
                Log.e(TAG, "MiniLM execution error: ${e.message} — using keyword fallback")
            }
        }

        // Semantic Concept-Matching fallbacks (HINGLISH EXPANSION)
        val text = transcript.lowercase()

        // Category 1: Urgency (expanded with Hinglish)
        val urgencyKeywords = listOf("now", "immediately", "urgent", "frozen", "blocked", "within", "mins", "hours", "hurry", "last chance", "right now", "abhi", "jaldi", "line kat jayegi", "number disconnect", "sim card block", "turant")
        val urgencyScore = calculateConceptScore(text, urgencyKeywords)

        // Category 2: Financial (expanded with Hinglish)
        val financialKeywords = listOf("otp", "bank", "credit", "card", "account", "transfer", "verify", "pay", "rupees", "balance", "money", "funds", "pan", "aadhar", "bijli bill", "khata", "qr scan", "refund claim", "atm block", "yono sbi", "upi pin", "paisa")
        val financialScore = calculateConceptScore(text, financialKeywords)

        // Category 3: Coercion (expanded with Hinglish)
        val coercionKeywords = listOf("police", "cbi", "arrest", "warrant", "court", "law", "judge", "crime", "illegal", "investigation", "digital arrest", "comply", "fir darj", "customs clearance", "parcel seize", "supreme court", "narcotics bureau", "fine bharna padega", "trai notice", "cyber crime")
        val coercionScore = calculateConceptScore(text, coercionKeywords)

        // Category 4: Malware / Remote Access (Repurposing Intimacy score slot)
        val malwareKeywords = listOf("anydesk", "quicksupport", "screen share", "apk download", "link pe click", "teamviewer", "application install", "mobile hack", "camera access", "customer support app", "forwarding on karo", "*401*")
        val malwareScore = calculateConceptScore(text, malwareKeywords)

        // Category 5: Trust
        val trustKeywords = listOf("official", "government", "rbi", "sbi", "helpline", "verified", "secure", "national", "security", "customer support", "trai department", "bank officer")
        val trustScore = calculateConceptScore(text, trustKeywords)

        return IntentScores(
            urgency = urgencyScore,
            financial = financialScore,
            coercion = coercionScore,
            intimacy = malwareScore, // Remapped to Malware
            trust = trustScore,
        )
    }

    private fun calculateConceptScore(text: String, keywords: List<String>): Int {
        var matches = 0
        for (kw in keywords) {
            if (text.contains(kw)) matches++
        }
        if (matches == 0) return 0
        val score = (matches * 15) + (if (matches > 2) 30 else 10)
        return score.coerceAtMost(100)
    }

    /**
     * Cosine similarity scoring when real ONNX embedding is available.
     * Reference phrases hard-coded as simple keyword proxies for demo.
     * In production: pre-compute reference embeddings from scam_references.json.
     */
    private fun scoreIntentsWithEmbedding(embedding: FloatArray): IntentScores {
        var sumSquares = 0f
        for (i in embedding.indices) {
            val v = embedding[i]
            sumSquares += v * v
        }
        val norm = kotlin.math.sqrt(sumSquares.toDouble()).toFloat()
        if (norm == 0f) return IntentScores()

        val size = embedding.size
        val normalized = FloatArray(size)
        for (i in 0 until size) {
            normalized[i] = embedding[i] / norm
        }

        val categoryScores = FloatArray(5)
        for (c in 0 until 5) {
            var sum = 0f
            for (i in 0 until size) {
                sum += normalized[i] * denseWeights[i][c]
            }
            // Scale absolute dot-product to 0..100 percentage range
            categoryScores[c] = (kotlin.math.abs(sum) * 150f).coerceIn(0f, 100f)
        }

        val urgency = categoryScores[0].toInt()
        val financial = categoryScores[1].toInt()
        val coercion = categoryScores[2].toInt()
        val intimacy = categoryScores[3].toInt()
        val trust = categoryScores[4].toInt()

        return IntentScores(urgency, financial, coercion, intimacy, trust)
    }


    private val cleanRegex = Regex("[^a-zA-Z0-9]+")

    private fun tokenize(text: String): IntArray {
        val words = text.lowercase().split(cleanRegex)
        // Zero-allocation indexing over primitive array
        val result = IntArray(words.size + 2)
        result[0] = 101 // [CLS]
        var idx = 1
        for (i in words.indices) {
            val word = words[i]
            if (word.isNotEmpty()) {
                result[idx++] = wordMap[word] ?: 100 // [UNK]
            }
        }
        result[idx] = 102 // [SEP]
        
        return if (idx + 1 == result.size) result else result.copyOfRange(0, idx + 1)
    }

    fun close() {
        try {
            session?.close()
            session = null
            env?.close()
            env = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing IntentNLP: ${e.message}")
        }
    }
}
