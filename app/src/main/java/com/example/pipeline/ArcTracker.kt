package com.example.pipeline

/**
 * ArcTracker.kt
 * 
 * PURPOSE: 
 * A State Machine designed to catch "Slow-Burn" social engineering tactics.
 * 
 * WHY IT EXISTS:
 * Scammers do not ask for money immediately. They establish a "Trust" phase, introduce a 
 * "Problem", and finally make a "Request". By tracking these phase transitions over time, 
 * the AI catches sophisticated scammers that evade basic keyword detection.
 */
enum class ArcPhase {
    UNKNOWN, INTRO, TRUST_BUILD, PROBLEM_ESTABLISH, REQUEST, CLOSE
}

class ArcTracker {
    private val phasesSeen = mutableListOf<ArcPhase>()
    private val detectedTopics = mutableSetOf<String>()
    private var turnCount = 0
    var arcScore = 0f
        private set

    private val topicKeywords = mapOf(
        "authority_claim" to listOf("bank", "rbi", "police", "cbi", "court", "trai", "income tax", "epfo", "insurance", "government", "officer", "department", "social security", "irs", "customs", "federal", "law enforcement"),
        "personal_info" to listOf("aadhaar", "pan", "account number", "date of birth", "address", "registered mobile", "kyc", "nominee", "customer id", "ssn", "credit card", "debit card", "bank account"),
        "problem_frame" to listOf("blocked", "suspended", "expired", "issue", "problem", "case", "complaint", "fir", "warrant", "fraud", "illegal", "pending", "arrest", "lawsuit", "unauthorized", "compromised", "money laundering", "detained", "held at customs"),
        "financial_request" to listOf("otp", "pin", "cvv", "transfer", "upi", "payment", "send money", "deposit", "refund", "cashback", "fine", "penalty", "fee", "press 1", "press 2", "dial 1", "call us back", "toll free", "verify your", "gift card", "wire transfer", "bitcoin"),
        "brand_impersonation" to listOf("amazon", "apple", "microsoft", "google", "paypal", "netflix", "walmart", "mcafee", "norton"),
        "delivery_scam" to listOf("package", "parcel", "delivery", "shipment", "courier", "fedex", "ups", "dhl", "tracking number"),
        "trust_signal" to listOf("don't worry", "we're here to help", "your account is safe", "routine check", "verification", "for your security", "valued customer"),
        "urgency_signal" to listOf("immediately", "urgent", "last chance", "24 hours", "today only", "action required", "or else", "otherwise", "will be blocked", "as soon as possible", "right away", "do not ignore"),
        "secrecy_signal" to listOf("don't tell anyone", "keep this confidential", "between us", "sensitive matter", "do not discuss", "keep it private"),
        "prize_signal" to listOf("congratulations", "you have been selected", "you won", "winner", "prize", "lottery", "reward", "free vacation", "exclusive offer")
    )

    fun reset() {
        phasesSeen.clear()
        detectedTopics.clear()
        turnCount = 0
        arcScore = 0f
    }

    fun update(text: String) {
        val textLower = text.lowercase()
        val newTopics = mutableSetOf<String>()
        
        for ((topic, keywords) in topicKeywords) {
            if (keywords.any { it in textLower }) {
                newTopics.add(topic)
            }
        }
        
        detectedTopics.addAll(newTopics)
        turnCount++
        
        val detectedPhases = classifyPhases(newTopics)
        for (phase in detectedPhases) {
            if (phasesSeen.isEmpty() || phasesSeen.last() != phase) {
                phasesSeen.add(phase)
            }
        }
        
        arcScore = computeArcScore()
    }

    private fun classifyPhases(topics: Set<String>): List<ArcPhase> {
        val phases = mutableListOf<ArcPhase>()
        if ("prize_signal" in topics) phases.add(ArcPhase.INTRO)
        if ("trust_signal" in topics || "authority_claim" in topics || "personal_info" in topics) phases.add(ArcPhase.TRUST_BUILD)
        if ("problem_frame" in topics || "urgency_signal" in topics) phases.add(ArcPhase.PROBLEM_ESTABLISH)
        if ("financial_request" in topics) phases.add(ArcPhase.REQUEST)
        return phases
    }

    private fun computeArcScore(): Float {
        var score = 0f
        
        // Classic arc patterns
        if (ArcPhase.TRUST_BUILD in phasesSeen && ArcPhase.REQUEST in phasesSeen) score += 0.5f
        if (ArcPhase.PROBLEM_ESTABLISH in phasesSeen && ArcPhase.REQUEST in phasesSeen) score += 0.3f
        
        // Topic-based boosts
        if ("secrecy_signal" in detectedTopics) score += 0.4f
        if ("authority_claim" in detectedTopics && "financial_request" in detectedTopics) score += 0.3f
        if ("urgency_signal" in detectedTopics && "financial_request" in detectedTopics) score += 0.2f
        if ("brand_impersonation" in detectedTopics && "financial_request" in detectedTopics) score += 0.4f
        if ("brand_impersonation" in detectedTopics && "problem_frame" in detectedTopics) score += 0.3f
        if ("delivery_scam" in detectedTopics && "authority_claim" in detectedTopics) score += 0.3f
        if ("delivery_scam" in detectedTopics && "problem_frame" in detectedTopics) score += 0.2f
        if ("authority_claim" in detectedTopics && "personal_info" in detectedTopics) score += 0.2f
        if ("prize_signal" in detectedTopics && "financial_request" in detectedTopics) score += 0.4f
        
        // Slow-burn bonus
        if (turnCount > 8 && ArcPhase.TRUST_BUILD in phasesSeen && ArcPhase.REQUEST in phasesSeen) score += 0.2f
        
        return score.coerceAtMost(1.0f)
    }
}
