package com.example.security

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * EnvironmentGuard.kt
 * V1.1_Updates Section 3: Environmental Security (The Tripwire)
 * 
 * Checks for indicators of a compromised (rooted) execution environment.
 * If these checks fail, the app should shut down immediately to protect the AI models.
 */
object EnvironmentGuard {

    fun isDeviceCompromised(): Boolean {
        return checkTestKeys() || checkSuBinaries() || checkDangerousMounts()
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinaries(): Boolean {
        val paths = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkDangerousMounts(): Boolean {
        try {
            val process = Runtime.getRuntime().exec("mount")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // If the main system partition is mounted as read-write, the device is likely rooted
                if (line?.contains(" /system ") == true && line?.contains("rw,") == true) {
                    return true
                }
            }
            reader.close()
            process.destroy()
        } catch (e: Exception) {
            // Ignore execution failures
        }
        return false
    }
}
