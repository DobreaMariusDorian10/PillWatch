package com.example.myapplication.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data_class.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessengerScreen(conversationId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val senderId = currentUser.uid

    val dbRef = FirebaseDatabase.getInstance().reference
        .child("chats").child(conversationId).child("messages")

    val messages = remember { mutableStateListOf<Message>() }
    var messageText by remember { mutableStateOf("") }

    var lastMessageTimestamp by remember { mutableStateOf(0L) }

    val usersMap = remember { mutableStateMapOf<String, String>() }

    // Load users once
    LaunchedEffect(Unit) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    val name = userSnap.child("name").getValue(String::class.java) ?: "Utilizator necunoscut"
                    usersMap[uid] = name
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Listen to messages and clean up listener properly
    DisposableEffect(conversationId) {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.let {
                    // Add only if not already present (avoid duplicates)
                    if (messages.none { m -> m.timestamp == it.timestamp && m.senderId == it.senderId && m.text == it.text }) {
                        messages.add(it)
                    }
                    if (it.timestamp > lastMessageTimestamp) {
                        lastMessageTimestamp = it.timestamp
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addChildEventListener(listener)

        onDispose {
            dbRef.removeEventListener(listener)
            messages.clear()
            lastMessageTimestamp = 0L
        }
    }

    // Mark conversation as read when last message changes
    LaunchedEffect(lastMessageTimestamp) {
        if (lastMessageTimestamp > 0L) {
            markConversationAsRead(senderId, conversationId, lastMessageTimestamp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            // Sort messages by timestamp ascending and reverse to show newest at bottom
            val sortedMessages = messages.sortedBy { it.timestamp }
            items(sortedMessages.reversed()) { message ->
                val isOwn = message.senderId == senderId
                val senderName = usersMap[message.senderId] ?: message.senderId

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isOwn) Color(0xFFDCF8C6) else Color.LightGray,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = senderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTime(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text("Scrie un mesaj...") },
                maxLines = 4,
                singleLine = false,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = messageText.trim()
                    if (text.isNotEmpty()) {
                        val msg = Message(
                            senderId = senderId,
                            text = text,
                            timestamp = System.currentTimeMillis()
                        )
                        val newMsgRef = dbRef.push()
                        newMsgRef.setValue(msg).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                messageText = ""
                            } else {
                                Log.e("MessengerScreen", "Eroare salvare mesaj: ${task.exception?.message}")
                            }
                        }
                    }
                }
            ) {
                Text("Trimite")
            }
        }
    }
}

fun markConversationAsRead(userUid: String, conversationId: String, lastTimestamp: Long) {
    val db = FirebaseDatabase.getInstance().reference
    db.child("userLastReadMessages")
        .child(userUid)
        .child(conversationId)
        .setValue(lastTimestamp)
}

