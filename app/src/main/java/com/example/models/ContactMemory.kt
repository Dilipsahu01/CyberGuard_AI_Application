package com.example.models

/**
 * ContactMemory.kt
 * 
 * PURPOSE: 
 * Defines the 9-byte bit-packed struct used to combat "Pig Butchering" and Romance Scams.
 * It tracks long-term trust, intimacy, and financial requests across multiple phone calls.
 * 
 * WHY IT EXISTS:
 * Standard phone dictionaries consume too much RAM. By aggressively packing 8 different 
 * emotional and historical metrics into exactly 9 raw bytes, this ensures the app can 
 * store history for 10,000+ scammers using virtually zero disk space on a budget phone.
 */
import java.io.Serializable

class ContactMemory(var memoryData: ByteArray = ByteArray(9)) : Serializable {

    // Byte 0 Flags
    private fun getFlag(bit: Int): Boolean = (memoryData[0].toInt() and (1 shl bit)) != 0
    private fun setFlag(bit: Int, value: Boolean) {
        var b = memoryData[0].toInt()
        b = if (value) b or (1 shl bit) else b and (1 shl bit).inv()
        memoryData[0] = b.toByte()
    }

    var metInPerson: Boolean
        get() = getFlag(0)
        set(value) = setFlag(0, value)

    var videoCalled: Boolean
        get() = getFlag(1)
        set(value) = setFlag(1, value)

    var askedMoney: Boolean
        get() = getFlag(2)
        set(value) = setFlag(2, value)

    var askedOtp: Boolean
        get() = getFlag(3)
        set(value) = setFlag(3, value)

    var sharedDocs: Boolean
        get() = getFlag(4)
        set(value) = setFlag(4, value)

    var urgencyUsed: Boolean
        get() = getFlag(5)
        set(value) = setFlag(5, value)

    var secrecyAsked: Boolean
        get() = getFlag(6)
        set(value) = setFlag(6, value)

    // Byte 1: Trust (3 bits) + Intimacy (3 bits)
    var trustLevel: Int
        get() = memoryData[1].toInt() and 0x07
        set(value) {
            val v = value.coerceIn(0, 7)
            memoryData[1] = ((memoryData[1].toInt() and 0x07.inv()) or v).toByte()
        }

    var intimacyLevel: Int
        get() = (memoryData[1].toInt() shr 3) and 0x07
        set(value) {
            val v = value.coerceIn(0, 7)
            memoryData[1] = ((memoryData[1].toInt() and 0x38.inv()) or (v shl 3)).toByte()
        }

    // Byte 2: Emotional Intensity (3 bits) + Platform (3 bits)
    var emotionalIntensity: Int
        get() = memoryData[2].toInt() and 0x07
        set(value) {
            val v = value.coerceIn(0, 7)
            memoryData[2] = ((memoryData[2].toInt() and 0x07.inv()) or v).toByte()
        }

    var contactPlatform: Int
        get() = (memoryData[2].toInt() shr 3) and 0x07
        set(value) {
            val v = value.coerceIn(0, 7)
            memoryData[2] = ((memoryData[2].toInt() and 0x38.inv()) or (v shl 3)).toByte()
        }

    // Byte 3: Days Known (0-255)
    var daysKnown: Int
        get() = memoryData[3].toInt() and 0xFF
        set(value) { memoryData[3] = value.coerceIn(0, 255).toByte() }

    // Byte 4: Total Calls
    var totalCalls: Int
        get() = memoryData[4].toInt() and 0xFF
        set(value) { memoryData[4] = value.coerceIn(0, 255).toByte() }

    // Byte 5-6: Money Requested (Little Endian, uint16)
    var moneyRequested: Int
        get() = (memoryData[5].toInt() and 0xFF) or ((memoryData[6].toInt() and 0xFF) shl 8)
        set(value) {
            val v = value.coerceIn(0, 65535)
            memoryData[5] = (v and 0xFF).toByte()
            memoryData[6] = ((v shr 8) and 0xFF).toByte()
        }

    // Byte 7: Last Contact Ago
    var lastContactAgo: Int
        get() = memoryData[7].toInt() and 0xFF
        set(value) { memoryData[7] = value.coerceIn(0, 255).toByte() }

    // Byte 8: Romance Score
    var romanceScore: Int
        get() = memoryData[8].toInt() and 0xFF
        set(value) { memoryData[8] = value.coerceIn(0, 100).toByte() }

    fun computeRomanceScore(): Int {
        var score = 0
        if (intimacyLevel >= 5) score += 25
        if (!metInPerson) score += 20
        if (!videoCalled) score += 15
        if (askedMoney) score += 25
        if (emotionalIntensity >= 5) score += 10

        val velocity = (intimacyLevel * 10) / Math.max(daysKnown, 1)
        if (velocity >= 3) score += 10
        if (velocity >= 7) score += 15

        if (contactPlatform == 5) score += 10 // dating app
        if (contactPlatform == 3 || contactPlatform == 4) score += 5 // FB/Insta
        if (secrecyAsked && askedMoney) score += 20

        romanceScore = score.coerceAtMost(100)
        return romanceScore
    }
    
    fun computeTrustVelocity(): Int {
        return ((intimacyLevel * 10) / Math.max(daysKnown, 1)).coerceAtMost(100)
    }

    fun toBytes(): ByteArray = memoryData
    
    companion object {
        fun fromBytes(data: ByteArray): ContactMemory {
            val mem = ContactMemory()
            System.arraycopy(data, 0, mem.memoryData, 0, Math.min(data.size, 9))
            return mem
        }
    }
}
