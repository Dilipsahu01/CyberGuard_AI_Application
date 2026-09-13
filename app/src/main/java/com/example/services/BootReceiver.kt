package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pipeline.SwarmReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Initiating CyberGuard 5G Swarm Synchronization and AI Preload...")
            
            // goAsync() allows this receiver to run for up to 60 seconds, 
            // giving us plenty of time to decrypt the ~150MB models into RAM.
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    // 1. Initialize SwarmReporter and flush any offline enqueued telemetry
                    val swarmReporter = SwarmReporter(context.applicationContext)
                    swarmReporter.flushQueue()
                    Log.d(TAG, "Swarm telemetry offline queue flush completed.")

                    // 2. Preload the AI Models (Zero-Allocation Singleton)
                    Log.i(TAG, "Starting Silent Boot Preload of AI Models...")
                    com.example.pipeline.PipelineSingleton.getInstance(context.applicationContext)
                    Log.i(TAG, "Silent Boot Preload Complete! Models are warm in RAM.")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize background services on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
