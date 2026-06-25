package com.example.models

data class RiskResult(
    val score: Int = 0,
    val transcript: String = "",
    val regexScore: Int = 0,
    val hitWord: String = "",
    val intents: IntentScores = IntentScores(),
    val stage: String = "",
    val isRoboVoice: Boolean = false
)
