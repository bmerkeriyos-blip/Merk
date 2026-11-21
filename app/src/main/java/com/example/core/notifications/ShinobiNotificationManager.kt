package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ShinobiNotificationManager {
    const val CHANNEL_CLASSES = "shinobi_classes"
    const val CHANNEL_ASSIGNMENTS = "shinobi_assignments"
    const val CHANNEL_EXAMS = "shinobi_exams"
    const val CHANNEL_FOCUS = "shinobi_focus"
    const val CHANNEL_BRIEFING = "shinobi_briefing"

    const val ACTION_REMINDER = "com.aistudio.shinobiplanner.ACTION_REMINDER"
    const val ACTION_SNOOZE = "com.aistudio.shinobiplanner.ACTION_SNOOZE"
    const val ACTION_MARK_DONE = "com.aistudio.shinobiplanner.ACTION_MARK_DONE"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(CHANNEL_CLASSES, "Class Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Upcoming university lectures and labs"
            },
            NotificationChannel(CHANNEL_ASSIGNMENTS, "Mission & Assignment Deadlines", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Study sessions, tasks, and assignment reminders"
            },
            NotificationChannel(CHANNEL_EXAMS, "Exam Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Critical examination countdowns and alerts"
                enableVibration(true)
            },
            NotificationChannel(CHANNEL_FOCUS, "Focus Dojo Timer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pomodoro and focus session completion notifications"
            },
            NotificationChannel(CHANNEL_BRIEFING, "Daily Ninja Briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Morning briefing, evening review, and streak-at-risk alerts"
            }
        )
        nm.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showTaskReminder(
        context: Context,
        taskId: String,
        title: String,
        category: String,
        notifId: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val channelId = when (category.uppercase()) {
            "CLASS" -> CHANNEL_CLASSES
            "EXAM", "DEADLINE" -> CHANNEL_EXAMS
            else -> CHANNEL_ASSIGNMENTS
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context, notifId + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val donePending = PendingIntent.getBroadcast(
            context, notifId + 20000, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Mission Alert: $title")
            .setContentText("Category: $category — Tap to open Shinobi HUD")
            .setPriority(
                if (category.equals("EXAM", true)) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Snooze 10m", snoozePending)
            .addAction(0, "Mark Done", donePending)

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    fun showSimpleNotification(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }
}
