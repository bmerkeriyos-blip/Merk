package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.ShinobiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(ShinobiNotificationManager.EXTRA_TASK_ID).orEmpty()
        val title = intent.getStringExtra(ShinobiNotificationManager.EXTRA_TITLE).orEmpty()
        val category = intent.getStringExtra(ShinobiNotificationManager.EXTRA_CATEGORY).orEmpty()
        val notifId = intent.getIntExtra(ShinobiNotificationManager.EXTRA_NOTIF_ID, 1001)

        when (action) {
            ShinobiNotificationManager.ACTION_REMINDER -> {
                if (title.isNotBlank()) {
                    ShinobiNotificationManager.showTaskReminder(
                        context = context,
                        taskId = taskId,
                        title = title,
                        category = category,
                        notifId = notifId
                    )
                }
            }

            ShinobiNotificationManager.ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(notifId)
                if (taskId.isNotBlank()) {
                    ReminderScheduler.scheduleSnooze(
                        context = context,
                        taskId = taskId,
                        title = title.ifBlank { "Scheduled Mission" },
                        category = category.ifBlank { "STUDY" },
                        notifId = notifId,
                        snoozeMinutes = 10
                    )
                }
            }

            ShinobiNotificationManager.ACTION_MARK_DONE -> {
                NotificationManagerCompat.from(context).cancel(notifId)
                if (taskId.isNotBlank()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = ShinobiDatabase.getInstance(context)
                            val task = db.plannerTaskDao().getTaskById(taskId)
                            if (task != null && !task.completed) {
                                db.plannerTaskDao().upsertTask(
                                    task.copy(
                                        completed = true,
                                        updatedAtEpochMs = System.currentTimeMillis(),
                                        synced = false
                                    )
                                )
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
