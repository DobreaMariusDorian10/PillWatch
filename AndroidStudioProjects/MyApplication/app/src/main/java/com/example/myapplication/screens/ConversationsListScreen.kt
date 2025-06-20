package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data_class.Conversation
import com.google.firebase.database.*

@Composable
fun ConversationsListScreen(
    currentUserUid: String,
    usersMap: Map<String, String>,
    onConversationSelected: (String) -> Unit
) {
    val conversations = remember { mutableStateListOf<Conversation>() }
    val conversationIds = remember { mutableStateListOf<String>() }
    val db = FirebaseDatabase.getInstance().reference.child("conversations")

    DisposableEffect(currentUserUid) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                conversations.clear()
                conversationIds.clear()
                for (convoSnap in snapshot.children) {
                    val convo = convoSnap.getValue(Conversation::class.java) ?: continue
                    if (convo.participants.containsKey(currentUserUid)) {
                        conversations.add(convo)
                        conversationIds.add(convoSnap.key ?: "")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener)
        onDispose { db.removeEventListener(listener) }
    }

    fun getConversationDisplayName(
        conversation: Conversation,
        currentUserUid: String,
        usersMap: Map<String, String>
    ): String {
        conversation.name?.let {
            if (it.isNotBlank()) return it
        }

        val otherUsers = conversation.participants.keys.filter { it != currentUserUid }
        val names = otherUsers.map { usersMap[it] ?: it }
        return when {
            names.isEmpty() -> "Conversație cu tine"
            names.size == 1 -> names[0]
            else -> "Grup (${names.size}): ${names.joinToString(", ")}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(conversations) { index, convo ->
            val convoId = conversationIds.getOrNull(index) ?: ""
            val displayName = getConversationDisplayName(convo, currentUserUid, usersMap)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConversationSelected(convoId) },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
