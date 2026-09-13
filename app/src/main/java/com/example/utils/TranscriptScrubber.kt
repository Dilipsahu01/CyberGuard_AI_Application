package com.example.utils

/**
 * TranscriptScrubber.kt
 *
 * PURPOSE:
 * Implements on-device PII (Personally Identifiable Information) scrubbing
 * for conversation transcripts before they are saved to local storage or
 * handled by any telemetry layers.
 *
 * WHY IT EXISTS:
 * Strict compliance with the DPDP (Digital Personal Data Protection) Act 2023.
 * Ensures that if a user's transcript contains names, bank account numbers,
 * or OTPs, they are masked locally to protect user privacy.
 */
object TranscriptScrubber {

    // Regex for 4-8 digit OTPs
    private val OTP_REGEX = Regex("\\b\\d{4,8}\\b")

    // Regex for long digit strings (likely bank accounts, Aadhar numbers)
    private val ACCOUNT_REGEX = Regex("\\b\\d{10,16}\\b")

    /**
     * Masks PII in the given transcript string.
     */
    fun scrub(transcript: String): String {
        if (transcript.isEmpty()) return ""

        var scrubbed = transcript

        // 1. Mask OTPs
        scrubbed = scrubbed.replace(OTP_REGEX, "[OTP]")

        // 2. Mask Account Numbers/Aadhar
        scrubbed = scrubbed.replace(ACCOUNT_REGEX, "[ACCOUNT_ID]")

        // 3. Simple Name masking attempt: if we see "my name is X", mask X.
        // This is a basic NER proxy for a prototype.
        val namePattern = Regex("(?i)(my name is|mera naam|i am)\\s+([a-zA-Z]+)")
        scrubbed = scrubbed.replace(namePattern) { matchResult ->
            "${matchResult.groupValues[1]} [NAME]"
        }

        return scrubbed
    }
}
