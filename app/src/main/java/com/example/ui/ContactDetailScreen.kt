package com.example.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactDetail(
    val name: String = "",
    val organization: String? = null,
    val isStarred: Boolean = false,
    val phones: List<ContactPhone> = emptyList()
)

data class ContactPhone(
    val number: String,
    val typeLabel: String
)

@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBackClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var contactDetail by remember { mutableStateOf(ContactDetail()) }

    LaunchedEffect(contactId) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            
            // Get Name and Starred Status
            var name = "Unknown"
            var isStarred = false
            val contactCursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.STARRED),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            contactCursor?.use {
                if (it.moveToFirst()) {
                    name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Unknown"
                    isStarred = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
                }
            }

            // Get Organization
            var organization: String? = null
            val orgCursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                null
            )
            orgCursor?.use {
                if (it.moveToFirst()) {
                    organization = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
                }
            }

            // Get Phones
            val phones = mutableListOf<ContactPhone>()
            val phoneCursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
                ),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            phoneCursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                    val label = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, label).toString()
                    phones.add(ContactPhone(number, typeLabel))
                }
            }

            contactDetail = ContactDetail(name, organization, isStarred, phones)
        }
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Gray800)
                }
                Text("Contact", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Gray800)
                IconButton(onClick = { onEditClick(contactId) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Primary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Blue100)
                        .border(width = 2.dp, color = Blue100, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Name & Org
                Text(
                    text = contactDetail.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                if (!contactDetail.organization.isNullOrEmpty()) {
                    Text(
                        text = contactDetail.organization!!,
                        fontSize = 16.sp,
                        color = Gray500
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = BorderGray)
                
                // Phones Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PHONE NUMBERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(contactDetail.phones) { phone ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = phone.typeLabel, fontSize = 12.sp, color = Gray500)
                                    Text(text = phone.number, fontSize = 16.sp, color = Gray800)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${phone.number}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(GreenCall.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = GreenCall)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("sms:${phone.number}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Primary.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Text", tint = Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            HorizontalDivider(color = BorderGray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { onEditClick(contactId) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Contact: ${contactDetail.name}\n" +
                                    contactDetail.phones.joinToString("\n") { "${it.typeLabel}: ${it.number}" })
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
                OutlinedButton(
                    onClick = {
                        val newStarred = !contactDetail.isStarred
                        val values = ContentValues().apply {
                            put(ContactsContract.Contacts.STARRED, if (newStarred) 1 else 0)
                        }
                        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                        context.contentResolver.update(uri, values, null, null)
                        contactDetail = contactDetail.copy(isStarred = newStarred)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (contactDetail.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (contactDetail.isStarred) Color(0xFFEAB308) else Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Favorite")
                }
            }
        }
    }
}
