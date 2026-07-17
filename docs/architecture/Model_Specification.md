# CyberGuard-AI: Inference Stack & Model Specification

This document provides a centralized technical specification for all active Machine Learning models and deterministic Regex pattern matchers utilized in the CyberGuard-AI call screening pipeline. 

All pipeline executions are strictly anchored to the `ScamDetectionService` and execute on background coroutines (`Dispatchers.Default` and `Dispatchers.IO`) to maintain Main-thread safety.

---

## 1. Machine Learning Models (Active Inference)

### 1.1 Silero VAD (Voice Activity Detection)
* **Purpose**: Identifies human speech segments to gate expensive NLP operations and save battery.
* **Input Type**: Raw Audio (FloatArray PCM chunks, 100ms / 1600 samples).
* **Output Type**: Probability Score (Float). Threshold > `0.5f` yields `Boolean` (Speech Detected).
* **Model Attributes**:
  * **Architecture**: Recurrent Neural Network (RNN).
  * **Memory Strategy**: `silero_vad.ort` is AES-256 decrypted at runtime and **memory-mapped (mmap)** into volatile RAM to prevent disk caching. Zero-allocation execution path.
  * **Constraints**: 16kHz sample rate, Mono channel.

### 1.2 Sherpa-ONNX Fast Conformer CTC
* **Purpose**: Real-time localized Speech-to-Text (ASR) tailored for Hinglish and regional accents.
* **Input Type**: Raw Audio (FloatArray stream).
* **Output Type**: Text Tokens (String transcription buffer).
* **Model Attributes**:
  * **Architecture**: Fast Conformer CTC.
  * **Quantization**: **INT8 Quantized** (`model.int8.onnx`) for a memory footprint of ~40MB.
  * **Tokenizer**: Byte-Pair Encoding (BPE), backed by `tokens.txt`.
  * **Memory Strategy**: Executed natively via C++ JNI bridge, minimizing JVM garbage collection overhead. Cleaned up explicitly via `pipelineManager.close()` inside a `try-finally` block.

### 1.3 MiniLM-L6 (IntentNLP Engine)
* **Purpose**: Performs semantic intent projection for high-level social engineering detection.
* **Input Type**: String buffer (100-word sliding window parsed from ASR).
* **Output Type**: Classification Logits (Financial, Urgency, Coercion, Intimacy, Trust) mapped to a 0-127 scale.
* **Model Attributes**:
  * **Architecture**: Transformer-based sequence classifier.
  * **Dimensionality**: 384-dimensional embedding vector.
  * **Quantization**: **INT8 Quantized** (`minilm_int8.ort`).
  * **Tokenizer**: BERT-style WordPiece tokenizer (`vocab.txt`).
  * **Memory Strategy**: Decrypted at runtime and **memory-mapped (mmap)** into a `MappedByteBuffer` shared via a Singleton instance.

---

## 2. Deterministic Regex Threat Matrix (RegexGate)

The `RegexGate.kt` module implements a high-speed, `synchronized` pattern matching layer. It bypasses semantic probability to provide an instantaneous, zero-latency kill-switch for known, highly-specific Indian threat vectors.

### 2.1 Threat Patterns & Utility
| Regex Pattern | Weight | Target Scam Utility |
| :--- | :---: | :--- |
| `\botp\b` | 35 | **Credential Theft**: Harvesting OTPs for immediate bank drain. |
| `digital.?arrest` | 40 | **Law Enforcement Impersonation**: Coercing victims via fake warrants. |
| `\banydesk\b` / `\bteamviewer\b` | 35 | **Remote Access Trojan (RAT)**: Tricking users into screen-sharing. |
| `police.{0,20}coming` / `\bcbi\b` | 30 | **Extortion / Coercion**: Instilling immediate fear of arrest. |
| `account.{0,20}(freeze\|block\|suspend)` | 25 | **Financial Panic**: Pushing victims to "secure" their funds. |
| `(wire\|transfer).{0,20}(money\|funds\|rupee)` | 35 | **Direct Financial Request**: The closing stage of a scam. |
| `verify.{0,20}(account\|identity\|aadhar\|pan)` | 20 | **KYC Fraud**: Phishing for identity documents. |
| `(aadhaar\|aadhar).{0,20}(number\|link\|otp)` | 35 | **Aadhaar Identity Theft**: Linking scams. |
| `\bgift.?card\b` | 30 | **Money Laundering**: Untraceable financial extortion. |
| `(share\|send).{0,10}(pin\|password\|cvv)` | 40 | **Payment Fraud**: Stealing hard card details. |
| `\b(fedex\|customs).{0,20}(parcel\|package\|duty)\b`| 35 | **Customs / Courier Scam**: Fake illegal parcel extortion. |
| `sim.{0,10}(block\|deactivate\|upgrade)` | 30 | **SIM Swap Fraud**: Hijacking the victim's phone number. |
| `\btrai\b` | 30 | **Telecom Authority Impersonation**: Fake line disconnection threats. |
| `electricity.{0,15}(disconnect\|cut)` | 35 | **Utility Scam**: Demanding fake APK installation to stop power cuts. |
| `kbc.{0,10}lottery` | 40 | **Prize Scam**: Advance-fee fraud for fake winnings. |
| `paytm.{0,10}kyc` | 35 | **E-Wallet Phishing**: Fake KYC updates for digital wallets. |
| `crypto.{0,15}(invest\|return\|profit)` | 25 | **Cryptocurrency Fraud**: Fake investment returns / Pig Butchering. |

---

## 3. Operational Consistency & Stability

All models and regex matchers strictly adhere to the resource constraints of `ScamDetectionService`:
- **Zero-Allocation**: Inference operations utilize pre-allocated primitive arrays (`FloatArray`, `ShortArray`) and `ConcurrentLinkedQueue` pools.
- **Hardware Acceleration**: Matrix operations are implicitly routed to the NPU/GPU via Android NNAPI delegates where supported.
- **Teardown**: All C++ memory scopes are rigorously closed using a synchronized `try-finally` block during Service `onDestroy()`, guaranteeing no memory leaks upon application termination or OS eviction.
