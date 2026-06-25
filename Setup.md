# CyberGuard-AI Storage Optimization Setup

This document records the storage and APK size optimizations applied to the CyberGuard-AI project prior to deployment.

## 1. Removed Duplicate ASR Model
**Issue:** The project contained a massive 132 MB duplicate of the ASR neural network. It existed both at `assets/sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/model.int8.onnx` and `assets/models/asr_model.ort`.
**Action Taken:** 
* Executed `rm app/src/main/assets/models/asr_model.ort`
* Verified that `StreamingASR.kt` dynamically points to the `sherpa-onnx-nemo...` folder, making `asr_model.ort` entirely redundant.
**Impact:** Instantly saved **132 MB** of permanent APK and device storage space.

## 2. ABI Architecture Filtering (C++ Native Libraries)
**Issue:** The universal debug APK was bundling ONNX Runtime and Sherpa-ONNX `.so` library binaries for four different CPU architectures (`x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`), causing a severe bloat of roughly ~120 MB.
**Action Taken:** 
* Modified `app/build.gradle.kts`
* Added an `ndk { abiFilters.add("arm64-v8a") }` block inside the `defaultConfig` scope.
**Impact:** Forces Gradle to only package the 64-bit ARM architecture (which powers 99% of modern Android devices), stripping emulator and legacy 32-bit binaries. This saves roughly **80 MB** per universal APK build.

## Summary of Optimization
Total Storage Saved: **~212 MB**
Original Universal APK Size: **357.7 MB**
Optimized APK Size: **~145.7 MB**

*Note: For the absolute smallest footprint upon final deployment to the Google Play Store, generating an Android App Bundle (.aab) instead of a direct APK is recommended.*
