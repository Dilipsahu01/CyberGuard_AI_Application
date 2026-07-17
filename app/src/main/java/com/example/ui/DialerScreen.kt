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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
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

// Row layout: digit to (letters, row)
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

    // SIM Selection State
    var showSimSelector by remember { mutableStateOf(false) }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    val availableSims = remember {
        try {
            telecomManager.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList<PhoneAccountHandle>()
        }
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ---- App header (Logo + menu) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp), // Increased horizontal safe-area padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CYBERGUARD-AI",
                    color = TitleBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(48.dp) // Minimum touch target size
                        .clickable { /* Menu action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = Gray800,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ---- Content pushed to bottom matching standard dialers ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ---- Smart T9 Predictive Contacts ----
                if (dialedNumber.isNotEmpty()) {
                    val matchedContacts = remember(dialedNumber, contactsList) {
                        contactsList.filter { contact ->
                            getT9String(contact.name).contains(dialedNumber) ||
                            contact.number.contains(dialedNumber)
                        }
                    }

                    if (matchedContacts.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                // ---- Number display row ("+91 ...") ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = BorderGray)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedNumber = PhoneNumberUtils.formatNumber(dialedNumber, Locale.getDefault().country) ?: dialedNumber
                    Text(
                        text = if (dialedNumber.isNotEmpty()) formattedNumber else "",
                        color = Gray800,
                        fontSize = 24.sp,
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

                // ---- Info strip (flag + local date/time) ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BlueBg)
                        .border(width = 1.dp, color = Primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\uD83C\uDDEE\uD83C\uDDF3", fontSize = 16.sp) // India flag emoji
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentTime,
                        color = Gray700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ---- Keypad ----
                keypadRows.forEachIndexed { rowIndex, row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (digit, letters) ->
                            KeypadCell(
                                digit = digit,
                                letters = letters,
                                showBottomBorder = rowIndex != keypadRows.lastIndex,
                                modifier = Modifier.weight(1f),
                                onClick = { dialedNumber += digit },
                                onLongClick = when (digit) {
                                    "0" -> { { dialedNumber += "+" } }
                                    "1" -> { { /* Dial Voicemail logic */ } }
                                    else -> null
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ---- Call button row ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(48.dp)) // Empty space for alignment
                    
                    // Call Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GreenCall)
                            .border(width = 1.dp, color = EmeraldBorder, shape = CircleShape)
                            .clickable { 
                                if (availableSims.size > 1) {
                                    showSimSelector = true
                                } else {
                                    onCallClick("+91$dialedNumber") 
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Backspace Button
                    if (dialedNumber.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp) // Minimum 48dp touch target
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = { dialedNumber = dialedNumber.dropLast(1) },
                                    onLongClick = { dialedNumber = "" }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = Gray600,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp)) // Keep row layout balanced
                    }
                }
            }
        }
    }

    // SIM Selection Bottom Sheet Dialog
    if (showSimSelector) {
        ModalBottomSheet(onDismissRequest = { showSimSelector = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select SIM for this call", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))
                availableSims.forEach { sim ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSimSelector = false
                                onCallClick("+91$dialedNumber") // In a real app we pass 'sim' object to the intent
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sim.id, fontSize = 16.sp, color = Gray800)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SuggestedContactChip(contact: T9Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Gray50) // From Theme.kt
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
            Text(
                text = contact.name, 
                color = Gray800, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "+91 ${contact.number}", 
                color = Gray500, 
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadCell(
    digit: String,
    letters: String,
    showBottomBorder: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(
                if (showBottomBorder)
                    Modifier.border(width = 1.dp, color = BorderGray)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .defaultMinSize(minHeight = 64.dp) // Ensure minimum touch target size
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = digit,
            color = Gray800,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (letters.isNotEmpty()) {
            Text(
                text = letters,
                color = Gray500,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
