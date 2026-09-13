package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey val phoneNumber: String,
    val contactName: String? = null,
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)
