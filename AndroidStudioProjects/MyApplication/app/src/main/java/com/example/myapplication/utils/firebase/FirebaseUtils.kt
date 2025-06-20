package com.example.myapplication.utils.firebase

import android.content.Context
import android.widget.Toast
import com.example.myapplication.data_class.Conversation
import com.example.myapplication.data_class.Message
import com.example.myapplication.utils.scheduleReminder
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

fun addFriend(currentUid: String, otherUid: String) {
    val db = FirebaseDatabase.getInstance().reference
    db.child("users").child(currentUid).child("friends").child(otherUid).setValue(true)
    db.child("users").child(otherUid).child("friends").child(currentUid).setValue(true)
}

fun startConversation(
    uid1: String,
    uid2: String,
    onResult: (String) -> Unit
) {
    val db = FirebaseDatabase.getInstance().reference

    db.child("conversations").get().addOnSuccessListener { snapshot ->
        for (conversation in snapshot.children) {
            val participants = conversation.child("participants").children.mapNotNull { it.key }
            if (participants.containsAll(listOf(uid1, uid2)) && participants.size == 2) {
                onResult(conversation.key!!)
                return@addOnSuccessListener
            }
        }

        val newConversation = db.child("conversations").push()
        val data = mapOf(
            "participants/$uid1" to true,
            "participants/$uid2" to true
        )
        newConversation.updateChildren(data).addOnSuccessListener {
            onResult(newConversation.key!!)
        }
    }
}
fun createGroupConversation(
    currentUserUid: String,
    selectedUserIds: List<String>,
    groupName: String,
    onComplete: ((String) -> Unit)? = null
) {
    val db = FirebaseDatabase.getInstance().reference
    val conversationsRef = db.child("conversations")
    val chatsRef = db.child("chats")

    val participants = (selectedUserIds + currentUserUid).associateWith { true }

    val newConvoRef = conversationsRef.push()
    val convoId = newConvoRef.key ?: return

    val newConversation = Conversation(
        participants = participants,
        name = groupName
    )

    newConvoRef.setValue(newConversation).addOnSuccessListener {
        chatsRef.child(convoId).setValue(mapOf("messages" to emptyMap<String, Message>()))
        onComplete?.invoke(convoId)
    }
}


fun deleteAlarmFromFirebase(context: Context, userId: String, alarmId: String, onSuccess: () -> Unit) {
    FirebaseDatabase.getInstance().reference
        .child("alarms").child(userId).child(alarmId)
        .removeValue()
        .addOnSuccessListener {
            Toast.makeText(context, "Alarmă ștearsă cu succes", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Eroare la ștergerea alarmei", Toast.LENGTH_SHORT).show()
        }
}