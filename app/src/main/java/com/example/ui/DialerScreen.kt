package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ---- Mock Data & T9 Mapping Logic ----
private data class T9Contact(val name: String, val number: String)

private val mockT9Contacts = listOf(
    T9Contact("Dad", "9876543211"),
    T9Contact("Mom", "9876543210"),
    T9Contact("Amisha", "7622365663"),
    T9Contact("Bunty", "7622365664"),
    T9Contact("Brijesh Tiwari", "7622365665")
)

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

@Composable
fun DialerScreen(
    onCallClick: (String) -> Unit = {}
) {
    var dialedNumber by remember { mutableStateOf("") }
    val currentTime = remember {
        SimpleDateFormat("hh:mm a 'IST', dd MMM, EEEE", Locale.getDefault()).format(Date())
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ---- App header (Logo + menu) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CYBERGUARD-AI",
                    color = TitleBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = Gray800,
                    modifier = Modifier.size(24.dp)
                )
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
                    val matchedContacts = remember(dialedNumber) {
                        mockT9Contacts.filter { contact ->
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
                    Text(
                        text = "+91 $dialedNumber".trimEnd(),
                        color = Gray800,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
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
                                onClick = { dialedNumber += digit }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ---- Call button ----
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GreenCall)
                        .border(width = 1.dp, color = EmeraldBorder, shape = CircleShape)
                        .clickable { onCallClick("+91$dialedNumber") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
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

@Composable
private fun KeypadCell(
    digit: String,
    letters: String,
    showBottomBorder: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .then(
                if (showBottomBorder)
                    Modifier.border(width = 1.dp, color = BorderGray)
                else Modifier
            )
            .clickable(onClick = onClick)
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
