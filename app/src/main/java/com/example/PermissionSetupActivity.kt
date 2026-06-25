package com.example

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class PermissionSetupActivity : ComponentActivity() {

    private val requestDialerRole = 101
    private val requestScreenerRole = 102

    private var permissionCheckTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val trigger = permissionCheckTrigger // observing the trigger
                PermissionWizardScreen(
                    onExit = {
                        Toast.makeText(this, "Setup completed successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onRequestPermission = { handlePermissionRequest(it) },
                    checkPermission = { isPermissionGranted(it) },
                    checkTrigger = trigger
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-checks statuses reactively and forces Compose to recompose
        permissionCheckTrigger++
    }

    private fun isPermissionGranted(key: String): Boolean {
        return when (key) {
            "mic" -> ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            "phone" -> ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            "overlay" -> Settings.canDrawOverlays(this)
            "dialer" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                    roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                } else {
                    val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
                    telecomManager.defaultDialerPackage == packageName
                }
            }
            "screener" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                } else true
            }
            "notif" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
            }
            else -> false
        }
    }

    private fun handlePermissionRequest(key: String) {
        when (key) {
            "mic" -> {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            }
            "phone" -> {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.READ_CALL_LOG,
                        android.Manifest.permission.ANSWER_PHONE_CALLS
                    ), 
                    2
                )
            }
            "overlay" -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            "dialer" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                        !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, requestDialerRole)
                    } else {
                        Toast.makeText(this, "Dialer role already approved!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    }
                    startActivity(intent)
                }
            }
            "screener" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                        !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, requestScreenerRole)
                    } else {
                        Toast.makeText(this, "Call screening role already approved!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Screening automatically verified below Android 10.", Toast.LENGTH_SHORT).show()
                }
            }
            "notif" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 3)
                } else {
                    Toast.makeText(this, "Permission implicitly active.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

data class PermissionStep(
    val key: String,
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val isRequired: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PermissionWizardScreen(
    onExit: () -> Unit,
    onRequestPermission: (String) -> Unit,
    checkPermission: (String) -> Boolean,
    checkTrigger: Int
) {
    val steps = remember {
        listOf(
            PermissionStep("mic", "Microphone Access", "Enables real-time acoustic pipeline checking for scam speech indicators on-device.", Icons.Default.Info, isRequired = true),
            PermissionStep("phone", "Telephone & Log Access", "Required to retrieve caller profiles during incoming scam ringing events.", Icons.Default.Phone, isRequired = true),
            PermissionStep("overlay", "System Warning Window", "Allows neon border alert boxes to layer on top of scam transactions immediately.", Icons.Default.Warning, isRequired = true),
            PermissionStep("dialer", "Default Dialer App", "Required deep OS integration to capture call added signals and act as incoming UI.", Icons.Default.Star, isRequired = true),
            PermissionStep("screener", "Default Call Screener", "Intercepts suspected robocalls before your device rings.", Icons.Default.Lock, false),
            PermissionStep("notif", "Push Notifications", "Displays the persistent 5G Active security status and live scam confidence percentage HUD.", Icons.Default.Notifications, false)
        )
    }

    var currentStepIdx by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIdx]
    
    // Check if current step is granted
    var isCurrentGranted by remember(currentStepIdx, checkTrigger) { 
        mutableStateOf(checkPermission(currentStep.key)) 
    }

    // Auto-advance if granted
    LaunchedEffect(isCurrentGranted) {
        if (isCurrentGranted) {
            delay(800) // brief delay so user sees the success state
            if (currentStepIdx < steps.size - 1) {
                currentStepIdx++
            } else {
                onExit()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617)) // Premium deep slate/blue gradient
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CYBERGUARD AI",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "System Setup Wizard",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Indicators Dot row
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        val isGranted = checkPermission(step.key)
                        Box(
                            modifier = Modifier
                                .size(width = if (idx == currentStepIdx) 24.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isGranted -> Color(0xFF10B981) // Green if granted
                                        idx == currentStepIdx -> Color(0xFF38BDF8) // Blue if active
                                        else -> Color(0x33FFFFFF)
                                    }
                                )
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(tween(400))).togetherWith(
                        slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut(tween(400))
                    )
                }, modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "stepAnimation"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (isCurrentGranted) Color(0x2210B981) else Color(0x1138BDF8))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentGranted) Icons.Default.CheckCircle else step.icon,
                            contentDescription = step.title,
                            tint = if (isCurrentGranted) Color(0xFF10B981) else Color(0xFF38BDF8),
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Text(
                        text = if (isCurrentGranted) "Permission Granted!" else step.title,
                        color = if (isCurrentGranted) Color(0xFF10B981) else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 28.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = step.desc,
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom Navigation CTAs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onRequestPermission(currentStep.key) },
                    enabled = !isCurrentGranted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentGranted) Color(0xFF10B981) else Color(0xFF38BDF8),
                        disabledContainerColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isCurrentGranted) "GRANTED" else "GRANT PERMISSION",
                        color = Color(0xFF020617),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep.isRequired) {
                        Text(
                            text = "* Required system privilege",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                if (currentStepIdx < steps.size - 1) {
                                    currentStepIdx++
                                } else {
                                    onExit()
                                }
                            }
                        ) {
                            Text(text = "Skip Optional", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStepIdx < (steps.size - 1)) {
                                currentStepIdx++
                            } else {
                                onExit()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A38BDF8)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (currentStepIdx == steps.size - 1) "FINISH" else "NEXT",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
