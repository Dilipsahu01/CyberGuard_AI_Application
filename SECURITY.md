# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please do **not** open a public GitHub issue.

Instead, contact the author directly via GitHub:

**https://github.com/Dilipsahu01**

You will receive a response within 72 hours. Valid reports will be acknowledged and addressed promptly.

## Scope

The following are in scope for security reports:

- Authentication or authorization bypasses in the Swarm Server API.
- Data leakage of caller PII (phone numbers, transcripts).
- Injection vulnerabilities in the telemetry endpoint.
- Logic flaws in the on-device risk scoring pipeline.

## Privacy Commitment

- **Anonymization:** All caller numbers are SHA-256 hashed before storage or transmission.
- **Zero-Transcript Telemetry:** No conversation audio or raw transcripts are ever sent to the server. We enforce a strictly binary telemetry format (10.25 bytes).
- **PII Scrubbing:** All local transcripts are scrubbed on-device to mask names, OTPs, and account identifiers before storage.
- **Right to Erasure:** Users can purge all local and remote data associations via the app settings.

## V1.1 Architectural Security Implementation

CyberGuard AI implements military-grade anti-tampering and on-device security protocols:

### 🛡️ The Vault (Model Protection)
- **AES-GCM Encryption:** All ONNX AI models are stored as AES-256 encrypted `.enc` files.
- **Hardware-Backed Keystore:** Master keys are generated and locked inside the Android Hardware Keystore (TEE).
- **Dynamic Key Derivation:** Keys are mixed with hardware-unique salts at runtime in the NDK layer to prevent static extraction.
- **Immediate Cache Purge:** Decrypted weights are purged from disk immediately after being loaded into RAM.

### 🗄️ Storage Security
- **SQLCipher:** Local SQLite databases are encrypted at rest using 256-bit AES.
- **Hardware-Bound Passphrases:** Database keys are managed via the Android Keystore.

### 🌐 Network Hardening
- **TLS Certificate Pinning:** Protects against Man-in-the-Middle (MITM) attacks.
- **5G URLLC Prioritization:** Uses DSCP `0xB8` for priority routing on 5G slices.

### 🚀 Environmental Security
- **Root Detection:** Scans for `su` binaries, test-keys, and `rw` system mounts.
- **Environmental Tripwires:** Terminates execution immediately if the execution environment is compromised.
