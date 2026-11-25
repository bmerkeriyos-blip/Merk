package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ShinobiDatabase
import com.example.data.repository.ShinobiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = ShinobiDatabase.getInstance(context)
                val uid = ShinobiRepository(context.applicationContext, db).resolveActiveLocalUid()
                val tasks = db.plannerTaskDao().getTasksOnce(uid)
                tasks.filter { !it.completed }.forEach { task ->
                    ReminderScheduler.scheduleTaskReminders(context, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
