# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please do **not** open a public GitHub issue.

Instead, contact the author directly via GitHub:

**https://github.com/Dilipsahu01**

Please include:
- A clear description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested fix (optional)

You will receive a response within 72 hours. Valid reports will be acknowledged and addressed promptly.

## Scope

The following are in scope for security reports:

- Authentication or authorization bypasses in the Go Swarm Server API
- Data leakage of caller PII (phone numbers, transcripts)
- Injection vulnerabilities in the telemetry endpoint
- Logic flaws in the on-device risk scoring pipeline that could suppress true positives

## Out of Scope

- Issues in third-party dependencies (report to their maintainers directly)
- Theoretical vulnerabilities without a working proof-of-concept
- Social engineering attacks

## Privacy Commitment

All caller numbers are SHA-256 hashed before storage or transmission.
No conversation audio or raw transcripts are ever sent to the server. 

## V1.1 Architectural Security Features (The Vault)
CyberGuard AI implements military-grade anti-tampering and on-device security protocols:
- **AES-GCM Encrypted Neural Networks:** All ONNX AI models (`silero_vad`, `minilm_nlp`) are stored on disk as AES-256 encrypted `.enc` files and are decrypted exclusively into volatile RAM at runtime.
- **Hardware-Backed Keystore:** The master decryption key is generated and locked inside the Android Hardware Keystore, rendering extraction physically impossible on compromised devices.
- **SHA-256 Integrity Baselines:** The AI pipeline cryptographically verifies all models against signed hashes before execution to prevent neural network poisoning.
- **NDK Obfuscation:** Critical API endpoints and cryptographic salts are hidden in `libcyberguard_secrets.so` (C++) to thwart reverse-engineering.
- **Environmental Tripwires:** The background service features proactive Root Detection (`su` binaries, `test-keys`, `rw` mounts) and terminates execution immediately if the execution environment is compromised.
