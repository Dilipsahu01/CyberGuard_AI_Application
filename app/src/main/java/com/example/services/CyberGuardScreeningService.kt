package com.example.services

/**
 * CyberGuardScreeningService.kt
 * 
 * PURPOSE: 
 * Hooks into the Android `CallScreeningService` API.
 * 
 * WHY IT EXISTS:
 * Triggered by the OS the exact moment a call comes in. It checks the Bloom Filter 
 * (local blocklist) and starts the heavy `ScamDetectionService` to begin listening 
 * to the audio stream.
 */
import android.telecom.Call
import android.telecom.CallScreeningService
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pipeline.BloomFilter
import com.example.utils.Constants

class CyberGuardScreeningService : CallScreeningService() {
    private val tag = "ScreeningService"
    private val bloomFilter = BloomFilter()

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: "Unknown"
        Log.d(tag, "onScreenCall intercepted number: $rawNumber")

        val isScamSuspect = bloomFilter.check(rawNumber)

        if (isScamSuspect) {
            Log.w(tag, "Screening hit! Rejecting known scammer number: $rawNumber")
            
            // Rejects call layout at root level
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            
            respondToCall(callDetails, response)
        } else {
            Log.d(tag, "Number clear in screening. Allowing ring phase.")
            
            // Allow call to ring
            val response = CallResponse.Builder().build()
            respondToCall(callDetails, response)

            // ScamDetectionService is NOT started here — InCallService will start it
            // when the call reaches STATE_ACTIVE (answered). Starting it during ringing
            // would grab the mic before an audio route exists, capturing garbage audio.
        }
    }
}
