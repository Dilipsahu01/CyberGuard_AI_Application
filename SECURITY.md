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
