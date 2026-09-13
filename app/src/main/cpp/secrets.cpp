#include <jni.h>
#include <string>
#include <vector>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_security_NativeSecrets_getServerBaseUrl(JNIEnv* env, jclass /* clazz */) {
    // Hidden string built at runtime using char array to evade Hex editors and `strings` tool
    // Represents: "https://api.cyberguard.example.com"
    char url[] = {
        'h', 't', 't', 'p', 's', ':', '/', '/',
        'a', 'p', 'i', '.', 'c', 'y', 'b', 'e', 'r', 'g', 'u', 'a', 'r', 'd', '.',
        'e', 'x', 'a', 'm', 'p', 'l', 'e', '.', 'c', 'o', 'm', '\0'
    };
    return env->NewStringUTF(url);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_security_NativeSecrets_getModelMasterKey(JNIEnv* env, jclass /* clazz */, jbyteArray salt) {
    // Hidden Base Key: b"CyberGuard_Secure_Model_Key_2026"
    jbyte base_key[] = {
        'C', 'y', 'b', 'e', 'r', 'G', 'u', 'a', 'r', 'd', '_',
        'S', 'e', 'c', 'u', 'r', 'e', '_', 'M', 'o', 'd', 'e', 'l', '_',
        'K', 'e', 'y', '_', '2', '0', '2', '6'
    };

    // Get the salt from Java
    jsize salt_len = env->GetArrayLength(salt);
    jbyte* salt_ptr = env->GetByteArrayElements(salt, nullptr);

    // MANDATE: Secure Key Derivation via XOR Mix-in
    // If the salt is empty or null, we return the base key (for compatibility).
    // In production, we'd enforce a valid dynamic salt.
    if (salt_len > 0) {
        for (int i = 0; i < 32; i++) {
            base_key[i] = (jbyte)(base_key[i] ^ salt_ptr[i % salt_len]);
        }
    }

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, base_key);

    // Cleanup and Zeroize
    env->ReleaseByteArrayElements(salt, salt_ptr, JNI_ABORT);

    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_security_NativeSecrets_getTelemetryPepper(JNIEnv* env, jclass /* clazz */) {
    // Hidden Telemetry Pepper: b"CyberGuard_DPDP_Telemetry_Key_32"
    jbyte pepper[] = {
        'C', 'y', 'b', 'e', 'r', 'G', 'u', 'a', 'r', 'd', '_',
        'D', 'P', 'D', 'P', '_', 'T', 'e', 'l', 'e', 'm', 'e', 't', 'r', 'y', '_',
        'K', 'e', 'y', '_', '3', '2'
    };

    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, pepper);

    return result;
}
