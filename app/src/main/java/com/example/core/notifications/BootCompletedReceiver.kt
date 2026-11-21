package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ShinobiDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val uid = Firebase.auth.currentUser?.uid ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = ShinobiDatabase.getInstance(context).plannerTaskDao().getTasksOnce(uid)
                tasks.filter { !it.completed }.forEach { task ->
                    ReminderScheduler.scheduleTaskReminders(context, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
