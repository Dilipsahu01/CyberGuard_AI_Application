package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdvancedSettingsScreen(
    viewModel: PipelineViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onNavigateToBlockedNumbers: () -> Unit = {},
    onNavigateToQuickResponses: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyberguard_settings", android.content.Context.MODE_PRIVATE) }

    var threatSensitivity by rememberSaveable { mutableFloatStateOf(prefs.getInt("alert_threshold", 70).toFloat()) }
    var deepfakeProtection by rememberSaveable { mutableStateOf(prefs.getBoolean("deepfake_protection", true)) }
    var intentNlpAnalysis by rememberSaveable { mutableStateOf(prefs.getBoolean("intent_nlp", true)) }
    var guardianNumber by rememberSaveable { mutableStateOf(prefs.getString("guardian_number", "") ?: "") }
    var swarmIntelligence by rememberSaveable { mutableStateOf(prefs.getBoolean("swarm_intelligence", true)) }
    var syncModeIndex by rememberSaveable { mutableIntStateOf(prefs.getInt("sync_mode_index", 0)) }
    var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
    var dialpadTones by rememberSaveable { mutableStateOf(prefs.getBoolean("dialpad_tones", true)) }
    var vibrateOnCall by rememberSaveable { mutableStateOf(prefs.getBoolean("vibrate_on_call", true)) }

    LaunchedEffect(guardianNumber) {
        prefs.edit().putString("guardian_number", guardianNumber).apply()
    }
    LaunchedEffect(threatSensitivity) { prefs.edit().putInt("alert_threshold", threatSensitivity.toInt()).apply() }
    LaunchedEffect(deepfakeProtection) { prefs.edit().putBoolean("deepfake_protection", deepfakeProtection).apply() }
    LaunchedEffect(intentNlpAnalysis) { prefs.edit().putBoolean("intent_nlp", intentNlpAnalysis).apply() }
    LaunchedEffect(swarmIntelligence) { prefs.edit().putBoolean("swarm_intelligence", swarmIntelligence).apply() }
    LaunchedEffect(syncModeIndex) { prefs.edit().putInt("sync_mode_index", syncModeIndex).apply() }
    LaunchedEffect(darkTheme) { prefs.edit().putBoolean("dark_theme", darkTheme).apply() }
    
    LaunchedEffect(dialpadTones) {
        prefs.edit().putBoolean("dialpad_tones", dialpadTones).apply()
    }
    
    LaunchedEffect(vibrateOnCall) {
        prefs.edit().putBoolean("vibrate_on_call", vibrateOnCall).apply()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onBackClick)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
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
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsSwitchRow(
                            icon = Icons.Filled.RecordVoiceOver,
                            title = "Deepfake Voice Protection",
                            subtitle = "Detect synthetic or cloned AI voices in real-time.",
                            checked = deepfakeProtection,
                            onCheckedChange = { deepfakeProtection = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
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
                            subtitle = "Automatically text this number if a scam is detected.",
                            value = guardianNumber,
                            onValueChange = { guardianNumber = it },
                            placeholder = "e.g. +91 9876543210",
                            onSaveClick = {
                                android.widget.Toast.makeText(context, "Guardian number saved!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                
                item {
                    SettingsSection(title = "Call Management") {
                        SettingsLinkRow(
                            icon = Icons.Filled.Block,
                            title = "Blocked Numbers",
                            subtitle = "Manage numbers blocked from calling you.",
                            linkText = "Manage",
                            onLinkClick = onNavigateToBlockedNumbers
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsLinkRow(
                            icon = Icons.Filled.Edit,
                            title = "Quick Responses",
                            subtitle = "Edit SMS templates for declining calls.",
                            linkText = "Edit",
                            onLinkClick = onNavigateToQuickResponses
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsLinkRow(
                            icon = Icons.Filled.VerifiedUser,
                            title = "Trusted Contacts",
                            subtitle = "Contacts that bypass AI scanning.",
                            linkText = "Manage",
                            onLinkClick = onNavigateToWhitelist
                        )
                    }
                }
                
                item {
                    SettingsSection(title = "Sound & Display") {
                        SettingsSwitchRow(
                            icon = Icons.Filled.TouchApp,
                            title = "Dialpad Tones",
                            subtitle = "Play tones when using dialpad.",
                            checked = dialpadTones,
                            onCheckedChange = { dialpadTones = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsSwitchRow(
                            icon = Icons.Filled.Vibration,
                            title = "Vibrate on Call",
                            subtitle = "Vibrate device for incoming calls.",
                            checked = vibrateOnCall,
                            onCheckedChange = { vibrateOnCall = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsLinkRow(
                            icon = Icons.Filled.MusicNote,
                            title = "Ringtone",
                            subtitle = "Change default ringtone.",
                            linkText = "Change",
                            onLinkClick = {
                                val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                item {
                    SettingsSection(title = "System Configuration") {
                        SettingsLinkRow(
                            icon = Icons.Filled.Security,
                            title = "App Permissions & Roles",
                            subtitle = "Manage Microphone and Dialer roles required for AI.",
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
                            subtitle = "Share anonymous hashes to crowd-source scam data.",
                            checked = swarmIntelligence,
                            onCheckedChange = { swarmIntelligence = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsSegmentedRow(
                            icon = Icons.Filled.Sync,
                            title = "Offline-First Sync",
                            subtitle = "Choose when the local model and threat data sync.",
                            options = listOf("Wi-Fi", "Cellular"),
                            selectedIndex = syncModeIndex,
                            onSelect = { syncModeIndex = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray)
                        SettingsLinkRow(
                            icon = Icons.Filled.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "Read our data collection and processing policies.",
                            linkText = "View",
                            onLinkClick = onNavigateToPrivacyPolicy
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

                item {
                    SettingsSection(title = "Data Management (DPDP Act)") {
                        SettingsActionRow(
                            icon = Icons.Filled.DeleteForever,
                            title = "Purge All My Data",
                            subtitle = "Permanently delete all call logs, memory, and telemetry associations.",
                            actionText = "Purge Everything",
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                viewModel.purgeAllData {
                                    // Navigate back or show success
                                    android.widget.Toast.makeText(context, "All data successfully erased.", android.widget.Toast.LENGTH_LONG).show()
                                    onBackClick()
                                }
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBackClick: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Gray800,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Advanced Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Gray800,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Gray500,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Gray800)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Primary)
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
    placeholder: String = "",
    onSaveClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Gray800)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Gray500) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
            )
            if (onSaveClick != null) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Save")
                }
            }
        }
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
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Gray800)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSegmentedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Gray800)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = Primary, activeContentColor = Color.White)
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Gray800)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500)
        }
        TextButton(onClick = onLinkClick) {
            Text(linkText, style = MaterialTheme.typography.labelLarge, color = Primary)
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    contentColor: Color = Primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray500)
        }
        TextButton(onClick = onClick) {
            Text(actionText, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdvancedSettingsScreenPreview() {
    AdvancedSettingsScreen()
}
