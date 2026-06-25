package com.example.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.PrivacyFragment

class PrivacyActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // Simple container to host the fragment
                AndroidView(factory = { ctx ->
                    androidx.fragment.app.FragmentContainerView(ctx).apply {
                        id = android.R.id.content
                        supportFragmentManager.beginTransaction()
                            .replace(id, PrivacyFragment())
                            .commit()
                    }
                })
            }
        }
    }
}
