#  CyberGuard AI: Booleans and Flags Inventory

This document provides a comprehensive inventory of all boolean flags, bit-packed structures, telemetry bitmasks, and binary status toggles discovered across the CyberGuard AI codebase.

## 📊 Summary Statistics

| Category | Quantity |
| :--- | :--- |
| **Total Booleans Discovered** | 56 |
| **Bit-Packed Fields (Individual)** | 22 |
| **Total Binary Indicators** | **78** |

---

## 🏗 Bit-Packed Structures

### 1. ContactMemory (Romance Scam History)
**File:** `app/src/main/java/com/example/models/ContactMemory.kt`
**Size:** 9 Bytes (72 bits)

| Exact Name | Type / Size | Purpose & Behavior |
| :--- | :--- | :--- |
| `metInPerson` | Bit 0 (Byte 0) | Indicates if the user has physically met this caller. |
| `videoCalled` | Bit 1 (Byte 0) | Indicates if a verified video call has occurred. |
| `askedMoney` | Bit 2 (Byte 0) | Indicates if the caller has previously asked for money. |
| `askedOtp` | Bit 3 (Byte 0) | Indicates if the caller has asked for an OTP. |
| `sharedDocs` | Bit 4 (Byte 0) | Indicates if documents have been shared between parties. |
| `urgencyUsed` | Bit 5 (Byte 0) | Indicates if the caller historically used high urgency. |
| `secrecyAsked` | Bit 6 (Byte 0) | Indicates if the caller has requested secrecy. |
| `trustLevel` | 3 Bits (Byte 1) | Encodes a trust scale from 0–7. |
| `intimacyLevel` | 3 Bits (Byte 1) | Encodes an intimacy scale from 0–7. |
| `emotionalIntensity`| 3 Bits (Byte 2) | Encodes emotional intensity of interactions from 0–7. |
| `contactPlatform` | 3 Bits (Byte 2) | Encodes the platform of interaction (e.g., dating app, social media). |
| `romanceScore` | 8 Bits (Byte 8) | Cached romance scam risk score (0–100). |

### 2. CallContext (Swarm Telemetry Payload)
**File:** `app/src/main/java/com/example/models/CallContext.kt`
**Size:** 10.25 Bytes (82 bits)

| Exact Name | Type / Size | Purpose & Behavior |
| :--- | :--- | :--- |
| `bloomHit` | 1 Bit | Status flag indicating a match in the 10-million-bit Bloom Filter. |
| `isVoip` | 1 Bit | Status flag indicating if the call is routed over VoIP. |
| `stirShakenFailed` | 1 Bit | Status flag indicating if cryptographic caller ID attestation failed. |
| `rapidCallback` | 1 Bit | Status flag indicating multiple short calls from the same number. |
| `daysKnown` | 8 Bits | Number of days since first interaction (0–255). |
| `totalCalls` | 8 Bits | Total number of interactions with this caller (0–255). |
| `regexScore` | 6 Bits | Score from deterministic regex matching (0–63). |
| `nlpIntents` | 35 Bits | 5 x 7-bit logits representing semantic threat vectors. |
| `ensembleScore` | 7 Bits | Final fused risk score (0–127). |

---

## 🔘 Boolean Flags and State Toggles

### Core Pipeline & AI Models

| Exact Name | Class / File | Purpose & Behavior |
| :--- | :--- | :--- |
| `isModelLoaded` | `IntentNLP` | Indicates if the MiniLM ONNX model is initialized in memory. |
| `isModelLoaded` | `SileroVAD` | Indicates if the VAD model is initialized. |
| `speechActive` | `SileroVAD` | True when the VAD engine detects human voice in the current chunk. |
| `utteranceEnded` | `SileroVAD` | Latched flag indicating a speech segment has concluded. |
| `isInitialized` | `BloomFilter` | Companion object flag ensuring BitSet singleton is ready. |
| `thermalThrottling` | `PipelineManager` | Active when ADPF signals heat > 85%, degrading NLP precision. |
| `skipNLP` | `PipelineManager` | Dynamic toggle to bypass heavy LLM inference during thermal stress. |
| `isRoboVoice` | `RiskResult` | Result flag for acoustic deepfake artifact detection. |

### Foreground Service & Telemetry

| Exact Name | Class / File | Purpose & Behavior |
| :--- | :--- | :--- |
| `isRecording` | `ScamDetectionService` | Global toggle for the background AudioRecord loop. |
| `hasMicPermission` | `ScamDetectionService` | Verification flag for system RECORD_AUDIO access. |
| `isEmergency` | `ScamDetectionService` | Local flag to bypass AI for SOS numbers (112, 911). |
| `success` | `SwarmReporter` | Result flag for telemetry HTTP transmission. |
| `isNetworkAvailable`| `SwarmReporter` | Connectivity check before attempting telemetry sync. |
| `receiverRegistered`| `CyberGuardInCallService` | Status flag for the Telecom disconnect BroadcastReceiver. |

### UI & UX State

| Exact Name | Class / File | Purpose & Behavior |
| :--- | :--- | :--- |
| `isScamDetected` | `ActiveCallScreen` | UI state triggering the Red Warning Banner and End Call morph. |
| `isIncoming` | `ActiveCallScreen` | Distinguishes between incoming and outgoing call UI layouts. |
| `showCaptions` | `ActiveCallLayout` | User toggle for the Live AI Transcript window. |
| `showNotes` | `ActiveCallLayout` | User toggle for the Scam Evidence Pad. |
| `isMuted` | `ActiveCallLayout` | UI toggle for microphone muting status. |
| `isSpeakerOn` | `ActiveCallLayout` | UI toggle for audio route (Speaker vs Earpiece). |
| `isOnHold` | `ActiveCallLayout` | UI toggle for active call suspension. |
| `showKeypad` | `ActiveCallLayout` | User toggle for DTMF dialpad overlay. |
| `expanded` | `CallLogsScreen` | UI state for expanding individual call log details. |
| `darkTheme` | `AdvancedSettings` | Preference toggle for forced Dark Mode. |
| `deepfakeProtection`| `AdvancedSettings` | User toggle to enable/disable acoustic RNN monitoring. |
| `intentNlpAnalysis` | `AdvancedSettings` | User toggle for high-compute semantic evaluation. |
| `swarmIntelligence` | `AdvancedSettings` | Opt-in toggle for anonymous threat telemetry sharing. |
| `hasVibrated` | `IncomingCallActivity`| Latch ensuring haptic alert fires exactly once per detection. |
| `isScamScenario` | `IncomingCallActivity`| Intent extra used for simulated/demo scam detections. |

### Security & Compliance

| Exact Name | Class / File | Purpose & Behavior |
| :--- | :--- | :--- |
| `isDeviceCompromised`| `EnvironmentGuard` | Result of root-detection (su binaries, test-keys, rw mounts). |
| `isEmergencyGuardian`| `LocalContactEntity` | Room flag marking trusted contacts that bypass AI analysis. |
| `isScam` | `CallLog` | Persistence flag indicating if a past call was malicious. |
| `wasBlocked` | `CallLog` | Persistence flag indicating if the AI autonomously ended the call. |
| `isFlagged` | `ScamCallEntity` | DB flag for severe threat detection history. |
| `isRequired` | `PermissionSetup` | UI flag distinguishing mandatory vs optional permissions. |

---

## 🛠 Low-Level Binary Hooks (C++)

**File:** `app/src/main/cpp/secrets.cpp`

| Exact Name | Type / Size | Purpose & Behavior |
| :--- | :--- | :--- |
| `salt_len > 0` | Comparison Flag | Guard check before performing XOR mix-in for dynamic key derivation. |
| `base_key[i] ^ salt_ptr`| Bitwise XOR | Runtime binary operation to derive the master decryption key per hardware. |
