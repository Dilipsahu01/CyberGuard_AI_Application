# CyberGuard AI - On-Device Telecom Defense against Social Engineering

<div align="center">
  <img src="https://img.shields.io/badge/Status-Prototype%20Validated-success" alt="Status">
  <img src="https://img.shields.io/badge/Platform-Android%205G-green" alt="Platform">
  <img src="https://img.shields.io/badge/AI-On--Device-blue" alt="AI">
  <img src="https://img.shields.io/badge/Latency-%E2%89%A430ms-orange" alt="Latency">
</div>

## The Problem

In 2024, India lost over ₹1,900 Crore to "digital arrest," voice phishing, and social engineering scams. Current telecom defenses rely heavily on reactive cloud-based lookups and user reports. These legacy systems fail against spoofed VoIP numbers and zero-day attack scripts. Furthermore, when vulnerable citizens—especially the elderly—are placed under psychological pressure by scammers posing as law enforcement, they enter a panic loop and become physically incapable of hanging up the phone.

## The Solution

**CyberGuard AI** is a proactive, real-time telecom defense system that operates entirely on the edge. Rather than simply blocking known numbers, it analyzes the behavioral psychology and intent of the live conversation. 

When a scammer utilizes coercion, false authority, or extreme urgency, the AI instantly detects the threat. To break the victim's panic loop, the system intervenes autonomously by flashing a full-screen RED warning overlay, vibrating the device, and instantly dispatching a "Guardian SMS" to trusted family members with the caller's details.

## Core Technology & Architecture

Engineered to run efficiently on low-cost 5G Android smartphones (tested on ₹12K Oppo A59 5G), CyberGuard AI utilizes a proprietary **Tri-Fold Asynchronous AI Gating Pipeline**:

*   **Stage 1 (VAD Gating):** A lightweight Voice Activity Detection model (Silero VAD) gates the CPU, preserving battery and preventing thermal throttling by sleeping during silence.
*   **Stage 2 (ASR):** Speech is transcribed into Hinglish text in 100ms chunks using a highly optimized Sherpa-ONNX Fast Conformer.
*   **Stage 3 (Intent NLP):** A custom 22MB INT8-quantized MiniLM model, fine-tuned via Layer-Wise Learning Rate Decay, processes the transcript. It uses a bespoke 5-neuron classification head to score the call across distinct psychological vectors (Financial, Urgency, Coercion, Intimacy, Trust).

By shifting computation entirely to the device, the system achieves an end-to-end pipeline latency of **≤30.1 ms**—well within the telecom budget for real-time intervention.

### Pipeline Flow

```mermaid
graph LR
    %% Styling Classes
    classDef input fill:#1f2937,stroke:#3b82f6,stroke-width:2px,color:#fff,rx:10px,ry:10px
    classDef vad fill:#374151,stroke:#10b981,stroke-width:2px,color:#fff,rx:10px,ry:10px
    classDef asr fill:#374151,stroke:#8b5cf6,stroke-width:2px,color:#fff,rx:10px,ry:10px
    classDef nlp fill:#374151,stroke:#ec4899,stroke-width:2px,color:#fff,rx:10px,ry:10px
    classDef output fill:#7f1d1d,stroke:#ef4444,stroke-width:3px,color:#fff,rx:10px,ry:10px
    classDef fast fill:#064e3b,stroke:#059669,stroke-width:1px,color:#a7f3d0

    %% Nodes
    A["Audio Stream<br/><span style='font-size:12px;color:#9ca3af'>100ms PCM-16BIT</span>"]:::input
    
    B["<b>STAGE 1: VAD Gating</b><br/>Silero VAD (2.3MB)<br/><span style='font-size:12px;color:#10b981'>Latency: 3.2 ms</span>"]:::vad
    
    C["<b>STAGE 2: Transcription</b><br/>Sherpa-ONNX Fast Conformer<br/><span style='font-size:12px;color:#a78bfa'>Latency: 14.5 ms</span>"]:::asr
    
    D["<b>STAGE 3: Intent NLP</b><br/>Custom MiniLM (22MB INT8)<br/><span style='font-size:12px;color:#f472b6'>Latency: 8.8 ms</span>"]:::nlp
    
    E["Threat Fusion<br/><b>Total Latency: ≤30.1 ms</b>"]:::output

    %% Connections
    A -->|16kHz| B
    B -->|Speech Detected| C
    B -.->|Silence - Sleep Mode| Z["Battery Preserved"]:::fast
    C -->|Hinglish Text| D
    D -->|5 Intent Logits| E
```

## Swarm Intelligence & 5G Infrastructure

CyberGuard AI is designed as a B2B2G (Business-to-Business-to-Government) infrastructure solution. 

When a threat is detected, it generates an ultra-compact **82-bit binary telemetry packet** (just 10.25 bytes). This packet transmits anonymous threat signals to a Go-based Swarm Server via a 5G URLLC network slice. The server utilizes a 10M-bit Bloom Filter for O(1) instant threat lookups across the national network, essentially immunizing the entire grid against a scammer within seconds of their first call. 

Additionally, a persistent **72-bit ContactMemory state machine** tracks multi-session conversational arcs, effectively defending against slow-burn "pig butchering" and romance scams.

## Privacy by Design

Because all AI processing happens locally on the smartphone, **zero bytes of raw audio or transcript data ever leave the device.** Only anonymized risk scores are shared with the network. The proprietary local neural network weights are secured by "The Vault"—a multi-layered defense architecture utilizing AES-256-GCM encryption, Android Keystore, and NDK XOR salts.

## Achievements & Project Status

*   **Top 50 Proposal:** Selected as one of the Top 50 proposals nationwide in the 5G Innovation Hackathon.
*   **Seed Funding:** Granted official prototype development seed funding.
*   **IMC 2026 Showcase:** Awarded an exclusive startup stall at the prestigious India Mobile Congress (IMC) 2026 ASPIRE Pavilion.

This project is a prototype-validated blueprint for securing telecom networks against social engineering.

*Built by Dilip Sahu - Founder & AI Architect*
