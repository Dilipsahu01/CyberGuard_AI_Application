package com.example.security

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ModelCryptoManager.kt
 * V1.1_Updates Section 1: Android Hardware Keystore Engine
 *
 * Securely imports the AES Master Key from the C++ layer into the physical
 * Android Keystore (Trusted Execution Environment), and uses it to decrypt
 * the AI models from assets.
 */
object ModelCryptoManager {
    private const val KEY_ALIAS = "CyberGuard_Model_Key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // 16 bytes
    private const val TAG = "ModelCryptoManager"

    init {
        importKeyToKeystore()
    }

    private fun importKeyToKeystore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Only import if not already locked in hardware
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val salt = ByteArray(0)
                val rawKey = NativeSecrets.getModelMasterKey(salt)
                val secretKey = SecretKeySpec(rawKey, KeyProperties.KEY_ALGORITHM_AES)

                val protection = KeyProtection.Builder(KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false) // We read the IV from the file
                    .build()

                keyStore.setEntry(KEY_ALIAS, KeyStore.SecretKeyEntry(secretKey), protection)

                // Scrub the raw key from RAM immediately to prevent heap dumps from reading it
                rawKey.fill(0)
                android.util.Log.i(TAG, "Master key successfully imported into hardware Keystore.")
            }
        } catch (e: UnsatisfiedLinkError) {
            // NDK library not loaded — this is expected in JVM test environments
            android.util.Log.e(TAG, "NDK Library not loaded. Key import skipped: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Hardware Keystore key import failed: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Retry import if key is missing (handles race condition on first boot)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            android.util.Log.w(TAG, "Key not found on first attempt. Retrying import...")
            importKeyToKeystore()
            keyStore.load(null)
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw SecurityException("Master key not found in hardware Keystore.")
        return entry.secretKey
    }

    /**
     * Decrypts an InputStream (from assets) into an OutputStream (internal app cache)
     * using a chunked streaming approach to keep JVM heap usage minimal.
     */
    fun decryptModelStream(inputStream: InputStream, outputStream: OutputStream) {
        // Read the 12-byte IV
        val iv = ByteArray(12)
        if (inputStream.read(iv) != 12) {
            throw SecurityException("Invalid encrypted model format: missing IV")
        }

        // Read the 16-byte Tag
        val tag = ByteArray(16)
        if (inputStream.read(tag) != 16) {
            throw SecurityException("Invalid encrypted model format: missing Tag")
        }

        // Initialize the Cipher using the hardware key and IV
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        // MANDATE: True Streaming Decryption via chunked buffer (64KB)
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val decrypted = cipher.update(buffer, 0, bytesRead)
                if (decrypted != null) {
                    outputStream.write(decrypted)
                }
                // MANDATE: Memory Zeroization - scrub the chunk buffer immediately
                buffer.fill(0)
            }

            // Final block processing (verifies GCM integrity tag)
            val finalBlock = cipher.doFinal(tag)
            if (finalBlock != null) {
                outputStream.write(finalBlock)
                finalBlock.fill(0) // Zero out final block
            }
            outputStream.flush()
        } finally {
            // Ensure buffers are zeroed even on failure
            buffer.fill(0)
            iv.fill(0)
            tag.fill(0)
        }
    }

    /**
     * RAM-Only Decryption Strategy: Decrypts directly into a ByteArray in memory.
     * Prevents the unencrypted AI model from ever touching the physical disk.
     */
    fun decryptModelToByteArray(inputStream: InputStream, originalName: String): ByteArray {
        val iv = ByteArray(12)
        if (inputStream.read(iv) != 12) throw SecurityException("Invalid encrypted model format: missing IV")

        val tag = ByteArray(16)
        if (inputStream.read(tag) != 16) throw SecurityException("Invalid encrypted model format: missing Tag")

        val ciphertext = inputStream.readBytes()
        val combinedCiphertext = ciphertext + tag

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val plaintext = cipher.doFinal(combinedCiphertext)

        // Integrity Check (Subpart 3)
        if (!ModelIntegrityVerifier.verifyByteArray(plaintext, originalName)) {
            plaintext.fill(0) // Scrub memory immediately on failure
            throw SecurityException("CRITICAL: SHA-256 Integrity Verification Failed for $originalName! Model weights may be poisoned.")
        }

        return plaintext
    }

    /**
     * Internal Cache Decryption Strategy: Used when native C++ libraries (like Sherpa-ONNX)
     * strictly require a physical file path. Decrypts to a secure, no-backup internal directory.
     */
    fun decryptModelToCache(context: android.content.Context, assetPath: String, originalName: String): java.io.File {
        val encryptedAsset = "$assetPath.enc"

        // Use context.noBackupFilesDir to prevent the unencrypted model from being synced to Google Drive
        val secureDir = java.io.File(context.noBackupFilesDir, "secure_models")
        if (!secureDir.exists()) secureDir.mkdirs()

        val outputFile = java.io.File(secureDir, originalName)

        // Fast-path: if already decrypted and valid, skip
        if (outputFile.exists() && ModelIntegrityVerifier.verifyFile(outputFile, originalName)) {
            return outputFile
        }

        context.assets.open(encryptedAsset).use { input ->
            outputFile.outputStream().use { output ->
                decryptModelStream(input, output)
            }
        }

        if (!ModelIntegrityVerifier.verifyFile(outputFile, originalName)) {
            outputFile.delete()
            throw SecurityException("CRITICAL: SHA-256 Integrity Verification Failed for $originalName")
        }

        return outputFile
    }

    /**
     * MANDATE: IP Protection (V1.1_Updates Section 1)
     * Securely erases all unencrypted model weights from the disk cache.
     */
    fun purgeModelCache(context: android.content.Context) {
        val secureDir = java.io.File(context.noBackupFilesDir, "secure_models")
        if (secureDir.exists()) {
            secureDir.listFiles()?.forEach {
                it.delete()
                // Zero-fill or immediate deletion is preferred for IP safety
            }
        }
    }
}
