# CyberGuard AI: Model Input/Output & Retraining Architecture

This document outlines the exact input/output pipelines and the security protocols for swapping or fine-tuning the Machine Learning models.

---

## 1. Intent NLP Classifier - MiniLM-L6 (ONNX)
**Role:** Semantic analysis of Hinglish scam patterns.

* **Retraining Pipeline:**
  1. Fine-tune `all-MiniLM-L6-v2` on real-world Indian scam transcripts.
  2. Perform **INT8 Dynamic Quantization**.
  3. Export to `.ort` format (Opset 14+).
  4. Encrypt via `encrypt_models.py` before deployment.

---

## 2. Speech-to-Text (ASR) - Sherpa-ONNX Conformer
**Role:** Real-time localized transcription.

* **Retraining Pipeline:**
  1. Fine-tune Conformer CTC using regional Indian accent datasets.
  2. Optimize for **Hinglish** vocabulary ("Aadhar", "KYC").
  3. Ensure INT8 quantization to maintain the <145MB memory footprint.

---

## 3. Post-Training Security Protocols

> [!IMPORTANT]
> To maintain the integrity of "The Vault," any model update must pass the **Security CI/CD Gate**:

1. **Integrity Check**: Generate the SHA-256 hash of the final `.ort` or `.onnx` file.
2. **Encryption**: AES-256 GCM encrypt the file using the project's master key.
3. **Hardening Update**: Update `ModelIntegrityVerifier.kt` with the new SHA-256 baseline hash.
4. **Cache Purge Verification**: Confirm that the runtime successfully purges the model weights from disk immediately after loading.

---

## 4. Hardware Delegation (NNAPI)
All retrained models must support **Android NNAPI** operations. If a custom layer is used that is not supported by NNAPI, the model will fallback to CPU execution, which may increase latency beyond the 100ms real-time audio budget.
