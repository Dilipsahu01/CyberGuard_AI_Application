package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {
    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "dark_mode") {
            // Force recomposition by updating a dummy state
            // In Compose, we will read the pref directly each recomposition, so no extra action needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register preference listener to react to external changes
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        setContent {
            MyApplicationTheme {
                SettingsScreen(prefs) { isEnabled ->
                    // Update preference and recreate activity to apply new theme
                    prefs.edit().putBoolean("dark_mode", isEnabled).apply()
                    // Recreate to apply theme immediately
                    recreate()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister listener to avoid leaks
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}

@Composable
fun SettingsScreen(prefs: SharedPreferences, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val isDark = prefs.getBoolean("dark_mode", true)
    var checked by remember { mutableStateOf(isDark) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("App Theme", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Dark Mode")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        onToggle(it)
                    }
                )
            }
            // Privacy policy button
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    // Launch the PrivacyActivity
                    context.startActivity(Intent(context, com.example.ui.PrivacyActivity::class.java))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0F))
            ) {
                Icon(Icons.Default.Info, contentDescription = "Privacy Info")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Policy", color = Color.White)
            }
        }
    }
}
