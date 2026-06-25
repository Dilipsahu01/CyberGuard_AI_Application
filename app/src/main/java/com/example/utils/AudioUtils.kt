package com.example.utils

object AudioUtils {

    /**
     * Converts a 16-bit signed PCM short array into a normalized float array (-1.0f to 1.0f).
     */
    fun shortToFloat(shorts: ShortArray, size: Int = shorts.size): FloatArray {
        val floats = FloatArray(size)
        shortToFloat(shorts, floats, size)
        return floats
    }

    /**
     * Non-allocating conversion that writes directly into the provided destination buffer,
     * zero-padding any remainder if the source size is smaller than the destination length.
     */
    fun shortToFloat(shorts: ShortArray, dest: FloatArray, size: Int) {
        val limit = Math.min(size, dest.size)
        for (i in 0 until limit) {
            dest[i] = shorts[i] / 32768.0f
        }
        if (limit < dest.size) {
            java.util.Arrays.fill(dest, limit, dest.size, 0f)
        }
    }
}
