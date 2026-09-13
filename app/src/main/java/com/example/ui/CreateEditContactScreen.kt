package com.example.ui

import android.content.ContentProviderOperation
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditPhone(val number: String, val type: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditContactScreen(
    contactId: Long = -1L,
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isEditMode = contactId > 0
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phones by remember { mutableStateOf(listOf(EditPhone("", ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))) }
    var rawContactId by remember { mutableStateOf(-1L) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> /* Handle photo URI in a real app */ }
    )

    LaunchedEffect(contactId) {
        if (isEditMode) {
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                
                // Get RawContact ID
                val rawCursor = resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )
                rawCursor?.use {
                    if (it.moveToFirst()) {
                        rawContactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                    }
                }

                // Get Name
                val nameCursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                    null
                )
                nameCursor?.use {
                    if (it.moveToFirst()) {
                        firstName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)) ?: ""
                        lastName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)) ?: ""
                    }
                }

                // Get Org
                val orgCursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )
                orgCursor?.use {
                    if (it.moveToFirst()) {
                        company = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)) ?: ""
                    }
                }

                // Get Phones
                val phoneList = mutableListOf<EditPhone>()
                val phoneCursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )
                phoneCursor?.use {
                    while (it.moveToNext()) {
                        val num = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                        val type = it.getInt(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                        phoneList.add(EditPhone(num, type))
                    }
                }
                if (phoneList.isNotEmpty()) {
                    phones = phoneList
                }
            }
        }
    }

    val saveContact = {
        coroutineScope.launch(Dispatchers.IO) {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = 0

            if (!isEditMode) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )

                // Name
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        .build()
                )

                // Company
                if (company.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                            .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                            .build()
                    )
                }

                // Phones
                for (p in phones) {
                    if (p.number.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, p.number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, p.type)
                                .build()
                        )
                    }
                }
            } else {
                // Edit Mode (simplified updates for name and organization - assumes they exist, or would need separate insert/update logic)
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        .build()
                )
                
                if (company.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(selection, arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE))
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                            .build()
                    )
                }
                
                // For phones, typically you delete all old and insert new to be safe in a simple editor
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
                        .build()
                )
                for (p in phones) {
                    if (p.number.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, p.number)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, p.type)
                                .build()
                        )
                    }
                }
            }

            try {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                withContext(Dispatchers.Main) {
                    onSaved()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                IconButton(onClick = { showCancelConfirm = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Gray800)
                }
                Text(
                    text = if (isEditMode) "Edit Contact" else "New Contact",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                IconButton(
                    onClick = { if (firstName.isNotBlank()) saveContact() },
                    enabled = firstName.isNotBlank()
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Save", tint = if (firstName.isNotBlank()) Primary else Gray400)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Add Photo", tint = Primary)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = firstName.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderGray,
                            errorBorderColor = RedEndCall,
                            errorLabelColor = RedEndCall
                        )
                    )
                    if (firstName.isBlank()) {
                        Text(
                            text = "First name is required",
                            color = RedEndCall,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderGray
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderGray
                        )
                    )
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    Text(
                        text = "PHONE NUMBERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gray500,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                itemsIndexed(phones) { index, phone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val typeOptions = listOf(
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE to "Mobile",
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK to "Work",
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME to "Home",
                            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER to "Other"
                        )
                        val selectedTypeName = typeOptions.find { it.first == phone.type }?.second ?: "Mobile"

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(0.4f)
                        ) {
                            OutlinedTextField(
                                value = selectedTypeName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                typeOptions.forEach { (typeVal, typeName) ->
                                    DropdownMenuItem(
                                        text = { Text(typeName) },
                                        onClick = {
                                            val newList = phones.toMutableList()
                                            newList[index] = phone.copy(type = typeVal)
                                            phones = newList
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = phone.number,
                            onValueChange = { newNum ->
                                val newList = phones.toMutableList()
                                newList[index] = phone.copy(number = newNum)
                                phones = newList
                            },
                            label = { Text("Number") },
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderGray
                            )
                        )

                        if (phones.size > 1) {
                            IconButton(onClick = {
                                val newList = phones.toMutableList()
                                newList.removeAt(index)
                                phones = newList
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Gray500)
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            val newList = phones.toMutableList()
                            newList.add(EditPhone("", ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))
                            phones = newList
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add phone number", color = Primary)
                    }
                    Spacer(Modifier.height(32.dp))
                }

                if (isEditMode) {
                    item {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Contact", color = RedEndCall)
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete this contact?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    coroutineScope.launch(Dispatchers.IO) {
                        context.contentResolver.delete(
                            ContactsContract.RawContacts.CONTENT_URI,
                            "${ContactsContract.RawContacts._ID} = ?",
                            arrayOf(rawContactId.toString())
                        )
                        withContext(Dispatchers.Main) {
                            onSaved()
                        }
                    }
                }) {
                    Text("Delete", color = RedEndCall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Gray800)
                }
            }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    onBackClick()
                }) {
                    Text("Discard", color = RedEndCall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Cancel", color = Gray800)
                }
            }
        )
    }
}
