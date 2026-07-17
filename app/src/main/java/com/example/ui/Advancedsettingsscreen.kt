package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NOTE: Several icons here (RecordVoiceOver, Psychology, Groups, Sync, Tune,
 * Storage, Brightness4) come from the `material-icons-extended` artifact
 * (androidx.compose.material:material-icons-extended). Add that dependency,
 * or swap for core-set equivalents if you'd rather not pull it in.
 *
 * Colors are pulled entirely from the shared Theme.kt tokens -- every hex in
 * the spec already matched an existing token 1:1 (Primary, Gray50, Gray800,
 * Gray500, BorderGray), so nothing new was added there.
 *
 * Default toggle states not specified in the brief (Deepfake Protection,
 * Intent NLP Analysis, Dark Theme) are assumed ON for the two AI-detection
 * toggles (this is a security-first app) and OFF for Dark Theme (system
 * default light). Adjust as needed.
 */

@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyberguard_settings", android.content.Context.MODE_PRIVATE) }
    
    var threatSensitivity by rememberSaveable { mutableFloatStateOf(70f) }
    var deepfakeProtection by rememberSaveable { mutableStateOf(true) }
    var intentNlpAnalysis by rememberSaveable { mutableStateOf(true) }
    var guardianNumber by rememberSaveable { mutableStateOf(prefs.getString("guardian_number", "") ?: "") }
    var swarmIntelligence by rememberSaveable { mutableStateOf(true) }
    var syncModeIndex by rememberSaveable { mutableIntStateOf(0) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(guardianNumber) {
        prefs.edit().putString("guardian_number", guardianNumber).apply()
    }

    Surface(color = Gray50, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onBackClick)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SettingsSection(title = "AI Detection Engine") {
                        SettingsSliderRow(
                            icon = Icons.Filled.Tune,
                            title = "Threat Sensitivity Level",
                            subtitle = "Adjust the AI ensemble score threshold for flagging calls.",
                            value = threatSensitivity,
                            onValueChange = { threatSensitivity = it }
                        )
                        SectionDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.RecordVoiceOver,
                            title = "Deepfake Voice Protection",
                            subtitle = "Detect synthetic or cloned AI voices in real-time.",
                            checked = deepfakeProtection,
                            onCheckedChange = { deepfakeProtection = it }
                        )
                        SectionDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.Psychology,
                            title = "Intent NLP Analysis",
                            subtitle = "Analyze financial coercion and urgency intents.",
                            checked = intentNlpAnalysis,
                            onCheckedChange = { intentNlpAnalysis = it }
                        )
                    }
                }

                item {
                    SettingsSection(title = "Guardian Protection") {
                        SettingsTextFieldRow(
                            icon = Icons.Filled.HealthAndSafety,
                            title = "Emergency Guardian Number",
                            subtitle = "Automatically text this number if a high-risk scam is detected.",
                            value = guardianNumber,
                            onValueChange = { guardianNumber = it },
                            placeholder = "e.g. +1 555-0100"
                        )
                    }
                }

                item {
                    SettingsSection(title = "System Configuration") {
                        SettingsLinkRow(
                            icon = Icons.Filled.Security,
                            title = "App Permissions & Roles",
                            subtitle = "Manage Microphone, Default Dialer, and Call Screening roles required for the AI to function.",
                            linkText = "Open OS Settings",
                            onLinkClick = {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                item {
                    SettingsSection(title = "Privacy & Telemetry") {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Groups,
                            title = "Swarm Threat Intelligence",
                            subtitle = "Share anonymous 82-bit hashes to crowd-source scam data. No PII is shared.",
                            checked = swarmIntelligence,
                            onCheckedChange = { swarmIntelligence = it }
                        )
                        SectionDivider()
                        SettingsSegmentedRow(
                            icon = Icons.Filled.Sync,
                            title = "Offline-First Sync",
                            subtitle = "Choose when the local model and threat data sync.",
                            options = listOf("Wi-Fi Only", "5G/Cellular"),
                            selectedIndex = syncModeIndex,
                            onSelect = { syncModeIndex = it }
                        )
                    }
                }

                item {
                    SettingsSection(title = "Local Resources") {
                        SettingsLinkRow(
                            icon = Icons.Filled.Storage,
                            title = "On-Device Models",
                            subtitle = "Silero VAD & MiniLM-L6 Loaded",
                            linkText = "Check for Updates",
                            onLinkClick = onCheckForUpdates
                        )
                    }
                }

                item {
                    SettingsSection(title = "Appearance") {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Brightness4,
                            title = "Dark Theme",
                            subtitle = "Force dark mode regardless of system setting.",
                            checked = darkTheme,
                            onCheckedChange = { darkTheme = it }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun SettingsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Gray800,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBackClick)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "Advanced Settings",
            color = Gray800,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    SectionDivider()
}

// ---------------------------------------------------------------------------
// Section container: uppercase header + rounded card
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            color = Gray500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = BorderGray, thickness = 1.dp)
}

// ---------------------------------------------------------------------------
// Row types
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Gray500, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Gray50,
                uncheckedBorderColor = BorderGray
            )
        )
    }
}

@Composable
private fun SettingsTextFieldRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Gray500, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Gray500, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderGray,
                cursorColor = Primary
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Gray500, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = value.toInt().toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = BorderGray
            ),
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}

@Composable
private fun SettingsSegmentedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Gray500, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Primary,
                        activeContentColor = Color.White,
                        activeBorderColor = Primary,
                        inactiveContainerColor = Color.White,
                        inactiveContentColor = Gray700,
                        inactiveBorderColor = BorderGray
                    )
                ) {
                    Text(label, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    linkText: String,
    onLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Gray500)
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onLinkClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(linkText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedSettingsScreenPreview() {
    AdvancedSettingsScreen()
}
