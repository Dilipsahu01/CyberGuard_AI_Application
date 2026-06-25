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
            Log.i(TAG, "Device boot completed. Initiating CyberGuard 5G Swarm Synchronization...")
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    // Initialize SwarmReporter and flush any offline enqueued telemetry
                    val swarmReporter = SwarmReporter(context.applicationContext)
                    swarmReporter.flushQueue()
                    Log.d(TAG, "Swarm telemetry offline queue flush completed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize SwarmReporter on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
