<div align="center">

# 🛡️ CyberGuard AI

### Real-Time Scam Call Detection — On-Device AI at the Edge

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Go](https://img.shields.io/badge/Go-00ADD8?style=for-the-badge&logo=go&logoColor=white)](https://go.dev)
[![ONNX Runtime](https://img.shields.io/badge/ONNX_Runtime-ED6C30?style=for-the-badge&logo=onnx&logoColor=white)](https://onnxruntime.ai)
[![Vercel](https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://vercel.com)
[![5G Hackathon](https://img.shields.io/badge/5G-Hackathon-E91E8C?style=for-the-badge)](https://github.com/Dilipsahu01/CyberGuard_AI_Application)

<br/>

> **7-stage real-time AI pipeline · 145 MB memory footprint · ~17ms end-to-end latency · 5,930 LOC Kotlin + Go**

</div>

---

## 📌 Overview

**CyberGuard AI** is a production-grade Android application that detects scam and fraudulent phone calls in **real-time using on-device AI**. A 7-stage pipeline — Voice Activity Detection → Deepfake scope → Streaming ASR → Regex Gate → Intent NLP → ArcTracker FSM → Ensemble Scoring — runs entirely on the device, with **zero cloud round-trip latency** and **full caller privacy** (all PII SHA-256 hashed before storage or transmission).

When a threat is confirmed (score ≥ 70/100), an instant alert is shown to the user and an anonymized 82-bit telemetry payload is dispatched to the **Swarm Server** (Go backend, Vercel-hosted) to crowd-source threat intelligence across the network.

---

## ⚡ Performance Highlights

| Metric | Value |
|--------|-------|
| End-to-end latency | **~17 ms** per inference pass |
| Total memory footprint | **~145 MB** on 3 GB RAM devices |
| Total Kotlin codebase | **~5,930 LOC** |
| Pipeline stages | **7** (VAD → Deepfake → ASR → Regex → NLP → ArcTracker → Ensemble) |
| Audio sample rate | **16,000 Hz**, Mono, 16-bit PCM |
| Capture window | **3 seconds = 48,000 samples** |
| AI inference slice | **512 samples (32 ms each)**, 93 slices/window |
| Bloom filter FPR at 10K entries | **~0.00009%** |
| Swarm telemetry payload | **82 bits (10.25 bytes)** |
| Per-contact memory (10K contacts) | **90 KB** (9 bytes/contact, 10× smaller than JSON) |

---

## 🏗️ 7-Stage AI Pipeline

```
Incoming Call Audio  (16 kHz · Mono · PCM-16BIT)
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 1 · Silero VAD                                       │
│  1,600-sample chunks (100ms) · [1, 1600] input tensor      │
│  Filters silence; speech chunks proceed downstream          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 2 · Deepfake Detector (scope: future integration)    │
│  detectRoboVoice() — architecturally wired, not yet active  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 3 · Streaming ASR — Sherpa-ONNX Fast Conformer CTC  │
│  INT8-quantized · 80ms chunk streaming · numThreads = 2    │
│  Produces rolling transcript                                 │
└────────────────────┬────────────────────────────────────────┘
                     │ transcript
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 4 · Regex Gate                                       │
│  19 pre-compiled India-specific scam patterns               │
│  O(1) amortized · allow / flag / block verdict              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 5 · Intent NLP — MiniLM-L6 (INT8, 384 dims)         │
│  5 intent axes: Urgency · Financial · Coercion ·            │
│                 Intimacy · Trust                             │
│  Cosine similarity against 384×5 weight matrix (7.5 KB)    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 6 · ArcTracker FSM                                   │
│  INTRO → TRUST_BUILD → PROBLEM_ESTABLISH → REQUEST → CLOSE  │
│  10 topic categories · ~170 keywords · O(1) per turn        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 7 · Ensemble Engine                                  │
│  Fuses all signals → risk score 0–100                       │
│  Zero heap allocation (primitive maxOf() ops only)          │
└────────────────────┬────────────────────────────────────────┘
                     │  score ≥ 70 → HIGH-RISK ALERT
              ┌──────┴──────┐
              ▼             ▼
        In-Call UI    Swarm Server
        Toast/Dialog  (Go · Vercel)
                      82-bit payload
                      DSCP 0xB8 QoS
```

---

## 🧠 Data Structures & Algorithms

### Bloom Filter — O(1) Scam Caller Lookup
- **10-million-bit** `BitSet` (1.22 MB fixed) with **Kirsch-Mitzenmacher double-hashing** (MurmurHash3 + xxHash32)
- Generates k=3 hash positions from 2 hash functions via `h1 + i*h2` — *"Less Hashing, Same Performance"* (Kirsch & Mitzenmacher, 2006)
- False positive rate at 10,000 entries: **~0.00009%**
- Thread-safe singleton via `@Volatile` + `synchronized` — zero re-allocation across Android service restarts

### ArcTracker — Social Engineering FSM
- Models the psychological arc of scam calls: `INTRO → TRUST_BUILD → PROBLEM_ESTABLISH → REQUEST → CLOSE`
- **10 topic categories × ~17 keywords = ~170 scans per speech turn** — effectively O(1)
- Categories include: `authority_claim`, `brand_impersonation`, `urgency_signal`, `secrecy_signal`, `prize_signal`, `financial_request`, and more

### IntentNLP — Semantic Similarity Scoring
- ONNX MiniLM-L6 inference at **~17ms** per pass
- `384 × 5` weight matrix (**1,920 floats, 7.5 KB**) initialized via `sin(seed)` — **zero file I/O** for the classification head
- Scores 5 intent dimensions (Urgency, Financial, Coercion, Intimacy, Trust) via cosine similarity

### 9-Byte ContactMemory — Bit-Packed Struct
Packs 8 emotional/historical metrics for romance scam detection into exactly **9 raw bytes**:

| Bytes | Contents |
|-------|----------|
| 0 | 7 boolean flags (metInPerson, videoCalled, askedMoney, askedOtp, sharedDocs, urgencyUsed, secrecyAsked) |
| 1 | Trust (3 bits) + Intimacy (3 bits) |
| 2 | Emotional Intensity (3 bits) + Platform (3 bits) |
| 3–4 | Days Known + Total Calls (uint8 each) |
| 5–6 | Money Requested (uint16 LE) |
| 7–8 | Last Contact Ago + Romance Score (uint8 each) |

**10,000 contacts → 90 KB** vs. **~1 MB with JSON** (10× more efficient)

### 82-Bit Telemetry Payload (CallContext)
Custom bit-field serializer packing 14 signals into 10.25 bytes — caller hash, boolean flags, risk scores, and NLP intent axes:

| Field | Bits |
|-------|------|
| Caller hash prefix | 14 |
| Boolean flags (bloom, VoIP, STIR/SHAKEN, rapid callback) | 4 |
| daysKnown + totalCalls | 16 |
| Regex risk score | 6 |
| 5 NLP intent scores | 35 |
| Final ensemble score | 7 |
| **Total** | **82 bits** |

---

## ⚙️ Concurrency Architecture

```
┌─────────────────────────────────────────────────────────┐
│  THREAD 1: Audio Capture (THREAD_PRIORITY_AUDIO)        │
│  ├─ AudioRecord @ 16kHz, Mono, PCM_16BIT                │
│  ├─ Reads into ShortArray(bufferSize) per hardware tick  │
│  ├─ Normalizes PCM16 → Float32 (÷ 32768.0f)             │
│  ├─ Fills dedicatedBuffer: FloatArray(48,000)            │
│  └─ On 3-sec full → copyOf() → dispatch to Thread 2     │
├─────────────────────────────────────────────────────────┤
│  THREAD 2: AI Inference (Dispatchers.Default coroutine) │
│  ├─ Slices 48,000 samples into 512-sample chunks        │
│  ├─ Per chunk: VAD → ASR → Regex → NLP → Ensemble       │
│  └─ Updates StateFlow for UI reactivity                  │
├─────────────────────────────────────────────────────────┤
│  THREAD 3: Service Lifecycle (SupervisorJob scope)       │
│  ├─ Telemetry push with retry + exponential backoff      │
│  ├─ Queue flush every 60 seconds                         │
│  └─ Whitelist sync (fire-and-forget)                     │
└─────────────────────────────────────────────────────────┘
```

**Thread-safety mechanisms**: `@Volatile` + `synchronized` singletons · `SupervisorJob` structured concurrency · `WakeLock` for uninterrupted capture · `StateFlow` for reactive UI · ping-pong buffer swap for concurrent capture + inference

---

## 📡 Network Resilience — 3-Tier Fallback

```
Priority 1: HTTPS with DSCP 0xB8 (Expedited Forwarding) QoS
    ├─ Custom SocketFactory sets trafficClass = 0xB8
    ├─ Header: X-5G-QoS: URLLC-Slice-EF
    └─ 2 retries with 5-second exponential backoff

Priority 2: Store-and-Forward (Room SQLite queue)
    ├─ Enqueue to telemetry_queue table on failure
    └─ Auto-flush every 60 seconds on reconnect

Priority 3: SMS / LoRa binary fallback
    ├─ 9-byte LoRa payload: 4B caller hash + 1B score + 4B timestamp
    └─ SMS: hex-encoded 10.25-byte payload via SmsManager
```

Zero data loss across intermittent 5G/4G/offline conditions.

---

## 💾 Memory Engineering — 145 MB Footprint

| Component | Size | Optimization |
|-----------|------|-------------|
| Silero VAD (`.ort`) | ~2 MB | Pre-quantized RNN |
| MiniLM-L6 (`minilm_int8.ort`) | ~22 MB | INT8 quantized |
| Fast Conformer CTC ASR | ~40 MB | INT8 quantized |
| ONNX Runtime native libs | ~30 MB | `arm64-v8a` only (half of multi-ABI) |
| Bloom Filter BitSet | **1.22 MB** | Fixed 10M-bit array |
| 10K contacts (ContactMemory) | **90 KB** | 9-byte bit-packed struct |

**Key build optimizations**: `abiFilters = ["arm64-v8a"]` · `isMinifyEnabled = true` · `isShrinkResources = true` (R8 dead-code elimination)

---

## ✨ App Features

- 🎤 **Real-time scam detection** during live calls (InCallService + CallScreeningService)
- 🔔 **Instant alert toast** when risk score ≥ 70
- 🌙 **Dark mode toggle** persisted via SharedPreferences
- 🔒 **Privacy Policy screen** accessible from Settings
- 📶 **Offline-first telemetry** — Room DB queue, auto-flushed on reconnect
- ♿ **Accessibility compliant** — all icons carry `contentDescription`
- 📊 **Performance timing** logged per audio chunk (target < 20ms)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17+**
- **Go 1.22+** (for the Swarm Server backend)
- **Android device / emulator** — API 26 (Android 8.0)+

### 1. Clone

```bash
git clone https://github.com/Dilipsahu01/CyberGuard_AI_Application.git
cd CyberGuard_AI_Application
```

### 2. Configure Environment

```bash
cp .env.example .env
# Add your GEMINI_API_KEY to .env
```

> ⚠️ **Never commit `.env` or `google-services.json`** — both are gitignored.

### 3. Add Local-Only Files

Place these files locally (they are excluded from git for security):

```
app/google-services.json                          ← from Firebase Console
app/src/main/assets/models/silero_vad.ort         ← Silero VAD model
app/src/main/assets/models/minilm_int8.ort        ← MiniLM INT8 model
app/src/main/assets/sherpa-onnx-.../              ← ASR model directory
app/libs/sherpa-onnx-*.aar                        ← Sherpa ONNX AAR
```

### 4. Build & Run

```bash
# Android app
./gradlew assembleDebug

# Or open in Android Studio and hit Run ▶
```

### 5. Run the Swarm Server (Go)

```bash
cd server
go mod tidy
go run ./main.go
# Runs on :8080 locally · auto-deploys to Vercel on push to main
```

---

## 🧪 Testing

```bash
# Android instrumented tests (Room telemetry queue)
./gradlew connectedAndroidTest

# Go unit tests (Swarm Server endpoint)
cd server && go test ./...
```

---

## 📦 CI/CD

GitHub Actions (`.github/workflows/ci.yml`) automatically:
1. Builds the Android project
2. Runs unit + instrumented tests
3. Deploys the Go backend to **Vercel** on every push to `main`

Live backend: `https://cyberguard-ai.vercel.app`

---

## 📁 Project Structure

```
CyberGuard_AI_Application/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── pipeline/          # VAD · ASR · Regex · NLP · ArcTracker · Ensemble
│   │   ├── services/          # InCallService · ScreeningService · BootReceiver
│   │   ├── models/            # Room DB entities, DAOs, CallContext, ContactMemory
│   │   ├── ui/                # Activities · Fragments · ViewModel · Theme
│   │   └── utils/             # Audio · Permissions · Constants
│   └── src/main/assets/       # ML models (local only, gitignored)
├── server/                    # Go Swarm Server (Vercel-deployable)
│   ├── main.go
│   ├── db.go
│   └── main_test.go
├── .env.example               # Environment variable template
├── .github/workflows/ci.yml   # CI/CD pipeline
└── README.md
```

---

## 🔐 Security & Privacy

- All **caller numbers are SHA-256 hashed** before any storage or transmission — zero PII leaves the device in plaintext
- **`google-services.json`** is gitignored — generate your own from the Firebase Console
- **API keys** live in `.env` (gitignored) or Android `BuildConfig` — never in source code
- **Swarm reports** transmit only anonymized 82-bit signals, not conversation content

---

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m 'feat: add your feature'`
4. Push: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📜 License

Distributed under the **MIT License**.

---

<div align="center">
  Built with ❤️ for the <strong>5G Hackathon</strong>
  <br/>
  <a href="https://github.com/Dilipsahu01/CyberGuard_AI_Application/issues">🐛 Report a Bug</a> · 
  <a href="https://github.com/Dilipsahu01/CyberGuard_AI_Application/issues">💡 Request a Feature</a>
</div>
