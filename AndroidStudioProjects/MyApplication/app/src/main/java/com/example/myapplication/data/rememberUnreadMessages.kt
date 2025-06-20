package com.example.myapplication.data

import androidx.compose.runtime.*
import com.example.myapplication.data_class.Message
import com.google.firebase.database.*

@Composable
fun rememberUnreadMessages(userUid: String, context: android.content.Context): State<Map<String, Message>> {
    val unreadMessages = remember { mutableStateOf<Map<String, Message>>(emptyMap()) }
    val alertedMessages = remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(userUid) {
        val db = FirebaseDatabase.getInstance().reference
        val readTimestampsRef = db.child("userLastReadMessages").child(userUid)
        val chatsRef = db.child("chats")
        val conversationsRef = db.child("conversations")

        val listener = object : ValueEventListener {
            override fun onDataChange(readSnapshot: DataSnapshot) {
                val readMap = readSnapshot.children.mapNotNull { child ->
                    val key = child.key
                    val value = child.getValue(Long::class.java)
                    if (key != null && value != null) key to value else null
                }.toMap()

                chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(chatsSnapshot: DataSnapshot) {
                        conversationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(convoSnapshot: DataSnapshot) {
                                val unread = mutableMapOf<String, Message>()
                                for (chat in chatsSnapshot.children) {
                                    val convoId = chat.key ?: continue
                                    val messagesSnap = chat.child("messages")
                                    val lastRead = readMap[convoId] ?: 0L
                                    val convo = convoSnapshot.child(convoId)
                                    val participants = convo.child("participants")
                                    if (!participants.hasChild(userUid)) continue
                                    val lastUnreadMessage = messagesSnap.children.mapNotNull {
                                        it.getValue(Message::class.java)
                                    }.lastOrNull { it.timestamp > lastRead }

                                    if (lastUnreadMessage != null) {
                                        unread[convoId] = lastUnreadMessage
                                        if (!alertedMessages.value.contains(convoId)) {
                                            alertedMessages.value += convoId
                                        }
                                    }
                                }
                                unreadMessages.value = unread
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        readTimestampsRef.addValueEventListener(listener)
    }

    return unreadMessages
}
