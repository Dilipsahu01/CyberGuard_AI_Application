# CyberGuard AI: Developer Setup Guide

This guide details the steps to set up the development environment for the CyberGuard AI Android application and Go server.

## 1. Prerequisites
- **Android Studio 2025.3.4+** (Ladybug or newer).
- **Go 1.22+** (For the telemetry server).
- **Python 3.10+** (For model encryption scripts).
- **Android 14+ Device** (Physical device required for Telecom API and NNAPI testing).

## 2. Android App Setup
1. Clone the private repository.
2. Ensure you have the `arm64-v8a` NDK toolchain installed in Android Studio.
3. **Environment Secrets:**
   - Create a `.env` file in the project root.
   - Populate it with required API keys (refer to `.env.example`).
4. **Model Preparation:**
   - If adding new models, place them in `raw_models_backup/`.
   - Run `python3 encrypt_models.py` to generate the `.enc` assets.
5. **Build:**
   - Run `./gradlew assembleDebug` to build the debug APK.
   - Run `./gradlew testDebugUnitTest` to verify the security and AI layers.

## 3. Go Server Setup
1. Navigate to the `server/` directory.
2. Run `go mod tidy` to install dependencies (Gorilla Mux, SQLCipher, RS CORS).
3. **Run:**
   - `go build -o server_bin .`
   - `./server_bin` (Default port: 8080).
4. **Verify:**
   - `curl http://localhost:8080/api/health`

## 4. Security Hardening Verification
Before submitting code, ensure the following are functional:
- **SQLCipher:** Verify that Room databases are not readable by standard SQLite browsers without the Keystore-bound passphrase.
- **TLS Pinning:** Ensure the app connects only to a server with a valid pinned certificate.
- **Model Cache:** Confirm that `noBackupFilesDir/secure_models/` is empty during idle states (post-engine load).
