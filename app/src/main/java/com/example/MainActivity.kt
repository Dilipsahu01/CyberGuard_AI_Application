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
        enableEdgeToEdge()
        val factory = PipelineViewModelFactory(application)

        setContent {
            MyApplicationTheme {
                val viewModel: PipelineViewModel = viewModel(factory = factory)
                var isCompromised by remember { mutableStateOf(false) }
                var appReady by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                     withContext(Dispatchers.IO) {
                         isCompromised = EnvironmentGuard.isDeviceCompromised()
                     }
                }
                
                if (isCompromised) {
                    Surface(color = Color(0xFFFEE2E2), modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("SYSTEM COMPROMISED", color = Color(0xFFEF4444), fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text("The application detected unusual behaviour or malware in the device. We can't load the CyberGuard AI.", color = Color(0xFFEF4444), fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (!appReady) {
                    com.example.ui.SplashScreen(onAppReady = { appReady = true })
                } else {
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
                    
                    if (needsPermissions) {
                        com.example.ui.PermissionsOnboardingScreen(
                            onGrantPermissions = {
                                viewModel.checkAllPermissions()
                            }
                        )
                    } else {
                        val initialRoute = if (intent.hasExtra("EXTRA_SCAM_NUMBER")) {
                            val num = intent.getStringExtra("EXTRA_SCAM_NUMBER") ?: ""
                            "post_call_review/${android.net.Uri.encode(num)}"
                        } else {
                            "dashboard"
                        }
                        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                            AppNavigation(viewModel, startDestination = initialRoute)
                        }
                    }
                }
            }
        }
    }
}
