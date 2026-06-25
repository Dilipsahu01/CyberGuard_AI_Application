package com.example.utils

object Constants {
    const val ACTION_SCAM_DETECTED = "com.cyberguard.SCAM_DETECTED"
    const val ACTION_RISK_UPDATE = "com.cyberguard.RISK_UPDATE"
    const val ACTION_DISCONNECT_CALL = "com.cyberguard.DISCONNECT_CALL"
    const val ACTION_CALL_ENDED = "com.cyberguard.CALL_ENDED"

    // Extras
    const val EXTRA_RISK_SCORE = "risk_score"
    const val EXTRA_TRANSCRIPT = "transcript"
    const val EXTRA_HIT_KEYWORD = "hit_keyword"
    const val EXTRA_CALLER_NUMBER = "caller_number"
}
