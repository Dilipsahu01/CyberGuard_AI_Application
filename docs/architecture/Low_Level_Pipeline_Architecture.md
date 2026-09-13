# CyberGuard-AI Low-Level Execution Pipeline

This diagram maps the exact class structures, bit-packing, memory constraints, and AI inference models running locally on the device with the latest security hardening.

```mermaid
graph TD
    classDef default fill:#0D1117,stroke:#30363D,stroke-width:1px,color:#E6EDF3,font-size:12px;
    classDef startEvent fill:#238636,stroke:#2EA043,stroke-width:2px,color:#FFF,font-weight:bold;
    classDef hardware fill:#1F6FEB,stroke:#388BFD,stroke-width:2px,color:#FFF;
    classDef aiEngine fill:#8957E5,stroke:#A371F7,stroke-width:2px,color:#FFF;
    classDef bitPacking fill:#D29922,stroke:#E3B341,stroke-width:2px,color:#000,font-weight:bold;
    classDef alertRed fill:#DA3633,stroke:#F85149,stroke-width:3px,color:#FFF,font-weight:bold;
    classDef safeGreen fill:#238636,stroke:#2EA043,stroke-width:2px,color:#FFF;
    classDef database fill:#005CC5,stroke:#0366D6,stroke-width:2px,color:#FFF;

    %% 1. Interception
    A[Incoming Call Event]:::startEvent --> B{RoleManager<br/>ROLE_CALL_SCREENING}
    B -->|Extract| C[Caller Number E.164]
    
    %% Stage 2: Hardware Checks
    C --> D[PhoneNumberUtils.normalize]:::hardware
    D --> E[SHA-256 Hash -> 14-bit Prefix]:::bitPacking
    E --> F{10M-bit Bloom Filter}:::database
    
    F -->|Match: 1| G[Flag: BloomFlag = 1<br/>Check SQLCipher Cache]
    G -->|Known Scammer| H[CRITICAL ALERT]:::alertRed
    
    %% Stage 3: Audio & VAD
    F -->|No Match| I[AudioRecord Service<br/>PCM-16BIT / 16kHz]:::hardware
    I --> J[Silero VAD Inference]:::aiEngine
    
    J --> K{Speech Probability > 0.5f?}
    K -->|Yes| L[Sherpa-ONNX ASR<br/>Local Transcriber]:::aiEngine
    L --> M[StateFlow: _transcriptFlow]
    
    %% Stage 4: Inference
    M --> N{Dual Inference Engine}
    N --> O[RegexGate FSM]:::hardware
    N --> P[MiniLM-L6 NLP Engine]:::aiEngine
    
    %% Stage 5: Fusion
    O --> Q[EnsembleEngine.calculate]:::hardware
    P --> Q
    
    Q --> R[Output: Ensemble Score 0-100]
    R --> S{Decision Gate}
    
    %% Stage 6: Persistence & Swarm
    S -->|Call Ended| T[TranscriptScrubber.kt]:::hardware
    T -->|Masked Text| U[SQLCipher Encrypted DB]:::database
    
    S -->|isScamDetected| V[Construct 82-bit Telemetry]:::bitPacking
    V --> W{Network Type}
    W -->|5G URLLC / TLS Pinning| X[5G Swarm Server]
    W -->|Offline| Y[Encrypted SQLite Queue]
    
    %% DPDP
    Z[User Request: Purge Data]:::startEvent --> ZA[Wipe SQLCipher DBs]
    ZA --> ZB[Server Cascade Delete API]
```

### Key Engineering Hardening Points:

1. **SQLCipher Persistence**: Every database write in Stage 6 is encrypted at rest using 256-bit AES via SQLCipher.
2. **On-Device NER (Scrubbing)**: The `TranscriptScrubber` in Stage 6 ensures that PII (Names/OTPs) is masked before touching the disk.
3. **Binary Telemetry**: The Swarm Server in Stage 6 only accepts bit-packed binary payloads. Raw text never traverses the network.
4. **TLS Certificate Pinning**: The communication between the device and the Swarm Server is hardened against MITM using pinned certificate hashes.
5. **Right to Erasure**: A dedicated purge mechanism (Stage 6.2) allows users to comply with the DPDP "Right to be Forgotten" mandate.
