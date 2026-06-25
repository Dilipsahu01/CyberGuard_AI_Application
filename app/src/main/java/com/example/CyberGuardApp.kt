package com.example

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.ui.theme.MyApplicationTheme

class CyberGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure theme preference is loaded early (used by Compose)
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // No explicit UI here; the theme will be queried by Compose via preference
    }
}
