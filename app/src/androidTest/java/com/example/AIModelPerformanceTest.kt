package com.example

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pipeline.PipelineManager
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AIModelPerformanceTest {

    @Test
    fun testAIModelWakeupAndInference() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        Log.i("PerformanceTest", "========================================")
        Log.i("PerformanceTest", "Starting Model Wakeup Test...")
        val startTime = System.currentTimeMillis()
        
        // Test Individual Model Load Times
        val vadStartTime = System.currentTimeMillis()
        val vad = com.example.pipeline.SileroVAD(appContext)
        val vadLoadTime = System.currentTimeMillis() - vadStartTime
        Log.i("PerformanceTest", "VAD Initialization Time: $vadLoadTime ms")

        val asrStartTime = System.currentTimeMillis()
        val asr = com.example.pipeline.StreamingASR(appContext)
        val asrLoadTime = System.currentTimeMillis() - asrStartTime
        Log.i("PerformanceTest", "ASR (Sherpa ONNX) Initialization Time: $asrLoadTime ms")

        val nlpStartTime = System.currentTimeMillis()
        val nlp = com.example.pipeline.IntentNLP(appContext)
        val nlpLoadTime = System.currentTimeMillis() - nlpStartTime
        Log.i("PerformanceTest", "NLP (MiniLM) Initialization Time: $nlpLoadTime ms")
        
        val pmStartTime = System.currentTimeMillis()
        val pipelineManager = PipelineManager(appContext) // For the old test below
        val loadTime = System.currentTimeMillis() - pmStartTime
        Log.i("PerformanceTest", "Total Pipeline Manager Initialization: $loadTime ms")
        Log.i("PerformanceTest", "========================================")
        
        // Test Sherpa ONNX Inference Time (1 Second of Audio)
        val dummyAudio = FloatArray(16000) { 0f } // 1 second of silence at 16kHz
        Log.i("PerformanceTest", "Testing Sherpa-ONNX Inference (Processing 1 second of audio)...")
        val asrInferenceStartTime = System.currentTimeMillis()
        asr.processChunk(dummyAudio)
        val asrInferenceTime = System.currentTimeMillis() - asrInferenceStartTime
        Log.i("PerformanceTest", "ASR (Sherpa ONNX) Inference Time: $asrInferenceTime ms")
        Log.i("PerformanceTest", "========================================")
        
        // Test NLP Inference
        val testString = "Sir your bank account has been blocked due to suspicious activity. Please tell me your OTP to unblock it immediately."
        Log.i("PerformanceTest", "Testing Voice/NLP Inference with string: '$testString'")
        
        val inferenceStartTime = System.currentTimeMillis()
        val scores = pipelineManager.nlp.analyze(testString)
        val inferenceTime = System.currentTimeMillis() - inferenceStartTime
        
        Log.i("PerformanceTest", "NLP Inference Time: $inferenceTime ms")
        Log.i("PerformanceTest", "Financial Probability: ${scores.financial}")
        Log.i("PerformanceTest", "Urgency Probability: ${scores.urgency}")
        Log.i("PerformanceTest", "========================================")
        
        assert(loadTime > 0)
        assert(inferenceTime > 0)
    }
}
