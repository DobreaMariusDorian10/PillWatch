package com.example.myapplication.screens

import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.cancelSystemAlarm
import com.example.myapplication.utils.scheduleReminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class AlarmActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var alarmId: String? = null
    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Wake the screen and show over lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val message = intent.getStringExtra("message") ?: "Reminder!"
        alarmId = intent.getStringExtra("alarmId")
        targetUserId = intent.getStringExtra("targetUserId")

        // Check if current user is the alarm target, if not close activity
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != targetUserId) {
            finish()
            return
        }

        findViewById<TextView>(R.id.tvAlarmMessage).text = message

        // Play alarm sound in loop
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
            isLooping = true
            start()
        }

        // Dismiss button stops alarm and closes activity
        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        // Delay button: postpone alarm by 5 minutes without deleting it
        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            alarmId?.let { id ->
                targetUserId?.let { userId ->
                    // Cancel existing system alarm
                    cancelSystemAlarm(this, id, userId)

                    // New alarm time: current time + 5 minutes
                    val newTriggerTime = System.currentTimeMillis() + 5 * 60 * 1000L

                    // Reference to this alarm in Firebase
                    val db = FirebaseDatabase.getInstance().reference
                        .child("alarms")
                        .child(userId)
                        .child(id)

                    // Update timestamp in Firebase
                    db.child("timestamp").setValue(newTriggerTime)
                        .addOnSuccessListener {
                            // Retrieve the message for scheduling the reminder
                            db.child("message").get().addOnSuccessListener { messageSnapshot ->
                                val updatedMessage = messageSnapshot.getValue(String::class.java) ?: "Reminder!"
                                // Reschedule alarm
                                scheduleReminder(this, newTriggerTime, updatedMessage, userId, id)
                                Toast.makeText(this, "Alarmă amânată cu 5 minute", Toast.LENGTH_SHORT).show()
                            }
                            stopAlarm()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Eroare la amânare alarmă", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun stopAlarm() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        super.onDestroy()
    }
}
fun createRepeatingAlarms(
    context: android.content.Context,
    targetUserId: String,
    startTimestamp: Long,
    intervalHours: Int,
    repetitions: Int,
    label: String,
    onComplete: (() -> Unit)? = null
) {
    val intervalMillis = intervalHours * 60 * 60 * 1000L
    val db = FirebaseDatabase.getInstance().reference.child("alarms").child(targetUserId)

    for (i in 0 until repetitions) {
        val triggerTime = startTimestamp + i * intervalMillis
        val alarmId = UUID.randomUUID().toString()

        val alarmData = mapOf(
            "alarmId" to alarmId,
            "timestamp" to triggerTime,
            "message" to "$label (doza ${i + 1})",
            "assignedTo" to targetUserId,
            "createdBy" to FirebaseAuth.getInstance().currentUser?.uid,
            "active" to true
        )

        db.child(alarmId).setValue(alarmData)
            .addOnSuccessListener {
                scheduleReminder(context, triggerTime, "$label (doza ${i + 1})", targetUserId, alarmId)
                if (i == repetitions - 1) {
                    onComplete?.invoke()
                }
            }
    }
}