package com.example.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.common.ShinobiTimeUtil
import com.example.core.notifications.ShinobiNotificationManager
import com.example.data.local.ShinobiDatabase
import com.example.data.repository.ShinobiRepository
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = ShinobiDatabase.getInstance(applicationContext)
        val cachedUid = ShinobiRepository.getCachedLocalUid(applicationContext)
        val uid = cachedUid?.takeIf { db.userProfileDao().getProfileOnce(it) != null }
            ?: db.userProfileDao().getMostRecentProfileOnce()?.uid
            ?: ShinobiRepository.LOCAL_GUEST_UID
        val profile = db.userProfileDao().getProfileOnce(uid) ?: return Result.success()
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
        val streak = profile.currentStreak

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
