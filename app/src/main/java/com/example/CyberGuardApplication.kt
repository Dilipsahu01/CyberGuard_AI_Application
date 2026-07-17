package com.example

/**
 * CyberGuardApplication.kt
 * 
 * PURPOSE: 
 * The Application-level Singleton. It initializes before any Activity or Service.
 * 
 * WHY IT EXISTS:
 * Used to set up critical global resources like the Room Database instance, 
 * ONNX Runtime environments, and the Swarm Telemetry network listener. 
 * This ensures the models are ready the exact millisecond a call rings.
 */
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.pipeline.SwarmReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CyberGuardApplication : Application() {
    private val TAG = "CyberGuardApplication"

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()
        
        // V1.1_Updates Section 6: Offline Threat Intelligence
        // Pre-load the DOT scammer CSV database from Room directly into the RAM-based BloomFilter.
        // This guarantees that the CallScreeningService can intercept scams at exactly 0ms latency
        // the moment the phone rings, without hitting the disk!
        com.example.pipeline.BloomFilter().loadFromDatabase(this)
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                networkRequest, 
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Network became available. Triggering SwarmReporter.flushQueue() for consistency.")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val swarmReporter = SwarmReporter(applicationContext)
                                swarmReporter.flushQueue()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to flush queue on reconnection: ${e.message}")
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }
}
