package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.utils.scheduleReminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reîncarcă alarmele din Firebase pentru utilizatorul curent
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val alarmsRef = FirebaseDatabase.getInstance().reference.child("alarms").child(userId)

            alarmsRef.get().addOnSuccessListener { snapshot ->
                snapshot.children.forEach { alarm ->
                    val alarmId = alarm.key ?: return@forEach
                    val triggerTime = alarm.child("time").getValue(Long::class.java) ?: return@forEach
                    val message = alarm.child("message").getValue(String::class.java) ?: "Reminder"

                    scheduleReminder(
                        context,
                        triggerTime,
                        message,
                        userId,
                        alarmId
                    )
                }
            }
        }
    }
}
