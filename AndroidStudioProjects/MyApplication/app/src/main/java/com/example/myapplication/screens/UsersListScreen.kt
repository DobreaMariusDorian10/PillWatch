package com.example.myapplication.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.utils.firebase.addFriend
import com.example.myapplication.data_class.User
import com.example.myapplication.utils.firebase.startConversation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
@Composable
fun UsersListScreen(
    currentUid: String,
    onStartChat: (conversationId: String) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference.child("users")
    val users = remember { mutableStateListOf<User>() }
    val friends = remember { mutableStateListOf<String>() }

    DisposableEffect(database) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    val uid = userSnapshot.key ?: ""
                    user?.let {
                        users.add(it.copy(uid = uid))
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(listener)
        onDispose { database.removeEventListener(listener) }
    }

    DisposableEffect(currentUid) {
        val friendsDb = FirebaseDatabase.getInstance().reference
            .child("users").child(currentUid).child("friends")
        val friendListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                friends.clear()
                for (friendSnapshot in snapshot.children) {
                    friends.add(friendSnapshot.key ?: "")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        friendsDb.addValueEventListener(friendListener)
        onDispose { friendsDb.removeEventListener(friendListener) }
    }

    LazyColumn {
        items(users) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${user.name} (${user.email})")

                when {
                    user.uid == currentUid -> {
                        Text("This is you", color = Color.Gray)
                    }
                    friends.contains(user.uid) -> {
                        Button(onClick = {
                            startConversation(currentUid, user.uid) { conversationId ->
                                onStartChat(conversationId)
                            }
                        }) {
                            Text("Chat")
                        }
                    }
                    else -> {
                        Button(onClick = {
                            addFriend(currentUid, user.uid)
                        }) {
                            Text("Add Friend")
                        }
                    }
                }
            }
        }
    }
}

