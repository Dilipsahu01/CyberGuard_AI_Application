# CyberGuard AI — Architecture Diagrams (Mermaid Code)

> **Rendering Instructions:** Paste each code block into [Mermaid Live Editor](https://mermaid.live), export as **SVG** or **PNG (2x scale)**, and place on the A4 page. All diagrams use large fonts, short labels, and thick strokes for print readability.

---

## Diagram 1: High-Level System Architecture

> Replaces: `[INSERT: Structural Blueprint/Diagram]` in §5.1

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '18px', 'fontFamily': 'Inter, Segoe UI, sans-serif', 'primaryColor': '#ffffff', 'primaryTextColor': '#000000', 'lineColor': '#6c63ff', 'primaryBorderColor': '#6c63ff'}}}%%

graph TB
    subgraph EDGE["📱 EDGE CLIENT — Android 14+ Application"]
        direction TB

        subgraph CALL["Call Interception Layer"]
            A["📞 CyberGuardInCallService<br/><i>Default Dialer / Call Screener</i>"]
            B[" EnvironmentGuard<br/><i>Root & Emulator Detection</i>"]
        end

        subgraph AUDIO["Audio Capture Engine"]
            C[" AudioRecord<br/><i>16kHz Mono · PCM-16BIT</i><br/><i>100ms Chunks · Zero-Alloc Pool</i>"]
        end

        subgraph AI["Tri-Fold AI Inference Pipeline"]
            D["🔇 Silero VAD<br/><i>Voice Activity Detection</i><br/><i>RNN · INT8 ONNX</i>"]
            E["🗣 Sherpa-ONNX ASR<br/><i>Fast Conformer CTC</i><br/><i>Hinglish STT · INT8</i>"]
            F[" IntentNLP<br/><i>MiniLM-L6-v2</i><br/><i>384D Embeddings · INT8</i>"]
        end

        subgraph DETECT["Threat Detection & Scoring"]
            G["⚡ RegexGate<br/><i>19 Deterministic Patterns</i>"]
            H["📊 ArcTracker FSM<br/><i>5-Phase Social Engineering Arc</i>"]
            I["🎯 EnsembleEngine<br/><i>Fusion Scoring · 0–100 Risk</i>"]
        end

        subgraph THERMAL["Orchestration"]
            J["🌡 PipelineManager<br/><i>ADPF Thermal Routing</i>"]
        end

        subgraph SECURITY["Security & Privacy Layer"]
            K["🔐 The Vault<br/><i>AES-GCM · HW Keystore</i><br/><i>NDK Key Derivation</i>"]
            L["🧹 TranscriptScrubber<br/><i>On-Device PII Masking</i>"]
            M["🗄 SQLCipher DB<br/><i>AES-256 Encrypted Storage</i>"]
        end

        subgraph OUTPUT["User Intervention"]
            N[" IncomingCallActivity<br/><i>Red Alert · Warning Overlay</i>"]
            O["📲 Guardian SMS Alert<br/><i>Emergency Contact Notify</i>"]
        end

        subgraph TELEMETRY["Swarm Telemetry"]
            P["📡 SwarmReporter<br/><i>82-bit Binary Pack</i><br/><i>TLS Pinning · DSCP 0xB8</i>"]
        end
    end

    subgraph SERVER["🖥 SWARM SERVER — Go Backend"]
        direction TB
        Q["🚧 Rate Limiter<br/><i>20 req/min per IP</i>"]
        R["🔑 HMAC-SHA256<br/><i>Payload Verification</i>"]
        S["🔓 AES-GCM Decrypt<br/><i>Caller Hash Recovery</i>"]
        T["💾 SQLite Threat DB<br/><i>Aggregated Intelligence</i>"]
    end

    A --> C
    C --> D
    D -->|"Speech Detected"| E
    E -->|"Transcript"| F
    F --> I
    G --> I
    H --> I
    J -.->|"Thermal Gate"| F
    I -->|"Score ≥ 70%"| N
    I -->|"Scam Detected"| O
    I --> L
    L --> M
    I --> P
    K -.->|"Decrypt Models"| D
    K -.->|"Decrypt Models"| E
    K -.->|"Decrypt Models"| F
    P -->|"5G URLLC"| Q
    Q --> R
    R --> S
    S --> T

    style EDGE fill:none,stroke:#6c63ff,stroke-width:3px,color:#333
    style SERVER fill:none,stroke:#00c853,stroke-width:3px,color:#333
    style CALL fill:none,stroke:#6c63ff,stroke-width:2px,color:#333
    style AUDIO fill:none,stroke:#1f6feb,stroke-width:2px,color:#333
    style AI fill:none,stroke:#8957e5,stroke-width:2px,color:#333
    style DETECT fill:none,stroke:#d29922,stroke-width:2px,color:#333
    style THERMAL fill:none,stroke:#f97316,stroke-width:2px,color:#333
    style SECURITY fill:none,stroke:#da3633,stroke-width:2px,color:#333
    style OUTPUT fill:none,stroke:#f85149,stroke-width:2px,color:#333
    style TELEMETRY fill:none,stroke:#00c853,stroke-width:2px,color:#333
```

---

## Diagram 2: Edge AI Inference Pipeline (Detailed)

> Use for: `[INSERT: Integration Phase Photo/Screenshot]` locations in §5.2

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '16px', 'fontFamily': 'Inter, Segoe UI, sans-serif'}}}%%

flowchart TD
    A["📞 Incoming Call Event"] --> B{" Emergency Number?<br/><b>100 / 112 / 911 / 999</b>"}

    B -->|"YES"| C["⛔ BYPASS<br/><i>stopSelf — Release All Resources</i><br/><i>Zero AI Interference</i>"]

    B -->|"NO"| D["🔍 Bloom Filter Lookup<br/><i>10M-bit Matrix · O(1)</i><br/><i>MurmurHash3 + xxHash32</i>"]

    D -->|"HIT: Known Scammer"| E[" CRITICAL ALERT<br/><i>Instant Warning Overlay</i>"]

    D -->|"NO MATCH"| F[" AudioRecord Starts<br/><i>16kHz · PCM-16BIT · 100ms Chunks</i><br/><i>Zero-Allocation Object Pool</i>"]

    F --> G["🔇 Stage 1: Silero VAD<br/><i>Speech Probability > 0.5f ?</i><br/><i>Latency: 3.2 ms</i>"]

    G -->|"No Speech"| H[" SKIP<br/><i>NLP Stays Dormant</i><br/><i>Battery Preserved</i>"]

    G -->|"Speech Detected"| I["🗣 Stage 2: Sherpa-ONNX ASR<br/><i>Fast Conformer CTC · INT8</i><br/><i>Hinglish/English · RTF: 0.31x</i><br/><i>Latency: 14.5 ms</i>"]

    I --> J{"🌡 ADPF Thermal Check<br/><b>Headroom < 15% ?</b>"}

    J -->|"NORMAL"| K[" Stage 3: IntentNLP<br/><i>MiniLM-L6 · 384D Vectors</i><br/><i>5 Psych Intents · Latency: 8.8 ms</i>"]

    J -->|"OVERHEATING"| L["⚡ DEGRADED MODE<br/><i>RegexGate Only</i><br/><i>Zero Compute Fallback</i>"]

    K --> M["⚡ RegexGate<br/><i>19 Deterministic Patterns</i><br/><i>Instant Hardware Interrupts</i>"]

    K --> N["📊 ArcTracker FSM<br/><i>INTRO → TRUST_BUILD →</i><br/><i>PROBLEM → REQUEST → CLOSE</i>"]

    M --> O["🎯 EnsembleEngine<br/><b>Fusion Score: 0 – 100</b>"]
    N --> O
    L --> O

    O -->|"Score < 70"| P[" SAFE<br/><i>Continue Monitoring</i>"]

    O -->|"Score ≥ 70"| Q[" SCAM DETECTED"]

    Q --> R["🔴 Screen Flashes RED<br/><i>Full-Screen Warning Overlay</i>"]
    Q --> S["📳 Device Vibrates<br/><i>300ms Haptic Alert</i>"]
    Q --> T["📲 Guardian SMS Sent<br/><i>Silent Emergency Alert</i>"]

    style A fill:#238636,stroke:#2ea043,stroke-width:3px,color:#fff
    style C fill:#6e7681,stroke:#8b949e,stroke-width:2px,color:#fff
    style E fill:#da3633,stroke:#f85149,stroke-width:3px,color:#fff
    style F fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style G fill:#8957e5,stroke:#a371f7,stroke-width:2px,color:#fff
    style H fill:#6e7681,stroke:#8b949e,stroke-width:2px,color:#fff
    style I fill:#8957e5,stroke:#a371f7,stroke-width:2px,color:#fff
    style K fill:#8957e5,stroke:#a371f7,stroke-width:2px,color:#fff
    style L fill:#f97316,stroke:#fb923c,stroke-width:2px,color:#fff
    style M fill:#d29922,stroke:#e3b341,stroke-width:2px,color:#000
    style N fill:#d29922,stroke:#e3b341,stroke-width:2px,color:#000
    style O fill:#1f6feb,stroke:#388bfd,stroke-width:3px,color:#fff
    style P fill:#238636,stroke:#2ea043,stroke-width:2px,color:#fff
    style Q fill:#da3633,stroke:#f85149,stroke-width:3px,color:#fff
    style R fill:#da3633,stroke:#f85149,stroke-width:2px,color:#fff
    style S fill:#da3633,stroke:#f85149,stroke-width:2px,color:#fff
    style T fill:#da3633,stroke:#f85149,stroke-width:2px,color:#fff
```

---

## Diagram 3: Security & Encryption Architecture ("The Vault")

> Use for: `[INSERT: Integration Phase Photo/Screenshot — Encryption pipeline]` in §5.2 Step 8

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '16px', 'fontFamily': 'Inter, Segoe UI, sans-serif'}}}%%

flowchart TD
    subgraph BUILD[" BUILD TIME — Developer Machine"]
        A["📄 Raw .onnx / .ort Models<br/><i>raw_models_backup/</i>"] --> B["🐍 encrypt_models.py<br/><i>AES-256-GCM Encryption</i>"]
        B --> C[" Encrypted .enc Files<br/><i>app/src/main/assets/models/</i>"]
        B --> D["#⃣ SHA-256 Baseline Hashes<br/><i>ModelIntegrityVerifier.kt</i>"]
    end

    subgraph RUNTIME["📱 RUNTIME — Android Device"]
        E["🔐 Android Hardware Keystore<br/><i>TEE / StrongBox</i>"]
        F[" NDK secrets.cpp<br/><i>XOR Salt Mixing</i><br/><i>Build.FINGERPRINT + HW IDs</i>"]
        G["🔑 Derived Master Key<br/><i>Dynamic · Runtime Only</i>"]

        E --> G
        F --> G

        G --> H["🔓 ModelCryptoManager.kt<br/><i>AES-GCM Decryption</i><br/><i>Sequential into RAM</i>"]

        C -.->|"Read .enc"| H

        H --> I[" ModelIntegrityVerifier<br/><i>SHA-256 Hash Validation</i><br/><i>Reject Poisoned Models</i>"]

        I -->|"PASS"| J[" ONNX Runtime Session<br/><i>Models Live in Volatile RAM</i><br/><i>NNAPI / CPU Delegation</i>"]

        I -->|"FAIL"| K["⛔ REJECTED<br/><i>Tampered Model Detected</i><br/><i>Pipeline Halted</i>"]

        H --> L["🗑 Immediate Cache Purge<br/><i>noBackupFilesDir wiped</i><br/><i>Zero Disk Footprint</i>"]
    end

    subgraph GUARD[" ENVIRONMENTAL SECURITY"]
        M["🔍 EnvironmentGuard.kt"]
        M --> N{"Root Detected?<br/><i>su binary / test-keys / rw mounts</i>"}
        N -->|"YES"| O["💀 APP TERMINATED<br/><i>IP Protection Enforced</i>"]
        N -->|"NO"| P[" Environment Clear<br/><i>Pipeline Proceeds</i>"]
    end

    style BUILD fill:none,stroke:#d29922,stroke-width:3px,color:#333
    style RUNTIME fill:none,stroke:#8957e5,stroke-width:3px,color:#333
    style GUARD fill:none,stroke:#da3633,stroke-width:3px,color:#333
    style A fill:#6e7681,stroke:#8b949e,stroke-width:2px,color:#fff
    style C fill:#238636,stroke:#2ea043,stroke-width:2px,color:#fff
    style E fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style G fill:#d29922,stroke:#e3b341,stroke-width:2px,color:#000
    style J fill:#238636,stroke:#2ea043,stroke-width:3px,color:#fff
    style K fill:#da3633,stroke:#f85149,stroke-width:2px,color:#fff
    style L fill:#f97316,stroke:#fb923c,stroke-width:2px,color:#fff
    style O fill:#da3633,stroke:#f85149,stroke-width:3px,color:#fff
```

---

## Diagram 4: Network Deployment Architecture

> Replaces: `[INSERT: Network Architecture Diagram]` in §6.1

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '16px', 'fontFamily': 'Inter, Segoe UI, sans-serif'}}}%%

flowchart LR
    subgraph DEVICES["📱 Edge Client Fleet"]
        direction TB
        D1["📱 Device 1<br/><i>Android 14+</i><br/><i>Full AI Pipeline</i>"]
        D2["📱 Device 2<br/><i>Android 14+</i><br/><i>Full AI Pipeline</i>"]
        D3["📱 Device 3<br/><i>Android 14+</i><br/><i>Full AI Pipeline</i>"]
        DN["📱 Device N<br/><i>Nationwide Fleet</i>"]
    end

    subgraph TRANSPORT[" 5G URLLC Transport"]
        direction TB
        T1["📶 5G Network Slice<br/><i>DSCP 0xB8</i><br/><i>Expedited Forwarding</i>"]
        T2[" TLS 1.3<br/><i>Certificate Pinning</i><br/><i>MITM Protection</i>"]
        T3["📡 SMS Fallback<br/><i>9-byte LoRa Payload</i><br/><i>Low-Bandwidth Mode</i>"]
    end

    subgraph SERVER["🖥 Swarm Server"]
        direction TB
        S1["🚧 Rate Limiter<br/><i>20 req/min per IP</i>"]
        S2["🔑 HMAC-SHA256<br/><i>Signature Verify</i>"]
        S3["🔓 AES-GCM<br/><i>Decrypt Caller Hash</i>"]
        S4["💾 SQLite<br/><i>Threat Intelligence DB</i>"]
        S5["📤 Bloom Filter Updates<br/><i>Push to Clients</i>"]

        S1 --> S2 --> S3 --> S4
        S4 --> S5
    end

    D1 -->|"82-bit Binary"| T1
    D2 -->|"82-bit Binary"| T1
    D3 -->|"82-bit Binary"| T1
    DN -->|"82-bit Binary"| T1

    D1 -.->|"Offline Fallback"| T3
    DN -.->|"Offline Fallback"| T3

    T1 -->|"HTTPS POST"| S1
    T2 -.->|"Secures"| T1
    T3 -->|"SMS Hex"| S1

    S5 -->|"Sync"| D1
    S5 -->|"Sync"| D2
    S5 -->|"Sync"| D3
    S5 -->|"Sync"| DN

    style DEVICES fill:none,stroke:#6c63ff,stroke-width:3px,color:#333
    style TRANSPORT fill:none,stroke:#f97316,stroke-width:3px,color:#333
    style SERVER fill:none,stroke:#00c853,stroke-width:3px,color:#333
    style D1 fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style D2 fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style D3 fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style DN fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style T1 fill:#f97316,stroke:#fb923c,stroke-width:2px,color:#fff
    style T2 fill:#238636,stroke:#2ea043,stroke-width:2px,color:#fff
    style T3 fill:#6e7681,stroke:#8b949e,stroke-width:2px,color:#fff
    style S4 fill:#238636,stroke:#2ea043,stroke-width:2px,color:#fff
```

---

## Diagram 5: End-to-End Call Data Flow (Post-Call Persistence)

> Use for: Data Flow tracing in §6.2

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '16px', 'fontFamily': 'Inter, Segoe UI, sans-serif'}}}%%

flowchart TD
    A["📞 Call Ended<br/><i>ScamDetectionService</i>"] --> B["📝 Raw Transcript<br/><i>In Volatile RAM</i>"]

    B --> C["🧹 TranscriptScrubber.kt<br/><i>PII Masking Engine</i>"]

    C --> D["OTP: 482619 → <b>[OTP]</b>"]
    C --> E["Acct: 1234567890 → <b>[ACCOUNT_ID]</b>"]
    C --> F["mera naam Ravi → <b>[NAME]</b>"]

    D --> G["📄 Scrubbed Transcript"]
    E --> G
    F --> G

    G --> H["🗄 SQLCipher Database<br/><i>AES-256 Encrypted</i><br/><i>HW Keystore Passphrase</i>"]

    A --> I["📊 EnsembleEngine Score<br/><i>+ NLP Logits + Regex + Arc</i>"]

    I --> J["📦 CallContext Packing<br/><i>14 Vectors → 82 Bits</i><br/><i>= 10.25 Bytes</i>"]

    J --> K["#⃣ SHA-256 Hash<br/><i>Caller Number → Anonymous</i>"]

    K --> L{"📶 Network Available?"}

    L -->|"YES"| M["📡 HTTPS POST<br/><i>DSCP 0xB8 · TLS Pinning</i><br/><i>HMAC-SHA256 Signed</i>"]

    L -->|"NO"| N["💾 Encrypted Queue<br/><i>SQLite Offline Buffer</i>"]

    N -.->|"Retry on Connect"| M

    M --> O["🖥 Swarm Server<br/><i>Threat DB Updated</i>"]

    A --> P{"🗑 User: Purge Data?"}
    P -->|"YES"| Q["🧨 Wipe SQLCipher DBs<br/><i>+ Server Cascade Delete</i><br/><i>DPDP Right to Erasure</i>"]

    style A fill:#1f6feb,stroke:#388bfd,stroke-width:2px,color:#fff
    style C fill:#d29922,stroke:#e3b341,stroke-width:2px,color:#000
    style H fill:#238636,stroke:#2ea043,stroke-width:3px,color:#fff
    style J fill:#8957e5,stroke:#a371f7,stroke-width:2px,color:#fff
    style M fill:#238636,stroke:#2ea043,stroke-width:2px,color:#fff
    style N fill:#f97316,stroke:#fb923c,stroke-width:2px,color:#fff
    style O fill:#00c853,stroke:#00e676,stroke-width:2px,color:#000
    style Q fill:#da3633,stroke:#f85149,stroke-width:2px,color:#fff
```

---

## Diagram 6: 82-Bit Telemetry Wire Format (CallContext)

> Use for: Binary telemetry visualization in §6.2 or §4

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '15px', 'fontFamily': 'Inter, Segoe UI, sans-serif'}}}%%

flowchart LR
    subgraph WIRE["📦 82-BIT CALLCONTEXT WIRE FORMAT — 10.25 Bytes Total"]
        direction LR

        subgraph ID["Caller Identity — 14 bits"]
            A["CallerHash<br/><b>14 bits</b><br/><i>SHA-256 Prefix</i>"]
        end

        subgraph FLAGS["Boolean Flags — 4 bits"]
            B["BF<br/><b>1</b>"]
            C["VP<br/><b>1</b>"]
            D["SS<br/><b>1</b>"]
            E["RC<br/><b>1</b>"]
        end

        subgraph HISTORY["Caller History — 16 bits"]
            F["DaysKnown<br/><b>8 bits</b>"]
            G["TotalCalls<br/><b>8 bits</b>"]
        end

        subgraph SCORES["Detection Scores — 48 bits"]
            H["Regex<br/><b>6 bits</b>"]
            I["Financial<br/><b>7 bits</b>"]
            J["Urgency<br/><b>7 bits</b>"]
            K["Coercion<br/><b>7 bits</b>"]
            L["Intimacy<br/><b>7 bits</b>"]
            M["Trust<br/><b>7 bits</b>"]
            N["Ensemble<br/><b>7 bits</b>"]
        end
    end

    style WIRE fill:none,stroke:#6c63ff,stroke-width:3px,color:#333
    style ID fill:none,stroke:#388bfd,stroke-width:2px,color:#333
    style FLAGS fill:none,stroke:#e3b341,stroke-width:2px,color:#333
    style HISTORY fill:none,stroke:#a371f7,stroke-width:2px,color:#333
    style SCORES fill:none,stroke:#2ea043,stroke-width:2px,color:#333
```

> **Flag Legend:** BF = Bloom Filter Hit, VP = VoIP Flag, SS = STIR/SHAKEN Fail, RC = Repeated Caller
> **Total:** 14 + 4 + 16 + 48 = **82 bits (10.25 bytes)**

---

## Rendering Tips for A4 Print

| Setting | Recommendation |
|:---|:---|
| **Export Format** | SVG preferred (scales perfectly), or PNG at **2x / 3x scale** |
| **Page Orientation** | Diagrams 1, 2, 3, 5 → **Portrait**. Diagram 4, 6 → **Landscape** |
| **Mermaid Live Editor** | Paste code at [mermaid.live](https://mermaid.live) → Actions → Export PNG (2048px width) |
| **Background** | Set to **white** for print, or keep dark for digital submission |
| **Font Override** | If text appears small, increase `fontSize` in the `%%{init}%%` header (e.g., `20px`) |
