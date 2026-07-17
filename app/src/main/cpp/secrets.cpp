#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_security_NativeSecrets_getServerBaseUrl(JNIEnv* env, jobject /* this */) {
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
Java_com_example_security_NativeSecrets_getModelMasterKey(JNIEnv* env, jobject /* this */) {
    // Hidden Master Key: b"CyberGuard_Secure_Model_Key_2026"
    jbyte key[] = {
        'C', 'y', 'b', 'e', 'r', 'G', 'u', 'a', 'r', 'd', '_',
        'S', 'e', 'c', 'u', 'r', 'e', '_', 'M', 'o', 'd', 'e', 'l', '_',
        'K', 'e', 'y', '_', '2', '0', '2', '6'
    };
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, key);
    return result;
}
