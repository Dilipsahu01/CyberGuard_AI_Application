package com.example.models

data class IntentScores(
    val urgency: Int = 0,     // 0 to 100
    val financial: Int = 0,   // 0 to 100
    val coercion: Int = 0,    // 0 to 100
    val intimacy: Int = 0,    // 0 to 100
    val trust: Int = 0        // 0 to 100
)
