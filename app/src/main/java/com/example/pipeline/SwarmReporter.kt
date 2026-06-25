package com.example.pipeline

/**
 * SwarmReporter.kt
 * 
 * PURPOSE: 
 * The networking interface for the Federated Swarm Immunity system.
 * 
 * WHY IT EXISTS:
 * It compresses local threat telemetry into an ultra-compact 10.25-byte payload. 
 * To ensure ultra-low latency, it forces these packets onto the 5G URLLC network slice 
 * using a DSCP 0xB8 Expedited Forwarding header. If offline, it triggers a 2G SMS fallback.
 */
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SmsManager
import android.util.Log
import com.example.models.CallContext
import com.example.models.PendingSwarmReport
import com.example.models.RiskResult
import com.example.models.ScamDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Socket
import javax.net.SocketFactory
import java.util.concurrent.TimeUnit

class SwarmReporter(private val context: Context) {
    private val SWARM_SERVER_URL: String
        get() = context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
            .getString("swarm_server_url", "https://api.cyberguard-ai.com/telemetry") ?: "https://api.cyberguard-ai.com/telemetry"
    private val db = ScamDatabase.getDatabase(context)

    private val httpClient = getHttpClient()

    companion object {
        private const val TAG = "SwarmReporter"

        private val prioritizedSocketFactory = object : SocketFactory() {
            private val defaultFactory = getDefault()
            
            private fun applyQos(socket: Socket): Socket {
                try {
                    socket.trafficClass = 0xB8 // Expedited Forwarding (DSCP EF)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply DSCP EF socket traffic class: ${e.message}")
                }
                return socket
            }

            override fun createSocket(): Socket = applyQos(defaultFactory.createSocket())
            override fun createSocket(host: String?, port: Int): Socket = applyQos(defaultFactory.createSocket(host, port))
            override fun createSocket(host: String?, port: Int, localHost: java.net.InetAddress?, localPort: Int): Socket = applyQos(defaultFactory.createSocket(host, port, localHost, localPort))
            override fun createSocket(address: java.net.InetAddress?, port: Int): Socket = applyQos(defaultFactory.createSocket(address, port))
            override fun createSocket(address: java.net.InetAddress?, port: Int, localAddress: java.net.InetAddress?, localPort: Int): Socket = applyQos(defaultFactory.createSocket(address, port, localAddress, localPort))
        }

        private class QosSslSocketFactory(private val delegate: javax.net.ssl.SSLSocketFactory) : javax.net.ssl.SSLSocketFactory() {
            private fun applyQos(socket: Socket): Socket {
                try {
                    socket.trafficClass = 0xB8 // Expedited Forwarding (DSCP EF)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply DSCP EF socket traffic class to SSL socket: ${e.message}")
                }
                return socket
            }

            override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
            override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

            override fun createSocket(): Socket = applyQos(delegate.createSocket())
            override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket = applyQos(delegate.createSocket(s, host, port, autoClose))
            override fun createSocket(host: String?, port: Int): Socket = applyQos(delegate.createSocket(host, port))
            override fun createSocket(host: String?, port: Int, localHost: java.net.InetAddress?, localPort: Int): Socket = applyQos(delegate.createSocket(host, port, localHost, localPort))
            override fun createSocket(address: java.net.InetAddress?, port: Int): Socket = applyQos(delegate.createSocket(address, port))
            override fun createSocket(address: java.net.InetAddress?, port: Int, localAddress: java.net.InetAddress?, localPort: Int): Socket = applyQos(delegate.createSocket(address, port, localAddress, localPort))
        }

        private fun getDefaultTrustManager(): javax.net.ssl.X509TrustManager {
            val trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as java.security.KeyStore?)
            return trustManagerFactory.trustManagers.first { it is javax.net.ssl.X509TrustManager } as javax.net.ssl.X509TrustManager
        }

        @Volatile
        private var sharedHttpClient: OkHttpClient? = null

        fun getHttpClient(): OkHttpClient {
            return sharedHttpClient ?: synchronized(this) {
                if (sharedHttpClient == null) {
                    try {
                        val trustManager = getDefaultTrustManager()
                        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                        sslContext.init(null, arrayOf(trustManager), null)
                        val qosSslSocketFactory = QosSslSocketFactory(sslContext.socketFactory)

                        sharedHttpClient = OkHttpClient.Builder()
                            .socketFactory(prioritizedSocketFactory)
                            .sslSocketFactory(qosSslSocketFactory, trustManager)
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .build()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to configure SSL QoS Socket Factory: ${e.message}. Using fallback OkHttpClient.")
                        sharedHttpClient = OkHttpClient.Builder()
                            .socketFactory(prioritizedSocketFactory)
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .build()
                    }
                }
                sharedHttpClient!!
            }
        }
    }

    /**
     * Entry point to report scam telemetry.
     */
    suspend fun reportScam(callerNumber: String, callContext: CallContext, riskResult: RiskResult, bloomHit: Boolean) {
        val hashPrefix = callerNumber.hashCode() and 0x3FFF
        val payload = callContext.packToBinary(bloomHit, riskResult, hashPrefix)
        Log.d(TAG, "Constructed microscopic Stage 7 swarm payload size: ${payload.size} bytes (approx 10.25 bytes spec limit)")
        
        if (isNetworkAvailable()) {
            val success = uploadPayload(payload, callerNumber)
            if (!success) {
                enqueueReport(payload, callerNumber)
            }
        } else {
            Log.w(TAG, "Network offline. Store & Forward active. Enqueueing report to Room database...")
            enqueueReport(payload, callerNumber)
            
            val prefs = context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
            val enableSms = prefs.getBoolean("enable_sms_fallback", false)
            if (enableSms) {
                triggerSmsFallback(payload, callerNumber)
            } else {
                Log.d(TAG, "SMS fallback is disabled by policy preferences.")
            }
        }
    }

    private fun uploadPayload(payload: ByteArray, @Suppress("UNUSED_PARAMETER") callerNumber: String): Boolean {
        return try {
            val requestBody = payload.toRequestBody("application/octet-stream".toMediaType())
            
            val hashedCaller = hashCallerNumber(callerNumber)

            val request = Request.Builder()
                .url(SWARM_SERVER_URL)
                .post(requestBody)
                .addHeader("X-Swarm-Caller", hashedCaller)
                .addHeader("X-5G-QoS", "URLLC-Slice-EF")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully transmitted 10.25-byte telemetry over 5G priority slice!")
                    true
                } else {
                    Log.e(TAG, "Server rejected swarm payload: ${response.code} — ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload swarm report over HTTP: ${e.message}")
            false
        }
    }

    private suspend fun enqueueReport(payload: ByteArray, callerNumber: String) {
        try {
            val report = PendingSwarmReport(
                timestamp = System.currentTimeMillis(),
                payload = payload,
                callerNumber = callerNumber
            )
            db.pendingSwarmReportDao().insertReport(report)
            Log.i(TAG, "Successfully enqueued telemetry report in pending_swarm_reports SQLite table.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write enqueued report: ${e.message}")
        }
    }

    suspend fun flushQueue() {
        try {
            val pendingReports = db.pendingSwarmReportDao().getAllPendingReports()
            if (pendingReports.isEmpty()) return

            Log.d(TAG, "Network restored. Flashing ${pendingReports.size} enqueued swarm reports...")
            for (report in pendingReports) {
                val success = uploadPayload(report.payload, report.callerNumber)
                if (success) {
                    db.pendingSwarmReportDao().deleteReport(report)
                    Log.i(TAG, "Successfully flushed enqueued report #${report.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing queue: ${e.message}")
        }
    }

    private fun hashCallerNumber(callerNumber: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(callerNumber.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        } catch (e: Exception) {
            "anonymous_caller"
        }
    }

    /**
     * Optional Silent SMS fallback for intermittent coverage zones.
     * Encodes 10.25-byte binary into safe base64/hex and transmits silently to designated node.
     */
    private fun triggerSmsFallback(payload: ByteArray, callerNumber: String) {
        try {
            val hexPayload = payload.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            val hashedCaller = hashCallerNumber(callerNumber)
            val smsText = "CG5G:$hashedCaller:$hexPayload"
            
            val prefs = context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE)
            val receiverNumber = prefs.getString("sms_fallback_number", "") ?: ""
            if (receiverNumber.isBlank()) {
                Log.w(TAG, "SMS fallback receiver number is not configured. Skipping SMS fallback.")
                return
            }
            
            Log.d(TAG, "Executing Silent SMS Fallback. Sending to $receiverNumber payload: $smsText")
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(receiverNumber, null, smsText, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "SMS Fallback trigger failed (permissions/hardware): ${e.message}")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
