package com.example.pipeline

/**
 * RegexGate.kt
 * 
 * PURPOSE: 
 * A high-speed, deterministic word-matching layer tailored for the Indian threat landscape.
 * 
 * WHY IT EXISTS:
 * While the neural networks understand "intent", we need an instantaneous kill-switch for 
 * highly specific local scams (e.g., "TRAI disconnect", "KBC lottery", "FedEx customs").
 * It uses zero CPU battery and provides instant baseline scoring.
 */
class RegexGate {
    val patterns = listOf(
        Regex("\\botp\\b", RegexOption.IGNORE_CASE) to 35,
        Regex("digital.?arrest", RegexOption.IGNORE_CASE) to 40,
        Regex("\\banydesk\\b", RegexOption.IGNORE_CASE) to 35,
        Regex("police.{0,20}coming", RegexOption.IGNORE_CASE) to 30,
        Regex("account.{0,20}(freeze|block|suspend)", RegexOption.IGNORE_CASE) to 25,
        Regex("\\bcbi\\b", RegexOption.IGNORE_CASE) to 30,
        Regex("(wire|transfer).{0,20}(money|funds|rupee)", RegexOption.IGNORE_CASE) to 35,
        Regex("verify.{0,20}(account|identity|aadhar|pan)", RegexOption.IGNORE_CASE) to 20,
        Regex("\\bgift.?card\\b", RegexOption.IGNORE_CASE) to 30,
        Regex("(share|send).{0,10}(pin|password|cvv)", RegexOption.IGNORE_CASE) to 40,
        Regex("(aadhaar|aadhar).{0,20}(number|link|otp)", RegexOption.IGNORE_CASE) to 35,
        Regex("\\bteamviewer\\b", RegexOption.IGNORE_CASE) to 35,
        Regex("\\b(fedex|customs).{0,20}(parcel|package|duty)\\b", RegexOption.IGNORE_CASE) to 35,
        Regex("sim.{0,10}(block|deactivate|upgrade)", RegexOption.IGNORE_CASE) to 30,
        Regex("\\btrai\\b", RegexOption.IGNORE_CASE) to 30,
        Regex("electricity.{0,15}(disconnect|cut)", RegexOption.IGNORE_CASE) to 35,
        Regex("kbc.{0,10}lottery", RegexOption.IGNORE_CASE) to 40,
        Regex("paytm.{0,10}kyc", RegexOption.IGNORE_CASE) to 35,
        Regex("crypto.{0,15}(invest|return|profit)", RegexOption.IGNORE_CASE) to 25
    )

    private val matchedPatterns = mutableSetOf<Regex>()
    private val matchedWords = mutableListOf<String>()

    fun check(newText: String): Pair<Int, String> = synchronized(this) {
        if (newText.isNotEmpty()) {
            for ((regex, _) in patterns) {
                if (!matchedPatterns.contains(regex)) {
                    val matchResult = regex.find(newText)
                    if (matchResult != null) {
                        matchedPatterns.add(regex)
                        matchedWords.add(matchResult.value)
                    }
                }
            }
        }

        var totalScore = 0
        for ((regex, weight) in patterns) {
            if (matchedPatterns.contains(regex)) {
                totalScore += weight
            }
        }

        val score = totalScore.coerceAtMost(100)
        val hitWordString = if (matchedWords.isNotEmpty()) matchedWords.joinToString(", ") else ""
        return Pair(score, hitWordString)
    }

    fun reset() = synchronized(this) {
        matchedPatterns.clear()
        matchedWords.clear()
    }
}
