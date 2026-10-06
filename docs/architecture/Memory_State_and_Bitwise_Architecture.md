#  CyberGuard-AI — Bit & Flag Architecture Reference

**The definitive catalog of every bit-packed structure, state flag, and memory mapping across the entire CyberGuard-AI codebase with hardened security updates.**

`v1.1` · Last Updated: July 2026

---

## 1. System Pipeline State Flags

| # | Flag Name | Type | Pipeline Stage | Activation Condition / Triggers |
|:-:|:---|:---:|:---|:---|
| 1 | `speechActive` | `Boolean` | **Stage 3** — VAD Inference | Activated when `SileroVAD.isSpeech()` returns probability > 0.5f. |
| 2 | `hasVibrated` | `Boolean` | **Stage 5** — Scoring | Activated exactly once when score crosses alert threshold. |
| 3 | `isScamDetected` | `Boolean` | **Stage 5** — Scoring | UI state activated when risk score exceeds 70/100. |

---

## 2. CallContext Bit-Packing (82 Bits)

> [!IMPORTANT]
> **Source:** `CallContext.kt`
> Enforced as the **exclusive** telemetry format for Swarm intelligence.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    82-BIT CALLCONTEXT WIRE FORMAT                               │
├──────────┬──┬──┬──┬──┬────────┬────────┬──────┬───────┬───────┬───────┬───────┬───────┬───────┤
│ CallerHash│BF│VP│SS│RC│DaysKnwn│TotCalls│Regex │FinInt │UrgInt │CoeInt │IntInt │TruInt │EnsScr │
│  14 bits  │1 │1 │1 │1 │ 8 bits │ 8 bits │6 bits│7 bits │7 bits │7 bits │7 bits │7 bits │7 bits │
└──────────┴──┴──┴──┴──┴────────┴────────┴──────┴───────┴───────┴───────┴───────┴───────┴───────┘
                                                                          Total = 82 bits
```

---

## 3. Privacy Compliance (DPDP Act) Structures

### 3.1 PII Masking Patterns (`TranscriptScrubber.kt`)
Local transcripts are scrubbed before storage using the following masking proxies:

| Entity | Pattern / Logic | Mask Result |
|:---|:---|:---|
| **OTP** | `\b\d{4,8}\b` | `[OTP]` |
| **Account ID** | `\b\d{10,16}\b` | `[ACCOUNT_ID]` |
| **Name (Hinglish)** | `(my name is|mera naam|i am)\s+([a-zA-Z]+)` | `[NAME]` |

---

## 4. Persistent Database Schema (SQLCipher Encrypted)

> [!NOTE]
> All databases listed below are encrypted at rest using **SQLCipher (AES-256)** with hardware-backed passphrases.

### `CallLog` Entity (Masked)
| Column | Type | Description |
|:---|:---:|:---|
| `riskScore` | `Int` | Fused risk score (0-100) |
| `transcript` | `String` | **PII-Scrubbed** conversation text |
| `wasBlocked` | `Boolean` | Indicates autonomous termination status |

---

## 5. Security & Cryptographic Hooks

### 5.1 Dynamic Key Derivation (NDK)
- **Base Key**: Hidden in `secrets.cpp` via char array obfuscation.
- **Salt**: XOR mixed at runtime using `Build.FINGERPRINT` and other hardware identifiers.
- **Purpose**: Prevents static extraction of the master decryption key from the ELF binary.

### 5.2 OkHttp Certificate Pinning
- **Domain**: `api.cyberguard-ai.com`
- **Mechanism**: `CertificatePinner` with SHA-256 hashes of the production certificate chain.
- **Purpose**: Hardening against Man-in-the-Middle (MITM) via malicious CA certificates.

---

## 6. AI Engine Constraints & Hardware Memory

| Constraint | Detail |
|:---|:---|
| **SQLCipher Latency** | Async `Dispatchers.IO` writes to avoid UI stutters (~18ms penalty) |
| **Model Cache Purge** | Immediate deletion of `model.int8.onnx` from `noBackupFilesDir` after engine load |
| **Keystore Policy** | Keys are imported with `setRandomizedEncryptionRequired(false)` to allow explicit IV handling |
| **ABI Target** | Strictly `arm64-v8a` to ensure NPU compatibility and lean binary size |
