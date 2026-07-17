package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scam_calls")
data class ScamCallEntity(
    @PrimaryKey
    val phoneNumber: String,
    val threatScore: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isFlagged: Boolean
)
