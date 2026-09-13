package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerNumber: String,
    val timestamp: Long,
    val riskScore: Int,
    val isScam: Boolean,
    val transcript: String,
    val hitKeywords: String,
    val durationSeconds: Int,
    val wasBlocked: Boolean,
    val direction: Int = -1, // -1 = UNKNOWN, 0 = INCOMING, 1 = OUTGOING, 2 = MISSED
    val userFeedback: String? = null // "CONFIRMED_SCAM", "FALSE_POSITIVE", or null
)
