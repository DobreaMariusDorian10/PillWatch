package com.example.myapplication.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.R
import com.example.myapplication.data.rememberUnreadMessages
import com.example.myapplication.data_class.Message
import com.example.myapplication.screens.*
import com.example.myapplication.ui.components.CreateGroupConversationPopup
import com.example.myapplication.utils.firebase.createGroupConversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata

@Composable
fun AppContent(onLogout: () -> Unit) {
    val context = LocalContext.current
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val dbRef = FirebaseDatabase.getInstance().reference
    val storageRef = FirebaseStorage.getInstance().reference

    val currentUserName = remember { mutableStateOf("Utilizator") }
    val profileImageUrl = remember { mutableStateOf<String?>(null) }
    val localImageUri = remember { mutableStateOf<Uri?>(null) }
    val usersMap = remember { mutableStateMapOf<String, String>() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localImageUri.value = it
            val imageRef = storageRef.child("users/$currentUserUid/profile.jpg")
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()

            imageRef.putFile(it, metadata).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    dbRef.child("users").child(currentUserUid).child("profileImageUrl")
                        .setValue(downloadUrl.toString())
                        .addOnSuccessListener {
                            profileImageUrl.value = downloadUrl.toString()
                            localImageUri.value = null
                        }
                        .addOnFailureListener { e -> Log.e("AppContent", "DB update error", e) }
                }
            }.addOnFailureListener { e -> Log.e("AppContent", "Image upload failed", e) }
        }
    }

    LaunchedEffect(currentUserUid) {
        if (currentUserUid.isNotBlank()) {
            dbRef.child("users").child(currentUserUid).get().addOnSuccessListener { snapshot ->
                snapshot.child("name").getValue(String::class.java)?.let { currentUserName.value = it }
                profileImageUrl.value = snapshot.child("profileImageUrl").getValue(String::class.java)
            }

            dbRef.child("users").get().addOnSuccessListener { snapshot ->
                snapshot.children.forEach { userSnap ->
                    val uid = userSnap.key ?: return@forEach
                    val name = userSnap.child("name").getValue(String::class.java) ?: "Utilizator necunoscut"
                    usersMap[uid] = name
                }
            }
        }
    }

    val unreadMessages by rememberUnreadMessages(currentUserUid, context)
    val hasNewMessages by remember { derivedStateOf { unreadMessages.isNotEmpty() } }
    var selectedTab by remember { mutableStateOf(0) }
    var currentConversationId by remember { mutableStateOf<String?>(null) }
    var showUnreadPopup by remember { mutableStateOf(false) }
    var showCreateConversationPopup by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            userName = currentUserName.value,
            profileImageUrl = profileImageUrl.value,
            localImageUri = localImageUri.value,
            onProfileImageClick = { imagePickerLauncher.launch("image/*") },
            hasNewMessages = hasNewMessages,
            onShowUnread = { showUnreadPopup = true },
            onLogout = onLogout
        )

        if (selectedTab == 1 && currentConversationId == null) {
            Button(
                onClick = { showCreateConversationPopup = true },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Creează conversație")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                currentConversationId != null -> MessengerScreen(conversationId = currentConversationId!!)
                selectedTab == 0 -> CalendarScreen()
                selectedTab == 1 -> UsersListScreen(currentUserUid) { currentConversationId = it }
                selectedTab == 2 -> ConversationsListScreen(currentUserUid, usersMap) { currentConversationId = it }
            }
        }

        if (currentConversationId == null) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Calendar", "Users", "Conversations").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                        Text(title)
                    }
                }
            }
        } else {
            Button(
                onClick = { currentConversationId = null },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Înapoi la listă")
            }
        }

        if (showUnreadPopup) {
            UnreadMessagesPopup(
                unreadMessages = unreadMessages,
                usersMap = usersMap,
                onDismiss = { showUnreadPopup = false },
                onConversationSelected = {
                    currentConversationId = it
                    showUnreadPopup = false
                }
            )
        }

        if (showCreateConversationPopup) {
            CreateGroupConversationPopup(
                currentUserUid = currentUserUid,
                usersMap = usersMap,
                onCreate = { selectedUserIds, groupName ->
                    createGroupConversation(currentUserUid, selectedUserIds, groupName) { newId ->
                        currentConversationId = newId
                    }
                    showCreateConversationPopup = false
                },
                onDismiss = { showCreateConversationPopup = false }
            )
        }
    }
}

@Composable
private fun TopBar(
    userName: String,
    profileImageUrl: String?,
    localImageUri: Uri?,
    onProfileImageClick: () -> Unit,
    hasNewMessages: Boolean,
    onShowUnread: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val painter = rememberAsyncImagePainter(localImageUri ?: profileImageUrl ?: R.drawable.ic_default_profile)

            Image(
                painter = painter,
                contentDescription = "Poza profil",
                modifier = Modifier.size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onProfileImageClick() },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))
            Text(userName, color = Color.White, fontSize = 20.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShowUnread) {
                BadgedBox(badge = {
                    if (hasNewMessages) Badge(containerColor = Color.Red, contentColor = Color.White) {}
                }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificări", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun UnreadMessagesPopup(
    unreadMessages: Map<String, Message>,
    usersMap: Map<String, String>,
    onDismiss: () -> Unit,
    onConversationSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mesaje necitite") },
        text = {
            if (unreadMessages.isEmpty()) {
                Text("Nu ai mesaje necitite.")
            } else {
                Column {
                    unreadMessages.forEach { (convoId, message) ->
                        val senderName = usersMap[message.senderId] ?: message.senderId
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onConversationSelected(convoId) }.padding(8.dp)
                        ) {
                            Text("$senderName: ${message.text}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Închide")
            }
        }
    )
}
