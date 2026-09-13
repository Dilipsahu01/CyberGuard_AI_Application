# CyberGuard AI: Model Input/Output & Retraining Architecture

This document outlines the exact input/output pipelines and the post-training security protocols for the AI models running natively on-device.

---

## 1. Voice Activity Detection (VAD) - Silero VAD (ONNX)
**Role:** Battery saver and audio slicer. Determines if a human is speaking.

* **Sample Rate:** Strictly `16,000 Hz`.
* **Output:** Float32 Probability (Threshold > 0.5f).

---

## 2. Speech-to-Text (ASR) - Sherpa-ONNX Fast Conformer CTC
**Role:** Transcribes speech into Hinglish/English text in real-time.

* **Quantization:** **INT8** strictly required for memory footprint.
* **Security:** Model weights are AES-256 encrypted at rest. They are decrypted to a temporary cache, loaded into memory, and the cache is **immediately purged** to protect IP.

---

## 3. Intent NLP Vector Classifier - MiniLM-L6 (ONNX)
**Role:** Semantic intent projection for social engineering detection.

* **Dimensionality:** 384-dimensional dense vector embedding.
* **Decision Logic:** Cosine Similarity against pre-computed scam intent vectors.

---

## 4. Deterministic Engines (RegexGate & Bloom Filter)
Critical high-speed filters running before the probabilistic models.

### Keyword Gate (RegexGate)
* **Operation:** Concurrent regex matching against 19 India-specific threat patterns.

---

## 5. Security & Post-Training Cryptography (The Vault)

> [!CAUTION]
> **CRITICAL:** Do NOT drop plaintext `.onnx` files into assets. They will be rejected by the SHA-256 integrity verifier.

### 5.1 Deployment Pipeline
1. Place plaintext models in `raw_models_backup/`.
2. Run `python3 encrypt_models.py`.
3. The script generates `.enc` files and logs SHA-256 baseline hashes.
4. Update `ModelIntegrityVerifier.kt` with the new hashes.

### 5.2 Hardened Runtime Decryption
- **Hardware-Linked Dynamic Keys:** Decryption keys are derived in the JNI layer using XOR-based salt mixing with hardware identifiers.
- **RAM-Dispatched Weights:** Models are decrypted directly into RAM via `MappedByteBuffer`.
- **Immediate Disk Cleanup:** Any model that requires a physical file path (like Sherpa-ONNX) is deleted from the disk cache the instant it is loaded into the engine.
