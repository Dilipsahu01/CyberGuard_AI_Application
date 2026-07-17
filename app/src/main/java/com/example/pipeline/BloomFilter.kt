package com.example.pipeline

import java.util.BitSet
import com.example.utils.PhoneNumberUtils

class BloomFilter(private val bitSize: Int = 10000000, private val numHash: Int = 3) {
    companion object {
        @Volatile private var sharedBitSet: BitSet? = null
        @Volatile private var isInitialized = false
        private val lock = Any()
        
        private fun getSharedBitSet(size: Int): BitSet {
            synchronized(lock) {
                if (sharedBitSet == null) {
                    sharedBitSet = BitSet(size)
                }
                return sharedBitSet!!
            }
        }
    }

    private val bitSet = getSharedBitSet(bitSize)

    init {
        synchronized(lock) {
            if (!isInitialized) {
                isInitialized = true
            }
        }
    }

    /**
     * V1.1_Updates Section 6: Offline Threat Intelligence
     * Loads the entire pre-built DOT scammer database from Room into the BloomFilter 
     * for 0ms latency detection.
     */
    fun loadFromDatabase(context: android.content.Context) {
        kotlin.concurrent.thread {
            try {
                val db = com.example.models.ScamDatabase.getDatabase(context)
                val cursor = db.query("SELECT phone_number FROM dot_scammers", null)
                cursor.use {
                    while (it.moveToNext()) {
                        add(it.getString(0))
                    }
                }
                android.util.Log.i("BloomFilter", "Successfully loaded offline DOT scam blacklist into memory!")
            } catch (e: Exception) {
                android.util.Log.e("BloomFilter", "Failed to load offline threat intelligence: ${e.message}")
            }
        }
    }

    /**
     * MurmurHash3 implementation for 32-bit hash.
     */
    private fun murmurHash3(data: String): Int {
        var h1 = 0xabcdef12.toInt()
        val bytes = data.toByteArray(Charsets.UTF_8)
        val length = bytes.size
        val nblocks = length / 4
        
        for (i in 0 until nblocks) {
            val index = i * 4
            var k1 = (bytes[index].toInt() and 0xff) or
                     ((bytes[index + 1].toInt() and 0xff) shl 8) or
                     ((bytes[index + 2].toInt() and 0xff) shl 16) or
                     ((bytes[index + 3].toInt() and 0xff) shl 24)
            
            k1 *= 0xcc9e2d51.toInt()
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= 0x1b873593
            
            h1 = h1 xor k1
            h1 = (h1 shl 13) or (h1 ushr 19)
            h1 = (h1 * 5) + 0xe6546b64.toInt()
        }
        
        var k1 = 0
        val tailIndex = nblocks * 4
        val remaining = length - tailIndex
        if (remaining >= 3) {
            k1 = k1 or ((bytes[tailIndex + 2].toInt() and 0xff) shl 16)
        }
        if (remaining >= 2) {
            k1 = k1 or ((bytes[tailIndex + 1].toInt() and 0xff) shl 8)
        }
        if (remaining >= 1) {
            k1 = k1 or (bytes[tailIndex].toInt() and 0xff)
            k1 *= 0xcc9e2d51.toInt()
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= 0x1b873593
            h1 = h1 xor k1
        }
        
        h1 = h1 xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)
        
        return h1
    }

    /**
     * xxHash32 implementation for high dispersion 32-bit hash.
     */
    private fun xxHash32(data: String): Int {
        val prime1 = 2654435761L
        val prime2 = 2246822519L
        val prime3 = 3266489917L
        val prime4 = 668265263L
        val prime5 = 374761393L

        val bytes = data.toByteArray(Charsets.UTF_8)
        val len = bytes.size
        var h32: Long
        var index = 0

        if (len >= 16) {
            val limit = len - 16
            var v1 = prime1 + prime2
            var v2 = prime2
            var v3 = 0L
            var v4 = -prime1

            while (index <= limit) {
                val read4 = { offset: Int ->
                    (bytes[index + offset].toLong() and 0xFF) or
                    ((bytes[index + offset + 1].toLong() and 0xFF) shl 8) or
                    ((bytes[index + offset + 2].toLong() and 0xFF) shl 16) or
                    ((bytes[index + offset + 3].toLong() and 0xFF) shl 24)
                }

                v1 += read4(0) * prime2
                v1 = ((v1 shl 13) or (v1 ushr 19)) and 0xFFFFFFFFL
                v1 *= prime1

                v2 += read4(4) * prime2
                v2 = ((v2 shl 13) or (v2 ushr 19)) and 0xFFFFFFFFL
                v2 *= prime1

                v3 += read4(8) * prime2
                v3 = ((v3 shl 13) or (v3 ushr 19)) and 0xFFFFFFFFL
                v3 *= prime1

                v4 += read4(12) * prime2
                v4 = ((v4 shl 13) or (v4 ushr 19)) and 0xFFFFFFFFL
                v4 *= prime1

                index += 16
            }

            h32 = ((v1 shl 1) or (v1 ushr 31)) + 
                  ((v2 shl 7) or (v2 ushr 25)) + 
                  ((v3 shl 12) or (v3 ushr 20)) + 
                  ((v4 shl 18) or (v4 ushr 14))
            h32 = h32 and 0xFFFFFFFFL
        } else {
            h32 = prime5
        }

        h32 += len

        while (index <= (len - 4)) {
            val readVal = (bytes[index].toLong() and 0xFF) or
                          ((bytes[index + 1].toLong() and 0xFF) shl 8) or
                          ((bytes[index + 2].toLong() and 0xFF) shl 16) or
                          ((bytes[index + 3].toLong() and 0xFF) shl 24)
            h32 += readVal * prime3
            h32 = (((h32 shl 17) or (h32 ushr 15)) and 0xFFFFFFFFL) * prime4
            index += 4
        }

        while (index < len) {
            h32 += (bytes[index].toLong() and 0xFF) * prime5
            h32 = (((h32 shl 11) or (h32 ushr 21)) and 0xFFFFFFFFL) * prime1
            index++
        }

        h32 = h32 xor (h32 ushr 15)
        h32 = (h32 * prime2) and 0xFFFFFFFFL
        h32 = h32 xor (h32 ushr 13)
        h32 = (h32 * prime3) and 0xFFFFFFFFL
        h32 = h32 xor (h32 ushr 16)

        return h32.toInt()
    }

    /**
     * Uses double-hashing to generate k hash values.
     */
    private fun getHashes(input: String): IntArray {
        val hashes = IntArray(numHash)
        val hash1 = murmurHash3(input)
        val hash2 = xxHash32(input)
        
        for (i in 0 until numHash) {
            val combinedHash = hash1 + (i * hash2)
            hashes[i] = (combinedHash and 0x7FFFFFFF) % bitSize
        }
        return hashes
    }

    fun add(input: String) {
        synchronized(lock) {
            val hashes = getHashes(PhoneNumberUtils.normalize(input))
            for (hash in hashes) {
                bitSet.set(hash)
            }
        }
    }

    fun check(input: String): Boolean {
        synchronized(lock) {
            val hashes = getHashes(PhoneNumberUtils.normalize(input))
            for (hash in hashes) {
                if (!bitSet[hash]) {
                    return false
                }
            }
            return true
        }
    }
}
