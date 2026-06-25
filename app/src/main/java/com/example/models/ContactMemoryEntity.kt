package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_memory")
data class ContactMemoryEntity(
    @PrimaryKey
    val numberHash: String,
    val memory: ByteArray,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ContactMemoryEntity
        if (numberHash != other.numberHash) return false
        if (!memory.contentEquals(other.memory)) return false
        if (updatedAt != other.updatedAt) return false
        return true
    }
    override fun hashCode(): Int {
        var result = numberHash.hashCode()
        result = 31 * result + memory.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
