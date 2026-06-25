package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SettingsScreen(
                    onBack = { finish() },
                    context = this,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, context: Context) {
    val prefs = remember { context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE) }
    
    var alertThreshold by remember { mutableFloatStateOf(prefs.getInt("alert_threshold", 70).toFloat()) }
    var autoHangup by remember { mutableStateOf(prefs.getBoolean("auto_hangup", true)) }
    var swarmServerUrl by remember { mutableStateOf(prefs.getString("swarm_server_url", "https://api.cyberguard-ai.com/telemetry") ?: "https://api.cyberguard-ai.com/telemetry") }
    var enableSmsFallback by remember { mutableStateOf(prefs.getBoolean("enable_sms_fallback", false)) }
    var smsFallbackNumber by remember { mutableStateOf(prefs.getString("sms_fallback_number", "") ?: "") }
    var whitelistInput by remember { mutableStateOf("") }
    
    var whitelistList by remember { 
        mutableStateOf(
            prefs.getStringSet("whitelist", setOf("+919440112233", "+919491122334"))?.toList() ?: emptyList(),
        ) 
    }

    val defaultRegexes = remember {
        listOf(
            "\\botp\\b" to "Demanding 6-Digit Banking OTP or PIN codes.",
            "digital.?arrest" to "Synthesized CBI/Police dynamic digital arrests.",
            "\\banydesk\\b|\\bteamviewer\\b" to "Asks for screen-capturing AnyDesk controls.",
            "account.{0,20}(freeze|block)" to "Financial account hold scare statements.",
            "police.{0,20}coming" to "Law enforcement dispatch psychological threats.",
            "(wire|transfer).{0,20}(money|funds)" to "Demands for government clearance fund routing."
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "AI PIPELINE CONFIG", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color(0xFF00F0FF),
                        fontFamily = FontFamily.Monospace
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0A0A0F))
            )
        },
        containerColor = Color(0xFF0A0A0F)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF08080C), Color(0xFF040411))
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sensitivity Card
            item {
                Text(
                    text = "RISK ACCURACY SCALERS",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200F0FF))
                        .border(1.dp, Color(0xFF1B2B3A), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI Alert Sensitivity", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${alertThreshold.toInt()}%", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        onValueChangeFinished = {
                            prefs.edit {
                    putInt("alert_threshold", alertThreshold.toInt())
                }
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00F0FF),
                            thumbColor = Color(0xFF00F0FF)
                        )
                    )
                    Text(
                        text = "Flags warning screen overlays instantly when confidence exceeds ${alertThreshold.toInt()}% during call added transactions.",
                        color = Color(0xFF888899),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Automated Defenses
            item {
                Text(
                    text = "AUTOMATED OS PROTOCOLS",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200F0FF))
                        .border(1.dp, Color(0xFF1B2B3A), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Disconnect High Threat Calls", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Terminated telephone trunk connection inside standard InCall SDK once confidence ticks past $alertThreshold%.",
                            color = Color(0xFF888899),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = autoHangup,
                        onCheckedChange = {
                            autoHangup = it
                            prefs.edit().putBoolean("auto_hangup", autoHangup).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00F0FF),
                            checkedTrackColor = Color(0x3F00F0FF)
                        )
                    )
                }
            }

            // Swarm Telemetry Config
            item {
                Text(
                    text = "5G SWARM RE-ROUTING GATEWAY",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200F0FF))
                        .border(1.dp, Color(0xFF1B2B3A), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text("Swarm Server URL", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = swarmServerUrl,
                        onValueChange = { 
                            swarmServerUrl = it
                            prefs.edit().putString("swarm_server_url", it).apply()
                        },
                        placeholder = { Text("https://cyberguard.requestcatcher.com/telemetry") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF222233)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real physical phones must use the host computer's active IP or a domain (e.g. http://192.168.1.100:5000/telemetry) to upload packet slices successfully.",
                        color = Color(0xFF888899),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // SMS Fallback Config
            item {
                Text(
                    text = "TELEMETRY SMS FALLBACK CODES",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200F0FF))
                        .border(1.dp, Color(0xFF1B2B3A), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable SMS Fallback", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = enableSmsFallback,
                            onCheckedChange = {
                                enableSmsFallback = it
                                prefs.edit().putBoolean("enable_sms_fallback", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00F0FF),
                                checkedTrackColor = Color(0x3F00F0FF)
                            )
                        )
                    }
                    if (enableSmsFallback) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("SMS Fallback Receiver Number", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = smsFallbackNumber,
                            onValueChange = { 
                                smsFallbackNumber = it
                                prefs.edit().putString("sms_fallback_number", it).apply()
                            },
                            placeholder = { Text("E.g. +919999999999") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF222233)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Encodes 10.25-byte 5G telemetry into hex and sends silent SMS report when mobile internet is offline.",
                        color = Color(0xFF888899),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Whitelist Config
            item {
                Text(
                    text = "SAFE NUMBER BYPASS LIST",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0A16))
                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = whitelistInput,
                            onValueChange = { whitelistInput = it },
                            placeholder = { Text("E.g. +91 944...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF222233)
                            )
                        )
                        Button(
                            onClick = {
                                if (whitelistInput.isNotBlank()) {
                                    val updated = whitelistList + whitelistInput.trim()
                                    whitelistList = updated
                                    prefs.edit().putStringSet("whitelist", updated.toSet()).apply()
                                    whitelistInput = ""
                                    Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (whitelistList.isEmpty()) {
                        Text("No whitelisted numbers added.", color = Color(0xFF555566), fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            whitelistList.forEach { num ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF131322))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(num, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                val updated = whitelistList.filter { it != num }
                                                whitelistList = updated
                                                prefs.edit().putStringSet("whitelist", updated.toSet()).apply()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keyword RegEx Inspect Card
            item {
                Text(
                    text = "ACTIVE TELECOM ANTI-FRAUD REGULAR EXPRESSIONS",
                    color = Color(0xFF666688),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultRegexes.forEach { (rule, exp) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF12121A))
                                .border(1.dp, Color(0x33666688), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Code icon",
                                    tint = Color(0x7300F0FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = rule, 
                                    color = Color(0xFF00F0FF), 
                                    fontSize = 13.sp, 
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = exp, color = Color(0xFF888899), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
