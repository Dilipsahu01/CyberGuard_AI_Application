package com.example.security

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * DeviceHealthManager.kt
 *
 * Enforces the Graceful Degradation Policy by actively monitoring the hardware footprint.
 * It prevents the app from running on unsupported devices (avoiding LMK crashes)
 * and uses Android's ADPF Thermal API to throttle AI models dynamically.
 */
object DeviceHealthManager {
    private const val TAG = "DeviceHealthManager"
    private const val MIN_RAM_BYTES = 3L * 1024 * 1024 * 1024 // 3GB RAM Floor

    enum class OperationalMode {
        NOMINAL,    // < 0.70 Thermal Headroom (All AI models running)
        THROTTLED,  // 0.70 - 0.85 (Skip heavy NLP, run VAD/ASR/Regex only)
        CRITICAL    // > 0.85 (Shutdown all AI, Regex only, UI warning)
    }

    /**
     * The Initialization Gate. Call this in MainActivity or Application onCreate.
     * Checks if the device physically has the RAM and architecture to run mmap models.
     */
    fun checkHardwareRequirements(context: Context): Boolean {
        // Check Physical RAM Floor
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRam = memoryInfo.totalMem

        if (totalRam < MIN_RAM_BYTES) {
            Log.e(TAG, "HARDWARE FAILURE: Device has ${totalRam / (1024*1024)}MB RAM. 3GB required.")
            return false
        }

        // Check Architecture (We enforce 64-bit arm64-v8a in Gradle, but runtime check is safe)
        val is64Bit = Build.SUPPORTED_ABIS.contains("arm64-v8a")
        if (!is64Bit) {
            Log.e(TAG, "HARDWARE FAILURE: 32-bit architecture detected. arm64-v8a required.")
            return false
        }

        return true
    }

    /**
     * The Runtime Monitor (ADPF Integration)
     * Queries the OS thermal subsystem to predict throttling before it destroys inference latency.
     */
    fun getOperationalMode(context: Context): OperationalMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // 0 is forecasting the current immediate state.
            val headroom = powerManager.getThermalHeadroom(0)

            return when {
                headroom.isNaN() -> OperationalMode.NOMINAL // Fallback if API fails
                headroom >= 0.85f -> OperationalMode.CRITICAL
                headroom >= 0.70f -> OperationalMode.THROTTLED
                else -> OperationalMode.NOMINAL
            }
        }
        
        // Fallback for devices below API 30 (We enforce API 30 in Manifest, but just in case)
        return OperationalMode.NOMINAL
    }
}
