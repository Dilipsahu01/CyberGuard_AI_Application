package com.example.security

object NativeSecrets {
    init {
        try {
            System.loadLibrary("crypto") // Load OpenSSL dependency first
            System.loadLibrary("cyberguard_secrets")
        } catch (e: UnsatisfiedLinkError) {
            // Using ModelCryptoManager tag so it shows up in the user's filtered logcat
            android.util.Log.e("ModelCryptoManager", "JNI Library failed to load: ${e.message}", e)
        }
    }

    @JvmStatic
    external fun getServerBaseUrl(): String

    // MANDATE: Dynamic Key Derivation (V1.1_Updates Section 2)
    // Takes a device-unique salt to prevent static key extraction from binary.
    @JvmStatic
    external fun getModelMasterKey(salt: ByteArray): ByteArray

    // DPDP Telemetry Encryption Key
    @JvmStatic
    external fun getTelemetryPepper(): ByteArray
}
