package com.example.security

object NativeSecrets {
    init {
        try {
            System.loadLibrary("cyberguard_secrets")
        } catch (e: UnsatisfiedLinkError) {
            println("NativeSecrets: JNI Library not found (Test Environment detected).")
        }
    }

    external fun getServerBaseUrl(): String
    
    // Returns the 32-byte AES Master Key from the NDK layer
    external fun getModelMasterKey(): ByteArray
}
