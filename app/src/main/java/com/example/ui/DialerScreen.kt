package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---- Mock Data & T9 Mapping Logic ----
private data class T9Contact(val name: String, val number: String)

private val mockT9Contacts = listOf(
    T9Contact("Dad", "9876543211"),
    T9Contact("Mom", "9876543210"),
    T9Contact("Amisha", "7622365663"),
    T9Contact("Bunty", "7622365664"),
    T9Contact("Brijesh Tiwari", "7622365665")
)

private suspend fun getLocalContacts(context: Context): List<T9Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<T9Contact>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val rawNum = it.getString(numIndex).replace(Regex("[^0-9+]"), "")
                contacts.add(T9Contact(it.getString(nameIndex), rawNum))
            }
        }
    } catch (e: Exception) {
        // Permission might not be granted; fall back to mock or empty
    }
    contacts
}

private fun getT9String(name: String): String {
    return name.uppercase().mapNotNull { char ->
        when (char) {
            in 'A'..'C' -> "2"
            in 'D'..'F' -> "3"
            in 'G'..'I' -> "4"
            in 'J'..'L' -> "5"
            in 'M'..'O' -> "6"
            in 'P'..'S' -> "7"
            in 'T'..'V' -> "8"
            in 'W'..'Z' -> "9"
            else -> null
        }
    }.joinToString("")
}

private val keypadRows = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    onCallClick: (String) -> Unit = {}
) {
    var dialedNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    var contactsList by remember { mutableStateOf(mockT9Contacts) }

    LaunchedEffect(Unit) {
        val local = getLocalContacts(context)
        if (local.isNotEmpty()) {
            contactsList = local
        }
    }

    val currentTime = remember {
        SimpleDateFormat("hh:mm a 'IST', dd MMM, EEEE", Locale.getDefault()).format(Date())
    }

    var showSimSelector by remember { mutableStateOf(false) }
    var setDefaultSim by remember { mutableStateOf(false) }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    val sharedPrefs = remember { context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE) }

    val availableSims = remember {
        try {
            telecomManager.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList<PhoneAccountHandle>()
        }
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- MANDATE 1: Top Section (Header, Input, Contacts, Location) ----
            Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CYBERGUARD-AI",
                        color = TitleBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu", tint = Gray800)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset Default SIM") },
                                onClick = {
                                    sharedPrefs.edit().remove("default_sim_id").apply()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }

                // ---- FIXED: Anti-Jank Contact Space (Always 90.dp) ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp) // Reserved vertical space to prevent layout shift
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (dialedNumber.isNotEmpty()) {
                        val matchedContacts = remember(dialedNumber, contactsList) {
                            contactsList.filter { contact ->
                                getT9String(contact.name).contains(dialedNumber) ||
                                contact.number.contains(dialedNumber)
                            }
                        }

                        if (matchedContacts.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(matchedContacts) { contact ->
                                    SuggestedContactChip(
                                        contact = contact,
                                        onClick = { dialedNumber = contact.number }
                                    )
                                }
                            }
                        }
                    }
                }

                // Number Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(width = 1.dp, color = BorderGray)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedNumber = PhoneNumberUtils.formatNumber(dialedNumber, Locale.getDefault().country) ?: dialedNumber
                    Text(
                        text = if (dialedNumber.isNotEmpty()) formattedNumber else " ",
                        color = Gray800,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Change country code",
                        tint = Gray800,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info Strip (Location Hook)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(BlueBg)
                        .border(width = 1.dp, color = Primary)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\uD83C\uDDEE\uD83C\uDDF3", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Jaipur, Rajasthan • $currentTime",
                        color = Gray700,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- MANDATE 1: Middle Section (Dialpad Grid with weight(1f)) ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // STRETCH TO FILL AVAILABLE SPACE
                    .padding(horizontal = 16.dp)
            ) {
                keypadRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { (digit, letters) ->
                            KeypadCell(
                                digit = digit,
                                letters = letters,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { dialedNumber += digit },
                                onLongClick = when (digit) {
                                    "0" -> { { dialedNumber += "+" } }
                                    else -> null
                                }
                            )
                        }
                    }
                }
            }

            // ---- MANDATE 1: Bottom Section (Call Button anchored at bottom) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(48.dp))

                // Massive Call Button (FloatingActionButton)
                FloatingActionButton(
                    onClick = {
                        if (dialedNumber.isNotEmpty()) {
                            val defaultSimId = sharedPrefs.getString("default_sim_id", null)
                            if (defaultSimId != null) {
                                onCallClick("+91$dialedNumber")
                            } else if (availableSims.size > 1) {
                                showSimSelector = true
                            } else {
                                onCallClick("+91$dialedNumber")
                            }
                        }
                    },
                    containerColor = GreenCall,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Backspace Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(if (dialedNumber.isNotEmpty()) {
                            Modifier.combinedClickable(
                                onClick = { dialedNumber = dialedNumber.dropLast(1) },
                                onLongClick = { dialedNumber = "" }
                            )
                        } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (dialedNumber.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Gray600,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    // SIM Selection Bottom Sheet
    if (showSimSelector) {
        ModalBottomSheet(onDismissRequest = { showSimSelector = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select SIM for this call", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))

                availableSims.forEach { sim ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (setDefaultSim) {
                                    sharedPrefs.edit().putString("default_sim_id", sim.id).apply()
                                }
                                showSimSelector = false
                                onCallClick("+91$dialedNumber")
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sim.id, fontSize = 16.sp, color = Gray800)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).clickable { setDefaultSim = !setDefaultSim }
                ) {
                    Checkbox(checked = setDefaultSim, onCheckedChange = { setDefaultSim = it })
                    Text("Set as Default SIM", fontSize = 14.sp, color = Gray600, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SuggestedContactChip(contact: T9Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Gray50)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Blue100),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = contact.name, color = Gray800, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = "+91 ${contact.number}", color = Gray500, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadCell(
    digit: String,
    letters: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- MANDATE 2: Auto-Scaling Keypad (Balanced font size) ----
            Text(
                text = digit,
                color = Gray800,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = Gray500,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light
                )
            } else {
                // Keep digit centered relative to those with letters
                Text(
                    text = " ",
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DialerScreenPreview() {
    MaterialTheme {
        DialerScreen()
    }
}
