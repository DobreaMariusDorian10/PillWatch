package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateGroupConversationPopup(
    currentUserUid: String,
    usersMap: Map<String, String>,
    onCreate: (List<String>, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var groupName by remember { mutableStateOf("") }
    val otherUsers = usersMap.filterKeys { it != currentUserUid }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Creează conversație grup") },
        text = {
            Column {
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nume grup") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (otherUsers.isEmpty()) {
                    Text("Nu există alți utilizatori disponibili.")
                } else {
                    Column(modifier = Modifier.height(200.dp)) {
                        LazyColumn {
                            items(otherUsers.entries.toList()) { (uid, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUserIds = if (selectedUserIds.contains(uid)) {
                                                selectedUserIds - uid
                                            } else {
                                                selectedUserIds + uid
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedUserIds.contains(uid),
                                        onCheckedChange = {
                                            selectedUserIds = if (it) {
                                                selectedUserIds + uid
                                            } else {
                                                selectedUserIds - uid
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(selectedUserIds.toList(), groupName.trim()) },
                enabled = selectedUserIds.isNotEmpty() && groupName.isNotBlank()
            ) {
                Text("Creează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează")
            }
        }
    )
}
