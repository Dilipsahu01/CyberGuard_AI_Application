package com.example

/**
 * MainActivity.kt
 * 
 * PURPOSE: 
 * The central UI dashboard for the CyberGuard app.
 * 
 * WHY IT EXISTS:
 * Displays real-time threat telemetry, blocked call history, and allows the user 
 * to manually manage their whitelist. It observes the `CallLogRepository` flows 
 * to automatically update the UI without manual refreshes.
 */
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.example.security.EnvironmentGuard
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CallLog
import com.example.models.CallLogRepository
import com.example.models.ScamDatabase
import com.example.ui.PipelineViewModel
import com.example.ui.PipelineViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.Inter
import com.example.ui.theme.RobotoMono
import com.example.ui.AppNavigation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // V1.1_Updates Section 3: The Tripwire
        lifecycleScope.launch(Dispatchers.IO) {
            if (EnvironmentGuard.isDeviceCompromised()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "SECURITY ALERT: Compromised/Rooted Environment Detected. Shutting down to protect AI Models.", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
            }
        }

        enableEdgeToEdge()

        val factory = PipelineViewModelFactory(application)

        setContent {
            MyApplicationTheme {
                val viewModel: PipelineViewModel = viewModel(factory = factory)
                var bypassOnboarding by remember { mutableStateOf(false) }
                
                // Refresh permissions state every time the app resumes
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            viewModel.checkAllPermissions()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // If any permissions are false or the state map is empty (initializing), show the onboarding screen
                val needsPermissions = viewModel.permissionStates.isEmpty() || viewModel.permissionStates.values.any { !it }
                
                if (needsPermissions && !bypassOnboarding) {
                    com.example.ui.PermissionsOnboardingScreen(
                        onGrantPermissions = {
                            viewModel.checkAllPermissions()
                        },
                        onSkip = { bypassOnboarding = true }
                    )
                } else {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}
