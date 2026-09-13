package com.example.ui

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class FavoriteContact(val id: Long, val name: String, val number: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(onOpenDialer: () -> Unit = {}, onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf<List<FavoriteContact>>(emptyList()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            favorites = getFavoriteContacts(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingDialerButton(onClick = onOpenDialer)
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No favorites yet. Star contacts to add them here.", color = Gray500)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favorites) { contact ->
                    var showMenu by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_CALL).apply {
                                        data = Uri.parse("tel:${contact.number}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: SecurityException) {
                                        e.printStackTrace()
                                    }
                                },
                                onLongClick = { showMenu = true }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Blue100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Remove from favorites") },
                                onClick = {
                                    showMenu = false
                                    removeFavorite(context, contact.id)
                                    favorites = getFavoriteContacts(context)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFavoriteContacts(context: android.content.Context): List<FavoriteContact> {
    val list = mutableListOf<FavoriteContact>()
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val selection = "${ContactsContract.Contacts.STARRED} = 1"
    
    try {
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: "Unknown"
                val number = cursor.getString(numIdx) ?: ""
                // Add unique contacts by ID (since a contact can have multiple phones, simple dedup logic here or distinct in query)
                if (list.none { it.id == id }) {
                    list.add(FavoriteContact(id, name, number))
                }
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
    return list
}

private fun removeFavorite(context: android.content.Context, contactId: Long) {
    val values = ContentValues().apply {
        put(ContactsContract.Contacts.STARRED, 0)
    }
    val selection = "${ContactsContract.Contacts._ID} = ?"
    val selectionArgs = arrayOf(contactId.toString())
    context.contentResolver.update(ContactsContract.Contacts.CONTENT_URI, values, selection, selectionArgs)
}
