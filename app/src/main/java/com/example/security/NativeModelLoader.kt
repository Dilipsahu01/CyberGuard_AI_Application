package com.example.security

import android.util.Log

/**
 * NativeModelLoader.kt
 *
 * PURPOSE:
 * Secure JNI Bridge for loading AI models directly into Native RAM.
 * Prevents model weights from touching the JVM heap, avoiding OOM errors.
 */
object NativeModelLoader {
    private const val TAG = "NativeModelLoader"

    init {
        try {
            System.loadLibrary("cyberguard_secrets")
            Log.i(TAG, "Native AI library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "FATAL: Could not load native library: ${e.message}")
        }
    }

    /**
     * Loads and decrypts an ONNX model entirely within the C++ layer.
     *
     * @param filePath Path to the encrypted .onnx.enc model file.
     * @param masterKey 32-byte AES master key for decryption.
     * @return A native pointer (Long) to the initialized OrtSession object.
     */
    external fun loadModelNative(filePath: String, masterKey: ByteArray): Long

    /**
     * Secure wrapper that ensures the key is scrubbed from Kotlin RAM immediately.
     */
    fun secureLoad(filePath: String, key: ByteArray): Long {
        return try {
            Log.d(TAG, "Initiating secure native load for: $filePath")
            loadModelNative(filePath, key)
        } catch (e: Exception) {
            Log.e(TAG, "Native load failed: ${e.message}")
            0L
        } finally {
            // MANDATE: Immediate Kotlin-side zeroization of the sensitive key
            key.fill(0)
            Log.d(TAG, "Master key zeroized in Kotlin RAM.")
        }
    }
}
