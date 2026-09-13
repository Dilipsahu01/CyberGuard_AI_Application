package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.models.BlockedNumberEntity
import com.example.models.ScamDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedNumbersScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val dao = remember { ScamDatabase.getDatabase(context).blockedNumberDao() }
    val blockedNumbers by dao.getAll().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var numberToBlock by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Numbers") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Blue100)
            ) {
                Text(
                    text = "Blocked numbers bypass AI pipeline.",
                    modifier = Modifier.padding(16.dp),
                    color = Primary
                )
            }

            if (blockedNumbers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No blocked numbers yet", color = Gray500)
                }
            } else {
                LazyColumn {
                    items(blockedNumbers) { entity ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(entity.phoneNumber, style = MaterialTheme.typography.bodyLarge)
                                    if (entity.contactName != null) {
                                        Text(entity.contactName, style = MaterialTheme.typography.bodyMedium, color = Gray500)
                                    }
                                    Text("Blocked: ${java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(entity.blockedAt))}", style = MaterialTheme.typography.bodySmall, color = Gray500)
                                }
                                TextButton(
                                    onClick = { coroutineScope.launch { dao.unblock(entity) } },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RedEndCall)
                                ) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Blocked Number") },
            text = {
                OutlinedTextField(
                    value = numberToBlock,
                    onValueChange = { numberToBlock = it },
                    label = { Text("Phone Number") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numberToBlock.isNotBlank()) {
                            coroutineScope.launch {
                                dao.block(BlockedNumberEntity(phoneNumber = numberToBlock))
                            }
                            showAddDialog = false
                            numberToBlock = ""
                        }
                    }
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
