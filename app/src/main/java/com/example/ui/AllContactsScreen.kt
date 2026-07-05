package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NOTE: the source design uses real contact photos, which I couldn't
 * download (network access to figma.com is blocked in my sandbox). A
 * placeholder avatar circle with a person icon stands in for each contact --
 * swap in AsyncImage/Coil with the actual photo URI in a real app.
 */

private data class ContactEntry(
    val name: String,
    val number: String,
    val expanded: Boolean = false
)

private data class ContactSection(
    val letter: String,
    val contacts: List<ContactEntry>
)

private val contactSections = listOf(
    ContactSection("A", listOf(ContactEntry("Amisha", "+917622365663"))),
    ContactSection(
        "B",
        listOf(
            ContactEntry("Bunty", "+917622365663"),
            ContactEntry("Brijesh Tiwari", "+917622365663", expanded = true)
        )
    ),
    ContactSection(
        "C",
        listOf(
            ContactEntry("Chotulal Chaudhary", "+917622365663"),
            ContactEntry("Chandrika Chautala", "+917622365663"),
            ContactEntry("Chirag Bansal", "+917622365663"),
            ContactEntry("Chirag Bansal", "+917622365663")
        )
    )
)

@Composable
fun AllContactsScreen(
    contactCount: Int = 1930,
    onOpenDialer: () -> Unit = {},
    onAddContact: () -> Unit = {}
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { AppHeader() }
                item { SearchBar(showAdd = true, onAddClick = onAddContact) }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Contacts",
                            color = Gray800,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$contactCount contacts saved",
                            color = Gray500,
                            fontSize = 13.sp
                        )
                    }
                }
                contactSections.forEach { section ->
                    item { LetterHeader(section.letter) }
                    items(section.contacts) { ContactRow(it) }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
            FloatingDialerButton(
                onClick = onOpenDialer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        text = letter,
        color = Gray800,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ContactRow(entry: ContactEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (entry.expanded) it.background(Gray100) else it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderGray)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Blue100)
                    .border(width = 1.dp, color = Blue100, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(entry.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Text(entry.number, fontSize = 13.sp, color = MutedForeground)
            }
        }
        if (entry.expanded) {
            ActionRow(
                primaryLabel = "Call",
                primaryIcon = Icons.Filled.Call,
                onPrimaryClick = {},
                secondaryLabel = "Edit",
                secondaryIcon = Icons.Filled.Edit,
                onSecondaryClick = {},
                onAllLogsClick = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AllContactsScreenPreview() {
    AllContactsScreen()
}
