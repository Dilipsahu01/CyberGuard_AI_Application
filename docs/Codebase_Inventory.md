# CyberGuard AI: Codebase Inventory & Breakdown

This document outlines the exact composition of the handwritten source code, configuration files, AI models, and documentation assets within the CyberGuard AI repository. 

*Note: This inventory explicitly excludes automatically generated build files, Gradle caches, and Git version control history (which total over 3,000 files). The following represents the actual engineering footprint.*

## 1. Core Source Code
The operational logic of the Android client, native C++ security hooks, and the Go telemetry backend.

*   **`82` Kotlin Files (`.kt`)**
    *   The core Android application logic, AI pipeline managers (`ScamDetectionService`), Jetpack Compose UI architecture, and Room database entities.
*   **`3` C / C++ Files (`.cpp`, `.c`)**
    *   The Android NDK native JNI bindings (`libcyberguard_secrets.so`) used for memory-safe encryption and string obfuscation.
*   **`3` Go Files (`.go`)**
    *   The Swarm Telemetry backend server code capable of ultra-fast concurrent UDP/HTTPS threat intelligence aggregation.
*   **`2` Python Scripts (`.py`)**
    *   Automation scripts (`encrypt_models.py`) used by ML engineers to AES-256 encrypt the PyTorch/ONNX weights before Android deployment.

## 2. Documentation & Configurations
The instructional guides, structural definitions, and Android manifest configurations.

*   **`11` Markdown Files (`.md`)**
    *   Deep-dive architectural specifications, security protocols, V1.1 roadmaps, and this codebase inventory (located mostly in `/docs`).
*   **`33` XML Files (`.xml`)**
    *   The `AndroidManifest.xml`, Jetpack Navigation graphs, color/string resources, and Vector Drawables (`.xml` icons).
*   **`4` Kotlin Script Files (`.kts`)**
    *   The Gradle build configurations (`build.gradle.kts`, `settings.gradle.kts`) controlling dependencies, ProGuard/R8 shrinking, and ABI stripping.
*   **`4` Properties Files (`.properties`)**
    *   Local environment properties (e.g., Keystore passwords, SDK paths, and Kotlin compiler flags).
*   **`13` JSON Files (`.json`)**
    *   Service configurations, mock responses, and static metadata.

## 3. AI Models & Threat Databases
The physical neural network weights and offline datasets enabling 0ms latency detection.

*   **`3` Encrypted AI Models (`.enc`)**
    *   The AES-256 encrypted production variants of the Silero VAD and MiniLM NLP models, loaded dynamically into volatile RAM.
*   **`3` Unencrypted Models (`.ort`, `.onnx`)**
    *   The raw plaintext models located in backup directories (e.g., the Sherpa-ONNX Fast Conformer).
*   **`9` Text Files (`.txt`)**
    *   The token mappings (`tokens.txt`) and vocabulary (`vocab.txt`) dictionaries required for the NLP/ASR tokenizers.
*   **`2` CSV Files (`.csv`)**
    *   The offline threat intelligence datasets (`dot_scam_blacklist.csv`, `mock_device_contacts.csv`) loaded into the Room DB and C++ Bloom Filter.

## 4. Assets & Media
Visual assets and presentation materials for the Hackathon showcase.

*   **`10` WebP Images (`.webp`)**
    *   Highly optimized UI graphics, logos, and app icons.
*   **`1` PowerPoint Deck (`.pptx`)**
    *   The official pitch presentation for the 5G Innovation Hackathon.

---
**Total Hand-Managed Engineering Files:** ~170 files.
