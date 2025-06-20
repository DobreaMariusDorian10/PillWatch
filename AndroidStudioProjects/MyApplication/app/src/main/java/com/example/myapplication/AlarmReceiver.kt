package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.screens.AlarmActivity
import com.google.firebase.auth.FirebaseAuth

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Reminder"
        val alarmId = intent.getStringExtra("alarmId") ?: return
        val targetUserId = intent.getStringExtra("targetUserId") ?: return

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != targetUserId) {
            // Nu e pentru userul actual – ignoră
            return
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("message", message)
            putExtra("alarmId", alarmId)
            putExtra("targetUserId", targetUserId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(alarmIntent)
    }


}

