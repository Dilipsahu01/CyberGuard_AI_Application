# CyberGuard-AI: Inference Stack & Model Specification

This document provides a centralized technical specification for all active Machine Learning models and deterministic pattern matchers utilized in the CyberGuard-AI call screening pipeline.

---

## 1. Machine Learning Models

### 1.1 Silero VAD (Voice Activity Detection)
* **Purpose**: Gating expensive NLP operations to save battery.
* **Architecture**: Recurrent Neural Network (RNN).
* **Quantization**: INT8.
* **Inference**: Executed on every 100ms audio chunk.

### 1.2 Sherpa-ONNX Fast Conformer CTC
* **Purpose**: Real-time localized Speech-to-Text (ASR) for regional accents.
* **Security**: Model weights are AES-encrypted at rest and purged from disk immediately after loading into RAM.
* **Inference**: Synchronous C++ execution via JNI.

### 1.3 MiniLM-L6 (IntentNLP Engine)
* **Purpose**: Semantic intent projection (Financial, Coercion, Urgency).
* **Architecture**: Transformer-based sequence classifier (384D embeddings).
* **Inference**: Triggered on utterance end or "Fast-Talker" threshold.

---

## 2. Deterministic Regex Threat Matrix (RegexGate)
Instantly bypasses semantic probability for known Indian threat vectors:
* **Law Enforcement**: `digital.?arrest`, `police.{0,20}coming`, `\bcbi\b`.
* **Technical Support**: `\banydesk\b`, `\bteamviewer\b`, `apk download`.
* **Financial**: `\botp\b`, `bank.{0,10}block`, `aadhar.{0,10}verify`.

---

## 3. Operational Consistency & Security

### 3.1 Zero-Allocation Execution
Inference operations utilize pre-allocated primitive arrays and object pools to stop Garbage Collection thrashing and thermal spikes.

### 3.2 SQLCipher Persistence
Scrubbed transcripts and risk results are persisted asynchronously into **SQLCipher-encrypted** local databases.

### 3.3 Secure Teardown
All native sessions are closed within synchronized `try-finally` blocks during Service `onDestroy()`, with a final sweep to ensure the model weight cache is purged from `noBackupFilesDir`.
