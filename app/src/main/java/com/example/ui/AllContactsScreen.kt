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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.provider.ContactsContract
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable

private data class ContactEntry(
    val id: Long,
    val name: String,
    val number: String,
    val expanded: Boolean = false
)

private data class ContactSection(
    val letter: String,
    val contacts: List<ContactEntry>
)

@Composable
fun AllContactsScreen(
    onOpenDialer: () -> Unit = {},
    onAddContact: () -> Unit = {},
    onContactClick: (Long) -> Unit = {},
    onEditContact: (Long) -> Unit = {},
    onViewCallLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    var allContacts by remember { mutableStateOf(emptyList<ContactEntry>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val contacts = mutableListOf<ContactEntry>()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    contacts.add(ContactEntry(id, name, number))
                }
            }
        }
        allContacts = contacts
    }

    val filteredContacts = remember(allContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            allContacts
        } else {
            allContacts.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.number.contains(searchQuery)
            }
        }
    }
    
    val contactSections = remember(filteredContacts) {
        filteredContacts.groupBy { 
            it.name.firstOrNull()?.uppercase() ?: "#" 
        }.map { (letter, contacts) ->
            ContactSection(letter, contacts)
        }.sortedBy { it.letter }
    }
    
    val contactCount = filteredContacts.size
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { AppHeader() }
                item { SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    showAdd = true, 
                    onAddClick = onAddContact
                ) }
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
                if (filteredContacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No contacts found. Add one to get started.", color = Gray500)
                        }
                    }
                } else {
                    contactSections.forEach { section ->
                        item { LetterHeader(section.letter) }
                        items(items = section.contacts, key = { "${it.id}_${it.number}" }) { entry -> 
                            var isExpanded by remember { mutableStateOf(false) }
                            ContactRow(
                                entry = entry.copy(expanded = isExpanded),
                                onClick = { 
                                    isExpanded = !isExpanded
                                    onContactClick(entry.id)
                                },
                                onCall = {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${entry.number}"))
                                    context.startActivity(intent)
                                },
                                onEdit = { onEditContact(entry.id) },
                                onAllLogs = onViewCallLogs
                            )
                        }
                    }
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
private fun ContactRow(
    entry: ContactEntry,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onAllLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                onPrimaryClick = onCall,
                secondaryLabel = "Edit",
                secondaryIcon = Icons.Filled.Edit,
                onSecondaryClick = onEdit,
                onAllLogsClick = onAllLogs,
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
