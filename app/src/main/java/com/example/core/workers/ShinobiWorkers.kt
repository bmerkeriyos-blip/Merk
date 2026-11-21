package com.example.core.workers

import android.content.Context
import androidx.work.*
import com.example.core.common.ShinobiTimeUtil
import com.example.core.notifications.ShinobiNotificationManager
import com.example.data.local.ShinobiDatabase
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.ShinobiRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val db = ShinobiDatabase.getInstance(applicationContext)
        val settings = db.userSettingsDao().getSettingsOnce(uid)
        if (settings?.offlineModeOnly == true) return Result.success()

        val repo = ShinobiRepository(
            applicationContext,
            db,
            FirestoreSyncRepository(applicationContext)
        )
        return repo.syncPendingToCloud(uid).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val WORK_NAME = "shinobi_cloud_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = PeriodicWorkRequestBuilder<CloudSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}

class DailyBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = ShinobiDatabase.getInstance(applicationContext)
        val uid = Firebase.auth.currentUser?.uid ?: ShinobiRepository.LOCAL_GUEST_UID
        if (db.userProfileDao().getProfileOnce(uid) == null) return Result.success()
        val settings = db.userSettingsDao().getSettingsOnce(uid)
        val tz = settings?.timezoneId ?: ShinobiTimeUtil.DEFAULT_TIMEZONE_ID
        val now = ShinobiTimeUtil.nowZoned(tz)
        if (settings?.quietHoursEnabled == true) {
            val h = now.hour
            val s = settings.quietStartHour
            val e = settings.quietEndHour
            val inQuiet = if (s > e) (h >= s || h < e) else (h in s until e)
            if (inQuiet) return Result.success()
        }

        val todayIso = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tasks = db.plannerTaskDao().getTasksOnce(uid)
        val todayOpen = tasks.count { it.dateIso == todayIso && !it.completed }
        val profile = db.userProfileDao().getProfileOnce(uid)
        val streak = profile?.currentStreak ?: 0

        val body = when {
            todayOpen > 0 && streak > 0 ->
                "You have $todayOpen active missions today. Keep your $streak-day chakra streak alive!"
            todayOpen > 0 ->
                "You have $todayOpen scheduled missions today. Open Shinobi Dojo to begin."
            else ->
                "Your schedule is clear today. Launch a Focus Dojo session to gain XP!"
        }
        ShinobiNotificationManager.showSimpleNotification(
            context = applicationContext,
            channelId = ShinobiNotificationManager.CHANNEL_BRIEFING,
            notifId = 4001,
            title = "Shinobi Daily Briefing",
            body = body
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "shinobi_daily_briefing"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailyBriefingWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
