<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
<img src="https://img.shields.io/badge/Backend-Go-00ADD8?style=for-the-badge&logo=go&logoColor=white" />
<img src="https://img.shields.io/badge/AI-Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white" />
<img src="https://img.shields.io/badge/Deploy-Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white" />

# 🛡️ CyberGuard AI

### Real-Time Scam Call Detection — Powered by On-Device AI & 5G Edge Intelligence

*Built for the 5G Hackathon · Privacy-First · Offline-Capable · Sub-20ms Latency*

</div>

---

## 📌 Overview

**CyberGuard AI** is a next-generation Android application that detects scam and fraudulent phone calls in **real-time** using a multi-stage on-device AI pipeline. It combines Voice Activity Detection (VAD), Automatic Speech Recognition (ASR), NLP-based intent analysis, and an ensemble risk engine — all running **on-device** for full privacy.

When a threat is detected, alerts are pushed to a **Swarm Server** (Go backend hosted on Vercel) to crowd-source scam intelligence across the network.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🎤 **On-Device VAD** | Silero VAD filters silence — only speech chunks are processed |
| 🗣️ **Streaming ASR** | Sherpa-ONNX NeMo CTC model transcribes speech at ~80ms chunks |
| 🔍 **Regex Gate** | Pattern-matches known scam phrases in O(1) before heavier models |
| 🧠 **Intent NLP** | MiniLM embedding model scores semantic similarity to scam intents |
| ⚖️ **Ensemble Engine** | Fuses all signals into a final risk score (0–100) |
| 📡 **Swarm Reporting** | Anonymized reports queued offline, flushed on reconnect via Room DB |
| 🌙 **Dark Mode** | Persistent dark/light toggle via SharedPreferences |
| 🔒 **Privacy Policy** | Dedicated in-app privacy screen |
| ♿ **Accessibility** | All icons carry `contentDescription`; Accessibility Scanner validated |

---

## 🏗️ Architecture

```
Incoming Call Audio
        │
        ▼
┌─────────────────┐
│  Silero VAD     │  ← filters silence, emits speech chunks
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Streaming ASR  │  ← Sherpa-ONNX NeMo CTC (int8, on-device)
│  (80ms chunks)  │
└────────┬────────┘
         │  transcript
         ▼
┌─────────────────┐
│   Regex Gate    │  ← instant pattern match (allow/flag/block)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Intent NLP    │  ← MiniLM-L6 embeddings, cosine similarity
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Ensemble Engine │  ← weighted fusion → risk score 0–100
└────────┬────────┘
         │ score ≥ 70 → ALERT
         ▼
┌─────────────────┐     ┌──────────────────────┐
│  InCall UI      │     │  Swarm Server (Go)   │
│  Toast / Dialog │ ──▶ │  Vercel Edge API     │
└─────────────────┘     └──────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17+**
- **Go 1.22+** (for the backend server)
- **Android device / emulator** running API 26+

### 1. Clone the Repository

```bash
git clone https://github.com/Dilipsahu01/CyberGuard_AI_Application.git
cd CyberGuard_AI_Application
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY
```

> ⚠️ **Never commit your `.env` file.** It is already in `.gitignore`.

### 3. Add Required Files (not in repo for security)

- Place your `google-services.json` from the [Firebase Console](https://console.firebase.google.com/) into `app/`
- Place your ML model files into `app/src/main/assets/`:
  - `models/silero_vad.ort`
  - `models/minilm_int8.ort`
  - `sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/` (model dir)

### 4. Build & Run (Android)

Open the project in **Android Studio**, sync Gradle, and hit **Run ▶** on your device or emulator.

```bash
./gradlew assembleDebug
```

### 5. Run the Backend Server (Go)

```bash
cd server
go mod tidy
go run ./main.go
# Server starts on :8080 locally
# Deploys automatically to Vercel on push to main
```

---

## 🧪 Testing

```bash
# Android instrumented tests (Room telemetry queue)
./gradlew connectedAndroidTest

# Go unit tests (telemetry endpoint)
cd server && go test ./...
```

---

## 📦 CI / CD

GitHub Actions (`.github/workflows/ci.yml`) automatically:
1. Builds the Android project
2. Runs unit tests
3. Deploys the Go server to **Vercel** on every push to `main`

Live backend: `https://cyberguard-ai.vercel.app`

---

## 🔐 Security Notes

- **API keys** are never stored in source code. Use `.env` (gitignored) or Android `BuildConfig` fields.
- **`google-services.json`** is gitignored — generate your own from Firebase Console.
- The Swarm Server receives only **anonymized, aggregated** scam signals — no PII is transmitted.

---

## 📁 Project Structure

```
CyberGuard_AI_Application/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── pipeline/          # VAD · ASR · NLP · Ensemble
│   │   ├── services/          # InCallService · ScreeningService
│   │   ├── models/            # Room DB entities & DAOs
│   │   ├── ui/                # Activities · Fragments · ViewModel
│   │   └── utils/             # Audio · Permissions · Constants
│   └── src/main/assets/       # ML models (added locally, not in repo)
├── server/                    # Go backend (Vercel)
│   ├── main.go
│   ├── db.go
│   └── main_test.go
├── .env.example               # Environment variable template
├── .github/workflows/ci.yml   # CI/CD pipeline
└── README.md
```

---

## 🤝 Contributing

Pull requests are welcome! Please:
1. Fork the repo
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📜 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.

---

<div align="center">
  Made with ❤️ for the 5G Hackathon &nbsp;·&nbsp; 
  <a href="https://github.com/Dilipsahu01/CyberGuard_AI_Application/issues">Report a Bug</a> &nbsp;·&nbsp;
  <a href="https://github.com/Dilipsahu01/CyberGuard_AI_Application/issues">Request a Feature</a>
</div>
