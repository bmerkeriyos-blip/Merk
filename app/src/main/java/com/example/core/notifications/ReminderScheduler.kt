package com.example.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.PlannerTaskEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

object ReminderScheduler {

    fun scheduleTaskReminders(context: Context, task: PlannerTaskEntity) {
        if (task.completed) {
            cancelTaskReminders(context, task.id)
            return
        }
        val offsets = task.reminderOffsetsCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()

        val date = runCatching { LocalDate.parse(task.dateIso) }.getOrNull() ?: return
        val startHour = (task.startTimeMinutes / 60).coerceIn(0, 23)
        val startMin = (task.startTimeMinutes % 60).coerceIn(0, 59)
        val zone = ShinobiTimeUtil.safeZoneId(ShinobiTimeUtil.DEFAULT_TIMEZONE_ID)
        val eventZdt = ZonedDateTime.of(date, LocalTime.of(startHour, startMin), zone)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        offsets.forEachIndexed { idx, offsetMins ->
            val triggerMillis = eventZdt.minusMinutes(offsetMins.toLong()).toInstant().toEpochMilli()
            if (triggerMillis <= System.currentTimeMillis()) return@forEachIndexed

            val reqCode = requestCodeFor(task.id, idx)
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ShinobiNotificationManager.ACTION_REMINDER
                putExtra(ShinobiNotificationManager.EXTRA_TASK_ID, task.id)
                putExtra(ShinobiNotificationManager.EXTRA_TITLE, task.title)
                putExtra(ShinobiNotificationManager.EXTRA_CATEGORY, task.category)
                putExtra(ShinobiNotificationManager.EXTRA_NOTIF_ID, reqCode)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarmSafely(alarmManager, triggerMillis, pending)
        }
    }

    fun scheduleSnooze(
        context: Context,
        taskId: String,
        title: String,
        category: String,
        notifId: Int,
        snoozeMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ShinobiNotificationManager.ACTION_REMINDER
            putExtra(ShinobiNotificationManager.EXTRA_TASK_ID, taskId)
            putExtra(ShinobiNotificationManager.EXTRA_TITLE, title)
            putExtra(ShinobiNotificationManager.EXTRA_CATEGORY, category)
            putExtra(ShinobiNotificationManager.EXTRA_NOTIF_ID, notifId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmSafely(alarmManager, triggerMillis, pending)
    }

    private fun scheduleAlarmSafely(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pending: PendingIntent
    ) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        }
    }

    fun cancelTaskReminders(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (idx in 0..5) {
            val reqCode = requestCodeFor(taskId, idx)
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ShinobiNotificationManager.ACTION_REMINDER
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun requestCodeFor(taskId: String, index: Int): Int =
        (taskId.hashCode().absoluteValue % 100_000) * 10 + index
}
