    package com.example.myapplication.utils

    import android.app.*
    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.os.Build
    import android.provider.Settings
    import android.widget.Toast
    import androidx.core.app.AlarmManagerCompat
    import com.example.myapplication.AlarmReceiver

    fun checkExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:" + context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun scheduleReminder(
        context: Context,
        triggerAtMillis: Long,
        message: String,
        targetUserId: String,
        alarmId: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", message)
            putExtra("alarmId", alarmId)
            putExtra("targetUserId", targetUserId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + targetUserId).hashCode(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelSystemAlarm(context: Context, alarmId: String, targetUserId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + targetUserId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
