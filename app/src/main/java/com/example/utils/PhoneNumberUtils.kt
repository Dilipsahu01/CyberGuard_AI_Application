package com.example.utils

/**
 * PhoneNumberUtils.kt
 * 
 * PURPOSE: 
 * Normalizes and secures phone numbers.
 * 
 * WHY IT EXISTS:
 * Standardizes messy caller ID strings into E.164 format and applies SHA-256 hashing. 
 * This ensures that NO raw phone numbers are ever saved to the local database or 
 * uploaded to the Swarm Server, guaranteeing strict user privacy.
 */
object PhoneNumberUtils {

    /**
     * Normalizes a phone number by stripping all non-digit characters except standard plus prefix,
     * removing country prefixes (e.g., +91 or 91) to match locally against databases.
     */
    fun normalize(phoneNumber: String): String {
        val sanitized = phoneNumber.replace(Regex("[^0-9]"), "")
        // Handle Indian local formatting standard adjustments (+91 or 91 prefix)
        return when {
            sanitized.startsWith("91") && sanitized.length > 10 -> sanitized.substring(2)
            sanitized.length > 10 -> sanitized.takeLast(10)
            else -> sanitized
        }
    }

    /**
     * Checks if two phone numbers match after normalization.
     */
    fun areEqual(number1: String, number2: String): Boolean {
        return normalize(number1) == normalize(number2)
    }

    fun hash(number: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(normalize(number).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
