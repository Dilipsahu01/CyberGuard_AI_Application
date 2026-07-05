<div align="center">

# 🛡️ CyberGuard-AI — Bit & Flag Architecture Reference

**The definitive catalog of every bit-packed structure, state flag, and memory mapping across the entire CyberGuard-AI codebase.**

*Detailing what each bit/flag represents, its size, and the exact pipeline stage where it is activated or evaluated.*

`v1.0` · Last Updated: July 2026

---

</div>

## Table of Contents

1. [System Pipeline State Flags](#1-system-pipeline-state-flags)
2. [CallContext Bit-Packing (82 Bits)](#2-callcontext-bit-packing-82-bits)
3. [Telemetry Payload Format (75 Bits)](#3-telemetry-payload-format-75-bits)
4. [ContactMemory Bit-Packed Struct (9 Bytes / 72 Bits)](#4-contactmemory-bit-packed-struct-9-bytes--72-bits)
5. [UI Configuration Flags](#5-ui-configuration-flags)
6. [Critical FSM States, Thresholds & Memory Allocations](#6-critical-fsm-states-thresholds--memory-allocations)
7. [Extended UI Flags & Reactive Streams](#7-extended-ui-flags--reactive-streams)
8. [Low-Level Bitwise Operations](#8-low-level-bitwise-operations)
9. [AI Engine Constraints & Hardware Memory](#9-ai-engine-constraints--hardware-memory)
10. [Memory Execution Context & Concurrency Locks](#10-memory-execution-context--concurrency-locks)
11. [Persistent Database Schema (Room SQLite)](#11-persistent-database-schema-room-sqlite)
12. [RegexGate Threat Matrix (19 India-Specific Patterns)](#12-regexgate-threat-matrix-19-india-specific-patterns)
13. [System-Level Service & Android Hardware Flags](#13-system-level-service--android-hardware-flags)
14. [SharedPreference States & Intent Flags](#14-sharedpreference-states--intent-flags)

---

## 1. System Pipeline State Flags

> [!NOTE]
> These boolean flags govern the **real-time execution flow** of the system across the 7-stage architecture.

| # | Flag Name | Type | Pipeline Stage | Activation Condition / Triggers |
|:-:|:---|:---:|:---|:---|
| 1 | `speechActive` | `Boolean` | **Stage 4** — VAD Inference | Activated when `SileroVAD.isSpeech()` returns a speech probability exceeding `0.5f` on a 100ms audio chunk. Indicates human voice detected. |
| 2 | `hasVibrated` | `Boolean` | **Stage 5** — Scoring Fusion & UI | Activated exactly once when the `EnsembleEngine` risk score crosses the alert threshold. Guards the `vibrateAlert()` function to prevent continuous vibration. |
| 3 | `isScamDetected` | `Boolean` | **Stage 5** — Scoring Fusion & UI | A UI state flag activated in `ActiveCallScreen.kt` when the risk score exceeds **70/100**, triggering the Red Warning Banner and morphing the End Call button. |
| 4 | `showCaptions` | `Boolean` | **Stage 5** — UI State | Activated by the user in `ActiveCallScreen.kt` to reveal the live ASR transcript window. Mutually exclusive with `showNotes`. |
| 5 | `showNotes` | `Boolean` | **Stage 5** — UI State | Activated by the user in `ActiveCallScreen.kt` to reveal the Scam Evidence Pad. Mutually exclusive with `showCaptions`. |

---

## 2. CallContext Bit-Packing (82 Bits)

> [!IMPORTANT]
> **Source:** `CallContext.kt`
> This struct serializes **14 telemetry signals** into exactly **82 bits (10.25 bytes)** for extreme storage efficiency.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    82-BIT CALLCONTEXT WIRE FORMAT                               │
├──────────┬──┬──┬──┬──┬────────┬────────┬──────┬───────┬───────┬───────┬───────┬───────┬───────┤
│ CallerHash│BF│VP│SS│RC│DaysKnwn│TotCalls│Regex │FinInt │UrgInt │CoeInt │IntInt │TruInt │EnsScr │
│  14 bits  │1 │1 │1 │1 │ 8 bits │ 8 bits │6 bits│7 bits │7 bits │7 bits │7 bits │7 bits │7 bits │
└──────────┴──┴──┴──┴──┴────────┴────────┴──────┴───────┴───────┴───────┴───────┴───────┴───────┘
                                                                          Total = 82 bits
```

| # | Bit/Flag Name | Size | Range | Activation / Data Source |
|:-:|:---|:---:|:---|:---|
| 1 | **Caller Hash Prefix** | 14 bits | `0 – 16,383` | Derived from the SHA-256 hash of the caller's normalized E.164 phone number. |
| 2 | **Bloom Flag** | 1 bit | `0` or `1` | Activated if the number is found in the 10-million-bit Bloom Filter. |
| 3 | **VoIP Flag** | 1 bit | `0` or `1` | Activated if the carrier signals the call is routed over VoIP. |
| 4 | **STIR/SHAKEN Flag** | 1 bit | `0` or `1` | Activated if the call fails cryptographic caller ID attestation. |
| 5 | **Rapid Callback Flag** | 1 bit | `0` or `1` | Activated if the user received multiple short calls from this number. |
| 6 | **Days Known** | 8 bits | `0 – 255` | Evaluated from `ContactMemory.kt` or the local SQLite database. |
| 7 | **Total Calls** | 8 bits | `0 – 255` | Number of historic interactions with this caller. |
| 8 | **Regex Risk Score** | 6 bits | `0 – 63` | Populated in **Stage 4** by `RegexGate.check()`. |
| 9 | **Financial Intent** | 7 bits | `0 – 127` | Logit from `IntentNLP.analyze()` (Stage 4). |
| 10 | **Urgency Intent** | 7 bits | `0 – 127` | Logit from `IntentNLP.analyze()` (Stage 4). |
| 11 | **Coercion Intent** | 7 bits | `0 – 127` | Logit from `IntentNLP.analyze()` (Stage 4). |
| 12 | **Intimacy Intent** | 7 bits | `0 – 127` | Logit from `IntentNLP.analyze()` (Stage 4). |
| 13 | **Trust Intent** | 7 bits | `0 – 127` | Logit from `IntentNLP.analyze()` (Stage 4). |
| 14 | **Ensemble Score** | 7 bits | `0 – 127` | The final fused risk score from `EnsembleEngine.calculate()` (Stage 5). |

---

## 3. Telemetry Payload Format (75 Bits)

> [!IMPORTANT]
> **Source:** `SYSTEM_ARCHITECTURAL_WORKFLOW.md` (Stage 6)
> This is the highly optimized payload format for sending data to the **Swarm Network** over **5G URLLC** (DSCP `0xB8`) or via SMS fallback.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    75-BIT TELEMETRY WIRE FORMAT                                 │
├───────┬───────┬──────────┬──────┬──────┬───────┬───────┬───────┬───────┬───────┬───────┬──┤
│FmtHdr │RiskScr│CallDuratn│ RSRP │ SINR │NetType│UrgLgt │FinLgt │CoeLgt │IntLgt │TruLgt │BK│
│3 bits │7 bits │ 14 bits  │6 bits│6 bits│3 bits │7 bits │7 bits │7 bits │7 bits │7 bits │1 │
└───────┴───────┴──────────┴──────┴──────┴───────┴───────┴───────┴───────┴───────┴───────┴──┘
                                                                          Total = 75 bits
```

| # | Bit/Flag Name | Size | Range / Values | Activation / Data Source |
|:-:|:---|:---:|:---|:---|
| 1 | **Format Header** | 3 bits | `0 – 7` | Hardcoded format version indicator. |
| 2 | **Risk Score** | 7 bits | `0 – 100` | Final unified scam risk score (Stage 5). |
| 3 | **Call Duration** | 14 bits | `0 – 16,383 sec` | Measured when the call ends (Stage 6). |
| 4 | **5G RSRP** | 6 bits | `-140 to -44 dBm` | Captured from Android `TelephonyManager` (Stage 6). |
| 5 | **5G SINR** | 6 bits | `-20 to 40 dB` | Captured from Android `TelephonyManager` (Stage 6). |
| 6 | **Network Type** | 3 bits | `0 – 7` | LTE, 5G, Wi-Fi, or Offline. |
| 7 | **Urgency Logit** | 7 bits | `0 – 100` | NLP projection (Stage 4). |
| 8 | **Financial Logit** | 7 bits | `0 – 100` | NLP projection (Stage 4). |
| 9 | **Coercion Logit** | 7 bits | `0 – 100` | NLP projection (Stage 4). |
| 10 | **Intimacy Logit** | 7 bits | `0 – 100` | NLP projection (Stage 4). |
| 11 | **Trust Logit** | 7 bits | `0 – 100` | NLP projection (Stage 4). |
| 12 | **Blocked State** | 1 bit | `0` or `1` | Activated (`1`) if the call was flagged and terminated by the AI. |

---

## 4. ContactMemory Bit-Packed Struct (9 Bytes / 72 Bits)

> [!IMPORTANT]
> **Source:** `ContactMemory.kt`
> Tracks **8 emotional/historical metrics** over time for long-term **"Pig Butchering" (Romance Scam)** detection.

### Byte 0 — Boolean Flags (7 × 1-bit)

| Bit | Flag Name | Description |
|:---:|:---|:---|
| 0 | `metInPerson` | Has the user physically met this caller? |
| 1 | `videoCalled` | Has a verified video call occurred? |
| 2 | `askedMoney` | Has the caller previously asked for money? |
| 3 | `askedOtp` | Has the caller asked for an OTP? |
| 4 | `sharedDocs` | Have documents been shared? |
| 5 | `urgencyUsed` | Has the caller historically used high urgency? |
| 6 | `secrecyAsked` | Has the caller requested secrecy? |

### Bytes 1–8 — Data Metrics

| Byte(s) | Field | Encoding | Range |
|:---:|:---|:---|:---|
| 1 | `Trust` \| `Intimacy` | 3 bits + 3 bits (bit-packed nibbles) | `0–7` each |
| 2 | `Emotional Intensity` \| `Platform` | 3 bits + 3 bits (bit-packed nibbles) | `0–7` each |
| 3 | `Days Known` | 8 bits | `0–255` |
| 4 | `Total Calls` | 8 bits | `0–255` |
| 5–6 | `Money Requested` | 16 bits (Little Endian) | `0–65,535` |
| 7 | `Last Contact Ago` | 8 bits | `0–255` |
| 8 | `Romance Score` | 8 bits | `0–100` |

> [!WARNING]
> **`Romance Score`** is evaluated in **Stage 5**. It triggers a **massive risk modifier** if the value exceeds **40**.

---

## 5. UI Configuration Flags

> [!NOTE]
> Located across the `app/src/main/java/com/example/ui/` directory Jetpack Compose screens. These boolean flags control user preferences and component visibility.

| # | Flag Name | Type | Screen | Default | Activation Condition / Triggers |
|:-:|:---|:---:|:---|:---:|:---|
| 1 | `deepfakeProtection` | `Boolean` | `Advancedsettingsscreen.kt` | `true` | User toggle for acoustic deepfake detection. |
| 2 | `intentNlpAnalysis` | `Boolean` | `Advancedsettingsscreen.kt` | `true` | User toggle for semantic NLP analysis. |
| 3 | `swarmIntelligence` | `Boolean` | `Advancedsettingsscreen.kt` | `true` | User toggle to opt into telemetry reporting. |
| 4 | `darkTheme` | `Boolean` | `Advancedsettingsscreen.kt` | `false` | User toggle to force dark mode. |
| 5 | `isWhitelisted` | `Boolean` | `WhitelistScreen.kt` | — | Toggled per-contact by the user. If `true`, the pipeline bypasses AI processing to save CPU/Battery. |

---

## 6. Critical FSM States, Thresholds & Memory Allocations

Beyond simple booleans and bit-fields, the application relies on specific memory-mapped bits and state machine phases that are crucial for the AI's success.

### 6.1 Bloom Filter (`BitSet`)

| Property | Value |
|:---|:---|
| **Size** | `10,000,000` bits (10 Million) |
| **Memory Footprint** | ~1.22 MB fixed allocation |
| **Hash Strategy** | Kirsch-Mitzenmacher double-hashing (`MurmurHash3` + `xxHash32`) |
| **Activation Stage** | **Stage 4** — checked before NLP processing |
| **Time Complexity** | `O(1)` per lookup |
| **False Positive Rate** | ~`0.00009%` |

### 6.2 ArcTracker FSM (Finite State Machine)

The `ArcTracker` evaluates the **psychological "arc"** of the call. Transitions through the following string states, evaluated in **Stage 4**:

```
┌─────────┐     ┌─────────────┐     ┌───────────────────┐     ┌─────────┐     ┌───────┐
│  INTRO  │ ──▶ │ TRUST_BUILD │ ──▶ │ PROBLEM_ESTABLISH │ ──▶ │ REQUEST │ ──▶ │ CLOSE │
└─────────┘     └─────────────┘     └───────────────────┘     └─────────┘     └───────┘
```

**Topic Vocabulary:** 10 categories (~170 keywords total)
> Examples: `authority_claim`, `financial_request`, `urgency_signal`

### 6.3 Audio Slicing Thresholds (Stage 3 & 4)

| Parameter | Value | Description |
|:---|:---:|:---|
| **Hardware Window** | `48,000` samples | 3 seconds of PCM-16BIT audio |
| **AI Inference Slice** | `512` samples | 32 ms |
| **VAD Chunk** | `1,600` samples | 100 ms |
| **Silence Threshold** | `≥ 4` consecutive chunks | Triggered after ~400ms of human silence |
| **NLP Sliding Window** | `100` words max | Protects the MiniLM-L6 model from token overflow |

---

## 7. Extended UI Flags & Reactive Streams

### 7.1 Jetpack Compose UI State (`mutableStateOf`)

These variables govern the dynamic visual states across the UI screens:

| Variable(s) | Type | Screen |
|:---|:---:|:---|
| `notesText` | `String` | ActiveCallScreen |
| `visibleSteps` | `List` | SplashScreen |
| `dialedNumber` | `String` | DialerScreen |
| `selectedTab` | `Int` | CallRecordingsScreen & CallLogsScreen |
| `score`, `transcript`, `hitWord`, `callerNumber`, `activeStage`, `isScamScenario`, `isAnswered` | Various | IncomingCallActivity |
| `isSimulating`, `activeCaller`, `liveTranscript`, `liveHitKeyword` | Various | PipelineViewModel |
| `autoHangup`, `swarmServerUrl`, `enableSmsFallback`, `smsFallbackNumber`, `whitelistInput`, `alertThreshold` | Various | Advancedsettingsscreen.kt |
| `selectedLogForDialog`, `number` | Various | MainActivity |

### 7.2 Coroutine StateFlow Streams

Thread-safe asynchronous reactive streams:

| Stream (Private / Public) | Type | Source |
|:---|:---:|:---|
| `_scoreFlow` / `scoreFlow` | `Int` | ScamDetectionService |
| `_transcriptFlow` / `transcriptFlow` | `String` | ScamDetectionService |
| `_statusFlow` / `statusFlow` | `String` | ScamDetectionService |
| `allLogs` / `scamLogs` | `List<CallLog>` | PipelineViewModel |

---

## 8. Low-Level Bitwise Operations

> [!NOTE]
> Specific memory and hash manipulation operations executed **natively on the device**.

### 8.1 Kirsch-Mitzenmacher Hashing — `BloomFilter.kt`

Extensive execution of bitwise operators for MurmurHash3 / xxHash32:

```kotlin
// Example operations:
(k1 shl 15) or (k1 ushr 17)
((v1 shl 13) or (v1 ushr 19)) and 0xFFFFFFFFL
```

Operators used: `shl` (shift left), `ushr` (unsigned shift right), `xor`, `and`

### 8.2 82-Bit Serialization — `CallContext.kt`

Bit-packing via dynamic mask boundaries:

```kotlin
// Packing:
shr bitsToPut
(v and mask) shl bitIndex

// Mask generation:
val mask = ((1 shl bitsToPut) - 1)
```

### 8.3 9-Byte Packing — `ContactMemory.kt`

Byte-level masking and Little-Endian stitching:

```kotlin
// Nibble write:
(memoryData[1].toInt() and 0x38.inv()) or (v shl 3)

// 16-bit Little-Endian read:
(memoryData[5].toInt() and 0xFF) or ((memoryData[6].toInt() and 0xFF) shl 8)
```

---

## 9. AI Engine Constraints & Hardware Memory

| Constraint | Detail |
|:---|:---|
| **Configurable Threshold** | `alertThreshold` (default: `70`) — located in `Advancedsettingsscreen.kt`, governs the `isScamDanger` boolean |
| **Quantization** | `Sherpa-ONNX Fast Conformer CTC` operates with **INT8 quantization**, constrained to ~40 MB |
| **ABI Stripping** | ONNX runtime native library (`libonnxruntime.so`) restricted solely to `arm64-v8a`, shedding ~30 MB of multi-ABI size bloat |
| **Inference Latency Target** | MiniLM-L6 inference operates within a strict window of **~17ms** per pass |
| **Singleton Byte Caching** | `env.createSession(buffer)` loads model bytes exactly once into a singleton byte-array cache to avoid repeated disk I/O |

---

## 10. Memory Execution Context & Concurrency Locks

| Mechanism | Detail |
|:---|:---|
| **Hardware Scheduling** | `THREAD_PRIORITY_AUDIO` (Priority 8) forces the OS to schedule audio capture ahead of standard threads in `ScamDetectionService.kt` |
| **Direct Memory Access** | `fileChannel.map(FileChannel.MapMode.READ_ONLY)` creates shared `MappedByteBuffer` references for AI models |
| **Thread Safety — RegexGate** | `synchronized(this)` guards the pattern-matching state machine in `RegexGate.check()` against `ConcurrentModificationException` |
| **Thread Safety — ONNX Env** | `@Volatile` + `synchronized` used for instantiating the ONNX Environment singletons globally |
| **Coroutines & Battery** | `WakeLock` usage during background capture, and `SupervisorJob` for structured concurrency |

---

## 11. Persistent Database Schema (Room SQLite)

> [!NOTE]
> Flags and structures backing the UI and Telemetry queue via Room.

### `CallLog` Entity

| Column | Type | Description |
|:---|:---:|:---|
| `isScam` | `Boolean` | Whether the call was classified as a scam |
| `wasBlocked` | `Boolean` | Whether the call was auto-blocked |

### `TelemetryQueueItem` Entity

| Column | Type | Description |
|:---|:---:|:---|
| `callerHash` | `String` | Hashed caller identifier |
| `score` | `Int` | Risk score |
| `transcript` | `String` | Captured transcript |
| `intentScores` | `JSON String` | Serialized NLP intent scores |
| `timestamp` | `Long` | Epoch timestamp |

### `PendingSwarmReport` Entity

| Column | Type | Description |
|:---|:---:|:---|
| `timestamp` | `Long` | Epoch timestamp |
| `payload` | `ByteArray` | Serialized telemetry payload |
| `callerNumber` | `String` | Originating phone number |

---

## 12. RegexGate Threat Matrix (19 India-Specific Patterns)

> [!CAUTION]
> Compiled `Regex` rules operating instantly as a **deterministic kill-switch** in **Stage 4**.

| # | Pattern | Weight |
|:-:|:---|:---:|
| 1 | `\botp\b` | **35** |
| 2 | `digital.?arrest` | **40** |
| 3 | `\banydesk\b` | **35** |
| 4 | `police.{0,20}coming` | **30** |
| 5 | `account.{0,20}(freeze\|block\|suspend)` | **25** |
| 6 | `\bcbi\b` | **30** |
| 7 | `(wire\|transfer).{0,20}(money\|funds\|rupee)` | **35** |
| 8 | `verify.{0,20}(account\|identity\|aadhar\|pan)` | **20** |
| 9 | `\bgift.?card\b` | **30** |
| 10 | `(share\|send).{0,10}(pin\|password\|cvv)` | **40** |
| 11 | `(aadhaar\|aadhar).{0,20}(number\|link\|otp)` | **35** |
| 12 | `\bteamviewer\b` | **35** |
| 13 | `\b(fedex\|customs).{0,20}(parcel\|package\|duty)\b` | **35** |
| 14 | `sim.{0,10}(block\|deactivate\|upgrade)` | **30** |
| 15 | `\btrai\b` | **30** |
| 16 | `electricity.{0,15}(disconnect\|cut)` | **35** |
| 17 | `kbc.{0,10}lottery` | **40** |
| 18 | `paytm.{0,10}kyc` | **35** |
| 19 | `crypto.{0,15}(invest\|return\|profit)` | **25** |

---

## 13. System-Level Service & Android Hardware Flags

Additional low-level constants governing the service lifecycle and hardware interfacing.

### Audio Format Constants

| Constant | Usage |
|:---|:---|
| `AudioFormat.CHANNEL_IN_MONO` | Mono channel input for audio capture |
| `AudioFormat.ENCODING_PCM_16BIT` | 16-bit PCM encoding for raw audio |

### Service Lifecycle

| Flag | Detail |
|:---|:---|
| `START_NOT_STICKY` | Used in `ScamDetectionService.kt` to ensure the background recording service strictly halts if killed (prevents silent zombie recording). |
| `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE` | Explicitly required for Android 14+ background mic access. |

---

## 14. SharedPreference States & Intent Flags

| Key | Type | Description |
|:---|:---:|:---|
| `whitelist` | `StringSet` | Set of whitelisted phone numbers |
| `trusted_contacts` | `StringSet` | Set of trusted contact identifiers |
| `dark_mode` | `Boolean` | UI preference overriding system defaults |
| `is_scam_scenario` | `Intent Boolean Extra` | Debug/Demo control — toggles mocked environments |

---

<div align="center">

*📄 This document serves as the single source-of-truth reference for all binary structures, state flags, and memory mappings in CyberGuard-AI.*

</div>
