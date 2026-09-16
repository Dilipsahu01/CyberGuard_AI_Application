package com.example.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class SecurityTests {

    @Test
    fun testEnvironmentGuard_noCrash() {
        // We cannot guarantee the emulator isn't rooted (some test images are userdebug/test-keys)
        // But we MUST guarantee that the function executes without crashing and throwing unexpected exceptions.
        val isCompromised = EnvironmentGuard.isDeviceCompromised()
        // If it runs, the test passes. We just print the result.
        println("Device Compromised Status: $isCompromised")
    }

    @Test
    fun testNativeSecrets_AESKeyFormat() {
        val masterKey = NativeSecrets.getModelMasterKey(ByteArray(16))
        // The AES-256 key must be exactly 32 bytes
        assertEquals("Master key must be exactly 32 bytes (256-bit)", 32, masterKey.size)
        
        // Ensure it's not all zeros
        val sum = masterKey.sumOf { it.toInt() }
        assertNotEquals("Master key should not be empty", 0, sum)
    }

    @Test
    fun testNativeSecrets_ServerUrl() {
        val url = NativeSecrets.getServerBaseUrl()
        assertTrue("Server URL must be HTTPS", url.startsWith("https://"))
        assertEquals("https://api.cyberguard.example.com", url)
    }

    @Test
    fun testModelIntegrityVerifier_edgeCases() {
        // Test 1: Invalid file name should fail
        val fakeBytes = ByteArray(10)
        assertFalse(ModelIntegrityVerifier.verifyByteArray(fakeBytes, "unknown_model.onnx"))

        // Test 2: Valid file name but wrong bytes (poisoned model)
        assertFalse(ModelIntegrityVerifier.verifyByteArray(fakeBytes, "silero_vad.ort"))
        
        // We cannot easily test the exact true case without the real 150MB file loaded in memory,
        // but ensuring it strictly rejects poisoned bytes is the most critical edge case.
    }

    @Test
    fun testModelCryptoManager_KeystoreLock() {
        // Ensure the class is initialized
        val cryptoManager = ModelCryptoManager
        
        // Verify the Android KeyStore actually has our key
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        val containsAlias = keyStore.containsAlias("CyberGuard_Model_Key")
        assertTrue("AndroidKeyStore MUST contain the AES alias after initialization", containsAlias)
        
        val entry = keyStore.getEntry("CyberGuard_Model_Key", null)
        assertTrue("Key entry must be a SecretKeyEntry", entry is KeyStore.SecretKeyEntry)
    }
}
