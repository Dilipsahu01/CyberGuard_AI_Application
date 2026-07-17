#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <android/log.h>
#include <openssl/evp.h>
#include <openssl/aes.h>
#include <openssl/crypto.h> // Required for OPENSSL_cleanse
#include <onnxruntime_cxx_api.h>
#include <new> // Required for std::bad_alloc

#define TAG "NATIVE_AI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_security_NativeModelLoader_loadModelNative(
        JNIEnv* env,
        jobject /* this */,
        jstring file_path,
        jbyteArray master_key) {

    const char* native_file_path = env->GetStringUTFChars(file_path, nullptr);
    jbyte* native_key_ptr = env->GetByteArrayElements(master_key, nullptr);
    jsize key_len = env->GetArrayLength(master_key);

    LOGD("Native: Loading encrypted model from %s", native_file_path);

    // 1. Read encrypted model file (IV[12] + TAG[16] + Ciphertext)
    std::ifstream file(native_file_path, std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        LOGE("Native: Failed to open model file.");
        env->ReleaseByteArrayElements(master_key, native_key_ptr, JNI_ABORT);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }

    std::streamsize file_size = file.tellg();
    file.seekg(0, std::ios::beg);

    std::streamsize ciphertext_size = file_size - 12 - 16;
    if (ciphertext_size <= 0) {
        LOGE("Native: Invalid file size.");
        env->ReleaseByteArrayElements(master_key, native_key_ptr, JNI_ABORT);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }

    std::vector<uint8_t> iv(12);
    std::vector<uint8_t> tag(16);
    std::vector<uint8_t> ciphertext;
    
    try {
        ciphertext.resize(ciphertext_size);
    } catch (const std::bad_alloc& e) {
        LOGE("Native: OOM when allocating ciphertext buffer: %s", e.what());
        env->ReleaseByteArrayElements(master_key, native_key_ptr, JNI_ABORT);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }

    if (!file.read((char*)iv.data(), 12) || 
        !file.read((char*)tag.data(), 16) || 
        !file.read((char*)ciphertext.data(), ciphertext_size)) {
        LOGE("Native: Failed to read model components.");
        env->ReleaseByteArrayElements(master_key, native_key_ptr, JNI_ABORT);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }
    file.close();

    // 2. Decrypt in Native RAM using AES-GCM (OpenSSL)
    std::vector<uint8_t> decrypted_model;
    try {
        decrypted_model.resize(ciphertext_size);
    } catch (const std::bad_alloc& e) {
        LOGE("Native: OOM when allocating decrypted model buffer: %s", e.what());
        OPENSSL_cleanse(native_key_ptr, key_len);
        env->ReleaseByteArrayElements(master_key, native_key_ptr, 0);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }
    
    int decrypted_len = 0;
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    
    if (!ctx) {
        LOGE("Native: Failed to create EVP cipher context.");
        OPENSSL_cleanse(native_key_ptr, key_len);
        env->ReleaseByteArrayElements(master_key, native_key_ptr, 0);
        env->ReleaseStringUTFChars(file_path, native_file_path);
        return 0;
    }

    EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr);
    EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, 12, nullptr);
    EVP_DecryptInit_ex(ctx, nullptr, nullptr, (unsigned char*)native_key_ptr, iv.data());

    EVP_DecryptUpdate(ctx, decrypted_model.data(), &decrypted_len, ciphertext.data(), ciphertext_size);
    EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, 16, tag.data());

    int ret = EVP_DecryptFinal_ex(ctx, decrypted_model.data() + decrypted_len, &decrypted_len);
    EVP_CIPHER_CTX_free(ctx);

    // MANDATE: Immediate zeroization of the decryption key in C++
    OPENSSL_cleanse(native_key_ptr, key_len);
    // Mode 0 applies the buffer modifications back to Kotlin (copying zeroes over)
    env->ReleaseByteArrayElements(master_key, native_key_ptr, 0);
    env->ReleaseStringUTFChars(file_path, native_file_path);

    if (ret <= 0) {
        LOGE("Native: Decryption failed (Integrity check failed).");
        OPENSSL_cleanse(decrypted_model.data(), decrypted_model.size());
        return 0;
    }

    LOGD("Native: Decryption successful. Model size: %zu bytes", decrypted_model.size());

    // 3. Initialize ONNX Runtime Session from Native Memory
    static Ort::Env ort_env(ORT_LOGGING_LEVEL_WARNING, "NativeModelLoader");
    Ort::SessionOptions session_options;
    session_options.SetIntraOpNumThreads(2);
    session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);

    Ort::Session* session = nullptr;
    try {
        // Create session directly from native RAM buffer
        session = new Ort::Session(ort_env, decrypted_model.data(), decrypted_model.size(), session_options);
        LOGD("Native: OrtSession created successfully. Pointer: %p", (void*)session);
    } catch (const Ort::Exception& e) {
        LOGE("Native: OrtSession creation failed: %s", e.what());
    } catch (const std::bad_alloc& e) {
        LOGE("Native: OrtSession pointer allocation failed: %s", e.what());
    }

    // MANDATE: Immediate zeroization of the decrypted weights once session is created
    // ONNX Runtime internalizes a copy of the graph, so we aggressively scrub the raw decrypted buffer.
    OPENSSL_cleanse(decrypted_model.data(), decrypted_model.size());

    return (jlong)session;
}
