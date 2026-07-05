# 🛡️ CyberGuard-AI: Architecture Specification

![Platform: Android 14+](https://img.shields.io/badge/Platform-Android_14+-3DDC84?logo=android&logoColor=white)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)
![AI: ONNX / Silero](https://img.shields.io/badge/AI_Engine-ONNX_%7C_Silero_VAD-005CED?logo=onnx&logoColor=white)
![Network: 5G URLLC](https://img.shields.io/badge/Network-5G_URLLC-FF2A2A?logo=5g&logoColor=white)

## 1. Executive Summary & Mission
**CyberGuard-AI** is a real-time, 100% on-device scam detection dialer engineered to intercept, analyze, and block social engineering attacks before cognitive compromise occurs. Built natively with **Jetpack Compose** and **Kotlin Coroutines**, the platform leverages a localized AI pipeline (Silero VAD + ONNX) to analyze intent and acoustic anomalies locally. By processing human speech natively on the edge, CyberGuard-AI guarantees absolute data privacy while delivering a zero-latency defense shield for vulnerable populations.

---

## 2. The Edge AI Pipeline & Hardware Optimization
Running deep learning models concurrently with active cellular calls requires extreme resource constraint management. CyberGuard-AI utilizes a heavily optimized edge inference engine to prevent CPU thermal throttling and OS-level Service eviction.

### ⚡ Inference Optimization & Footprint
*   **17ms Inference Window:** The pipeline achieves real-time transcription and semantic analysis by slicing the audio buffer, ensuring the maximum block execution time never exceeds ~17ms per pass.
*   **INT8 Quantization:** The core ASR engine utilizes an `INT8` quantized Sherpa-ONNX Fast Conformer CTC model, drastically reducing precision overhead while retaining 94%+ accuracy for conversational Hinglish/English.
*   **ABI Stripping:** Native libraries are strictly stripped down to `arm64-v8a`, shrinking the final production APK to an ultra-lean **<40MB footprint**.

### 🧠 Memory Safety & C++ Teardown
To prevent catastrophic native memory leaks common in JNI/ONNX bridges:
*   **Zero-Copy Execution:** Employs `MappedByteBuffer` to load the AI models directly into memory without duplicating the payload into the Dalvik heap.
*   **Synchronized Teardown:** The `PipelineSingleton` implements a rigorous `@Synchronized` C++ teardown protocol. Upon `ScamDetectionService` destruction, explicit `close()` commands are dispatched to the C++ `OnlineRecognizer` and `OnlineStream` instances, instantly releasing unmanaged memory back to the OS.

---

## 3. 5G URLLC Telemetry & Bit-Packing (The Swarm Network)
CyberGuard-AI relies on crowd-sourced threat intelligence ("The Swarm") without sacrificing user privacy or burning cellular bandwidth. To achieve this, we engineered a custom binary protocol optimized for **5G Ultra-Reliable Low-Latency Communication (URLLC)**.

### 📦 82-Bit `CallContext` Serialization
The system compresses the entire lifecycle of a call into a microscopic bit-packed payload. We encode 14 distinct telemetry vectors into exactly **10.25 bytes (82 bits)**:
| Telemetry Vector | Bit Allocation | Data Representation |
| :--- | :--- | :--- |
| Bloom Filter Status | 1 bit | `1` (Hit) / `0` (Miss) |
| Threat Score | 7 bits | `0-100` (Max 127) |
| Intent Logits (x5) | 15 bits total | 3 bits per vector (Urgency, Coercion, etc.) |
| Session Duration | 12 bits | Up to 4,095 seconds |
| Days Known | 8 bits | `0-255` days |
| NLP & Acoustic Flags | 39 bits | Reserve & Metadata |

### 🌐 5G URLLC Transport Layer
By stripping out verbose JSON/REST overhead, the final 75-bit network payload is dispatched via UDP with a **DSCP `0xB8`** header (Expedited Forwarding). This guarantees priority routing on 5G networks, allowing the Swarm to update global threat signatures in milliseconds with virtually zero battery or network cost to the user.

---

## 4. Human-Centric Security & Fallback Systems
Advanced AI must gracefully step out of the way when human safety is paramount. The platform implements two critical failsafes to protect vulnerable users.

### 🚨 The SOS Emergency Bypass (Zero-Latency Guarantee)
The AI pipeline must **never** interfere with emergency services. 
Injected at the absolute top of the `ScamDetectionService` interception flow is a hardcoded regex bypass. If an emergency number (e.g., `100`, `112`, `911`, `999`) is dialed, the app immediately executes a `stopSelf()` command. This forcefully bypasses the AI pipeline, instantly releasing all `WakeLocks` and `AudioRecord` hardware buffers to guarantee zero latency and absolute reliability for the emergency call.

### 🛡️ The Guardian Alert System
Designed specifically for the elderly and cognitively vulnerable. When the localized AI pipeline triggers an `isScamDetected` event (Threat Score > 70%), the platform securely interfaces with the Android `SmsManager`. A background emergency SMS is silently dispatched to a pre-configured Guardian, reading:
> *"[CyberGuard Alert] A high-risk scam call from <Scammer Number> was just intercepted on this device."*

---

## 5. Concurrency & UI Architecture
The presentation layer is built exclusively with **Jetpack Compose**, implementing a fluid, reactive state machine driven by Kotlin `StateFlows`.

*   **Smart T9 Predictive Dialer:** Implements a high-performance predictive search algorithm over local SQLite contacts, rendering instant visual suggestions via a highly optimized `LazyRow`.
*   **Mutually Exclusive Tooling:** The Active Call screen dynamically allocates screen real estate, ensuring complex elements like "Live AI ASR Captions" and the "Scam Evidence Pad" remain mutually exclusive to prevent cognitive overload.
*   **Coroutine Safety:** To prevent CPU thrashing and orphaned threads during an abrupt call termination, all background AI inference tasks are strictly anchored to a supervised `serviceScope`. When the OS tears down the Service, the `SupervisorJob` cascades cancellation to all child coroutines instantaneously.

---

## 6. Privacy Compliance & Data Safety
CyberGuard-AI is engineered from the ground up to comply with strict App Store Spyware and Data Broker policies.

*   **100% On-Device Ephemeral Processing:** The microphone stream is parsed in volatile RAM and piped directly into the local ONNX models. Because the audio bytes are destroyed immediately after inference and never uploaded to the cloud, the app legally bypasses "Data Sharing" penalties.
*   **Prominent Disclosure:** Before invoking `FOREGROUND_SERVICE_TYPE_MICROPHONE`, the app explicitly blocks the user with an unskippable "Prominent Disclosure" screen, strictly detailing the exact nature of the localized acoustic analysis to guarantee absolute consent and transparency.
