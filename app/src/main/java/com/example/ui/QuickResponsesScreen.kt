package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickResponsesScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cyberguard_settings", Context.MODE_PRIVATE) }
    
    var responses by remember { 
        mutableStateOf(loadResponses(sharedPrefs)) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Responses") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                responses = responses + ""
                saveResponses(sharedPrefs, responses)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "SMS templates for declining incoming calls.",
                color = Gray500,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                itemsIndexed(responses) { index, response ->
                    var isEditing by remember { mutableStateOf(response.isEmpty()) }
                    var editedText by remember { mutableStateOf(response) }
                    
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                    ) {
                        if (isEditing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editedText,
                                    onValueChange = { editedText = it },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val newList = responses.toMutableList()
                                    newList[index] = editedText
                                    responses = newList
                                    saveResponses(sharedPrefs, responses)
                                    isEditing = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                                IconButton(onClick = {
                                    if (response.isEmpty()) {
                                        val newList = responses.toMutableList()
                                        newList.removeAt(index)
                                        responses = newList
                                        saveResponses(sharedPrefs, responses)
                                    } else {
                                        isEditing = false
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = response,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { isEditing = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    val newList = responses.toMutableList()
                                    newList.removeAt(index)
                                    responses = newList
                                    saveResponses(sharedPrefs, responses)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadResponses(sharedPrefs: android.content.SharedPreferences): List<String> {
    val defaultSet = setOf(
        "Can't talk right now. What's up?",
        "I'll call you right back.",
        "I'm in a meeting.",
        "Sorry, I'm busy. Call you later."
    )
    val stringSet = sharedPrefs.getStringSet("quick_responses", null)
    if (stringSet == null) {
        saveResponses(sharedPrefs, defaultSet.toList())
        return defaultSet.toList()
    }
    return stringSet.toList()
}

private fun saveResponses(sharedPrefs: android.content.SharedPreferences, list: List<String>) {
    val responsesSet = list.toSet()
    sharedPrefs.edit().putStringSet("quick_responses", responsesSet).apply()
}
