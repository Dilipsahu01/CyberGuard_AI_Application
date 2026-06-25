package com.example.pipeline

/**
 * EnsembleEngine.kt
 * 
 * PURPOSE: 
 * This is the final Decision Math layer. It fuses the outputs of all isolated AI models 
 * into a single deterministic 0-100% Risk Score.
 * 
 * WHY IT EXISTS:
 * AI models can hallucinate. By blending Semantic NLP Intents (MiniLM) with rigid mathematical 
 * triggers (RegexGate), conversational state tracking (ArcTracker), and Pig Butchering memory 
 * (ContactMemory), this engine drastically reduces False Positives before terminating a call.
 */
import com.example.models.IntentScores

class EnsembleEngine {
    fun calculate(regexScore: Int, intents: IntentScores, arcScore: Float = 0f, romanceScore: Int = 0): Int {
        // High-importance intent indicators using inline primitive maxOf functions (zero heap allocation)
        val maxCoreIntent = maxOf(intents.financial, intents.urgency, intents.coercion)
        val maxSupportIntent = maxOf(intents.intimacy, intents.trust)
        
        // Non-linear ensemble model
        var calculatedScore = 0f

        if (regexScore > 0) {
            calculatedScore += regexScore * 0.45f
        }
        
        calculatedScore += if (maxCoreIntent > 0) {
            maxCoreIntent * 0.45f
        } else {
            maxSupportIntent * 0.20f
        }

        // Synergy bonus: If the call has BOTH a financial keyword/intent AND urgency, trigger a higher warning bump!
        if (((intents.financial > 40) || (regexScore > 30)) && (intents.urgency > 45)) {
            calculatedScore += 18f
        }

        // Synergy bonus: Coercion + police keywords are extremely dangerous scams
        if ((intents.coercion > 40) && (regexScore > 25)) {
            calculatedScore += 15f
        }

        // Slow-Burn Arc Tracker Bonus
        if (arcScore > 0f) {
            calculatedScore += (arcScore * 100f) // arcScore is 0.0 to 1.0, scales to 100 max
        }

        // Romance / Pig Butchering Multi-Session Bonus
        if (romanceScore > 40) {
            calculatedScore += (romanceScore * 0.5f) // high romance score directly bumps final risk
        }

        // Clip maximum score to 100
        val finalScore = kotlin.math.round(calculatedScore).toInt().coerceAtMost(100)
        return finalScore.coerceAtLeast(0)
    }
}
