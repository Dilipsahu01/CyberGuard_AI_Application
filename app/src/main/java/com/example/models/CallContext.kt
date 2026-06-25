package com.example.models

/**
 * CallContext.kt
 * 
 * PURPOSE: 
 * Gathers all hardware, network, and contact-book context about the current call.
 * 
 * WHY IT EXISTS:
 * Packs 14 different metrics (e.g., isVoip, STIR/SHAKEN failure, days known) into 
 * the highly compressed 82-bit Swarm Payload for telemetry upload.
 */
import java.io.Serializable

data class CallContext(
    // 8 Integer Telemetry Signals
    val daysKnown: Int = 0,
    val totalCalls: Int = 0,
    val missedCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val averageDurationSec: Int = 0,
    val timeSinceLastCall: Int = 0,
    val spamReportsLocal: Int = 0,
    val networkTrustScore: Int = 0,

    // 6 Boolean Telemetry Signals
    val isVoip: Boolean = false,
    val stirShakenFailed: Boolean = false,
    val rapidCallback: Boolean = false,
    val hiddenCallerId: Boolean = false,
    val internationalRouting: Boolean = false,
    val notInContacts: Boolean = true
) : Serializable {

    /**
     * Packs the telemetry signals into a compact 10.25-byte (82 bits) Swarm Payload 
     * perfectly mirroring the C++ Stage 7 bitfield spec.
     */
    fun packToBinary(bloomHit: Boolean, riskResult: RiskResult, callerHashPrefix: Int): ByteArray {
        val bytes = ByteArray(11) // 10.25 bytes -> 11 bytes ceiling
        var bitOffset = 0
        
        fun writeBits(value: Int, numBits: Int) {
            var v = value
            var bitsToWrite = numBits
            while (bitsToWrite > 0) {
                val byteIndex = bitOffset / 8
                val bitIndex = bitOffset % 8
                val bitsAvailableInByte = 8 - bitIndex
                val bitsToPut = Math.min(bitsToWrite, bitsAvailableInByte)
                
                val mask = ((1 shl bitsToPut) - 1)
                val shiftedValue = (v and mask) shl bitIndex
                bytes[byteIndex] = (bytes[byteIndex].toInt() or shiftedValue).toByte()
                
                v = v shr bitsToPut
                bitOffset += bitsToPut
                bitsToWrite -= bitsToPut
            }
        }

        // 14 bits for anonymised caller identity
        writeBits(callerHashPrefix.coerceIn(0, 16383), 14)
        
        // 4 bits boolean flags
        writeBits(if (bloomHit) 1 else 0, 1)
        writeBits(if (isVoip) 1 else 0, 1)
        writeBits(if (stirShakenFailed) 1 else 0, 1)
        writeBits(if (rapidCallback) 1 else 0, 1)
        
        // 16 bits integer metrics
        writeBits(daysKnown.coerceIn(0, 255), 8)
        writeBits(totalCalls.coerceIn(0, 255), 8)
        
        // 6 bits Regex risk
        writeBits(riskResult.regexScore.coerceIn(0, 63), 6)
        
        // 35 bits NLP Intent (5 x 7 bits, score 0-100 fits in 7 bits)
        writeBits(riskResult.intents.intimacy.coerceIn(0, 127), 7)
        writeBits(riskResult.intents.urgency.coerceIn(0, 127), 7)
        writeBits(riskResult.intents.trust.coerceIn(0, 127), 7)
        writeBits(riskResult.intents.financial.coerceIn(0, 127), 7)
        writeBits(riskResult.intents.coercion.coerceIn(0, 127), 7)
        
        // 7 bits final ensemble risk score
        writeBits(riskResult.score.coerceIn(0, 127), 7)

        return bytes
    }
}
