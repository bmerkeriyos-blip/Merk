package com.example.data.repository

import android.content.Context
import com.example.core.common.RecurrenceRule
import com.example.core.common.RankProgression
import com.example.core.common.ShinobiTimeUtil
import com.example.core.notifications.ReminderScheduler
import com.example.data.local.AchievementEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.MusicTrackEntity
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.ShinobiDatabase
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.SubtaskEntity
import com.example.data.local.TaskCompletionHistoryEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class TimeSlot(val startMinutes: Int, val endMinutes: Int)

data class TaskConflict(
    val first: PlannerTaskEntity,
    val second: PlannerTaskEntity
)

class ShinobiRepository(
    private val context: Context,
    private val db: ShinobiDatabase
) {
    companion object {
        const val LOCAL_GUEST_UID = "local_shinobi"
        const val LOCAL_PREFS_NAME = "shinobi_local_prefs"
        const val KEY_ACTIVE_UID = "active_uid"

        fun getCachedLocalUid(context: Context): String? =
            context.applicationContext
                .getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_UID, null)

        fun setCachedLocalUid(context: Context, uid: String) {
            context.applicationContext
                .getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_UID, uid)
                .apply()
        }
    }

    private val profileDao = db.userProfileDao()
    private val subjectDao = db.subjectDao()
    private val taskDao = db.plannerTaskDao()
    private val subtaskDao = db.subtaskDao()
    private val sessionDao = db.studySessionDao()
    private val historyDao = db.historyAndAchievementDao()
    private val chatDao = db.chatDao()
    private val mediaDao = db.mediaAndWallpaperDao()
    private val notesDao = db.notesAndScansDao()
    private val settingsDao = db.userSettingsDao()

    fun observeProfile(uid: String): Flow<UserProfileEntity?> = profileDao.observeProfile(uid)
    fun observeSubjects(uid: String): Flow<List<SubjectEntity>> = subjectDao.observeSubjects(uid)
    fun observeTasks(uid: String): Flow<List<PlannerTaskEntity>> = taskDao.observeTasks(uid)
    fun observeAllSubtasks(uid: String): Flow<List<SubtaskEntity>> = subtaskDao.observeAllSubtasksForUser(uid)
    fun observeSessions(uid: String): Flow<List<StudySessionEntity>> = sessionDao.observeSessions(uid)
    fun observeHistory(uid: String): Flow<List<TaskCompletionHistoryEntity>> = historyDao.observeHistory(uid)
    fun observeAchievements(uid: String): Flow<List<AchievementEntity>> = historyDao.observeAchievements(uid)
    fun observeWallpapers(uid: String): Flow<List<WallpaperItemEntity>> = mediaDao.observeWallpapers(uid)
    fun observeTracks(uid: String): Flow<List<MusicTrackEntity>> = mediaDao.observeTracks(uid)
    fun observeNotes(uid: String): Flow<List<StudyNoteEntity>> = notesDao.observeNotes(uid)
    fun observeSettings(uid: String): Flow<UserSettingsEntity?> = settingsDao.observeSettings(uid)

    /**
     * Resolves the active local profile UID, safely migrating any previously signed-in
     * local Room profile into offline-only continuity without losing tasks, XP, or history.
     */
    suspend fun resolveActiveLocalUid(): String {
        val cached = getCachedLocalUid(context)
        if (!cached.isNullOrBlank() && profileDao.getProfileOnce(cached) != null) {
            return cached
        }
        val mostRecent = profileDao.getMostRecentProfileOnce()
        val resolved = mostRecent?.uid ?: LOCAL_GUEST_UID
        setCachedLocalUid(context, resolved)
        return resolved
    }

    suspend fun ensureUserInitialized(uid: String = LOCAL_GUEST_UID) {
        setCachedLocalUid(context, uid)

        val existingProfile = profileDao.getProfileOnce(uid)
        if (existingProfile == null) {
            profileDao.upsertProfile(
                UserProfileEntity(
                    uid = uid,
                    displayName = "Shinobi Scholar",
                    username = "local_shinobi",
                    email = "",
                    photoUrl = "",
                    academicYear = "",
                    xp = 0,
                    level = 1,
                    ninjaTitle = RankProgression.titleForLevel(1),
                    onboardingCompleted = false
                )
            )
        }

        val existingSettings = settingsDao.getSettingsOnce(uid)
        if (existingSettings == null) {
            settingsDao.upsertSettings(
                UserSettingsEntity(
                    userId = uid,
                    themePreset = "NARUTO_CHAKRA",
                    aiEnabled = false,
                    floatingAiEnabled = false,
                    offlineModeOnly = true,
                    dashboardWidgetsCsv = "PROGRESS,NEXT_CLASS,TIMER,MISSIONS,WEEKLY,QUICK_ACTIONS"
                )
            )
        } else if (existingSettings.aiEnabled || existingSettings.floatingAiEnabled || !existingSettings.offlineModeOnly) {
            settingsDao.upsertSettings(
                existingSettings.copy(
                    aiEnabled = false,
                    floatingAiEnabled = false,
                    offlineModeOnly = true,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }

        if (historyDao.getAchievementsOnce(uid).isEmpty()) {
            historyDao.upsertAchievements(defaultAchievements(uid))
        }

        if (mediaDao.observeWallpapers(uid).first().isEmpty()) {
            mediaDao.upsertWallpaper(
                WallpaperItemEntity(
                    id = "builtin_village_$uid",
                    userId = uid,
                    screenTarget = "GLOBAL",
                    uriString = "builtin://img_naruto_uzumaki",
                    builtInResName = "img_naruto_uzumaki",
                    brightness = 0.72f,
                    blurRadius = 2f,
                    overlayOpacity = 0.65f,
                    parallaxEnabled = true
                )
            )
        }
    }

    private fun defaultAchievements(uid: String): List<AchievementEntity> = listOf(
        AchievementEntity("ach_first_$uid", uid, "First Scroll", "Complete your first study mission", "FIRST_SCROLL", false, 80),
        AchievementEntity("ach_streak3_$uid", uid, "Chakra Ignition", "Maintain a 3-day study streak", "STREAK_3", false, 150),
        AchievementEntity("ach_streak7_$uid", uid, "Shadow Discipline", "Maintain a 7-day study streak", "STREAK_7", false, 300),
        AchievementEntity("ach_focus60_$uid", uid, "Deep Meditation", "Complete 60+ minutes of focus sessions", "FOCUS_60", false, 120),
        AchievementEntity("ach_srank_$uid", uid, "S-Rank Clearance", "Complete an S-Rank mission or exam prep", "S_RANK", false, 250),
        AchievementEntity("ach_scanner_$uid", uid, "Scroll Scholar", "Organize 3 study scrolls or 5 missions", "SCROLL_MASTER", false, 100)
    )

    suspend fun saveProfile(profile: UserProfileEntity) {
        val updated = profile.copy(updatedAtEpochMs = System.currentTimeMillis())
        profileDao.upsertProfile(updated)
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        settingsDao.upsertSettings(
            settings.copy(
                aiEnabled = false,
                floatingAiEnabled = false,
                offlineModeOnly = true,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun addOrUpdateSubject(subject: SubjectEntity) {
        val updated = subject.copy(updatedAtEpochMs = System.currentTimeMillis(), synced = false)
        subjectDao.upsertSubject(updated)
    }

    suspend fun deleteSubject(subjectId: String) {
        subjectDao.deleteById(subjectId)
    }

    suspend fun saveTask(task: PlannerTaskEntity, subtasks: List<SubtaskEntity> = emptyList()) {
        val now = System.currentTimeMillis()
        val entity = task.copy(updatedAtEpochMs = now, synced = false)
        taskDao.upsertTask(entity)
        subtasks.forEachIndexed { idx, sub ->
            subtaskDao.upsertSubtask(sub.copy(taskId = entity.id, sortOrder = idx))
        }
        ReminderScheduler.scheduleTaskReminders(context, entity)
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        subtaskDao.upsertSubtask(subtask.copy(completed = !subtask.completed))
    }

    /**
     * Toggles task completion, awards/revokes XP, updates streaks & achievements,
     * and generates next recurrence instance when completing a recurring task.
     * Returns Pair(xpDelta, leveledUpNewTitleOrNull)
     */
    suspend fun toggleTaskCompletion(task: PlannerTaskEntity): Pair<Int, String?> {
        val nowCompleted = !task.completed
        val todayIso = ShinobiTimeUtil.nowZoned().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val updatedTask = task.copy(
            completed = nowCompleted,
            completedAtIso = if (nowCompleted) todayIso else ""
        )
        saveTask(updatedTask)

        var xpDelta = 0
        var rankUpTitle: String? = null
        val profile = profileDao.getProfileOnce(task.userId)
        if (profile != null) {
            if (nowCompleted) {
                val streakBonus = RankProgression.streakBonusXp(profile.currentStreak)
                xpDelta = task.xpReward + (streakBonus / 4)
                val newXp = profile.xp + xpDelta
                val rankInfo = RankProgression.computeLevelInfo(newXp)
                if (rankInfo.level > profile.level) {
                    rankUpTitle = "Lv.${rankInfo.level} ${rankInfo.title}"
                }
                val newStreak = computeNextStreak(profile.lastActiveDate, profile.currentStreak, todayIso)
                val newLongest = maxOf(profile.longestStreak, newStreak)
                val updatedProfile = profile.copy(
                    xp = newXp,
                    level = rankInfo.level,
                    ninjaTitle = rankInfo.title,
                    currentStreak = newStreak,
                    longestStreak = newLongest,
                    lastActiveDate = todayIso
                )
                saveProfile(updatedProfile)
                historyDao.insertHistory(
                    TaskCompletionHistoryEntity(
                        userId = task.userId,
                        taskId = task.id,
                        taskTitle = task.title,
                        subjectName = task.subjectName,
                        missionRank = task.missionRank,
                        xpEarned = xpDelta
                    )
                )
                evaluateAchievements(task.userId, newStreak, task.missionRank)

                val rec = RecurrenceRule.fromCode(task.recurrence)
                if (rec != RecurrenceRule.NONE) {
                    val currentDate = runCatching { LocalDate.parse(task.dateIso) }.getOrNull()
                    if (currentDate != null) {
                        val nextDate = when (rec) {
                            RecurrenceRule.DAILY -> currentDate.plusDays(1)
                            RecurrenceRule.WEEKLY -> currentDate.plusWeeks(1)
                            RecurrenceRule.WEEKDAYS -> {
                                var d = currentDate.plusDays(1)
                                while (d.dayOfWeek.value >= 6) d = d.plusDays(1)
                                d
                            }
                            RecurrenceRule.NONE -> currentDate
                        }
                        val nextIso = nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val existingNext = taskDao.getTasksOnce(task.userId).any {
                            it.title == task.title && it.dateIso == nextIso && it.startTimeMinutes == task.startTimeMinutes
                        }
                        if (!existingNext) {
                            saveTask(
                                task.copy(
                                    id = UUID.randomUUID().toString(),
                                    dateIso = nextIso,
                                    completed = false,
                                    completedAtIso = "",
                                    synced = false
                                )
                            )
                        }
                    }
                }
            } else {
                xpDelta = -task.xpReward
                val newXp = (profile.xp - task.xpReward).coerceAtLeast(0)
                val rankInfo = RankProgression.computeLevelInfo(newXp)
                saveProfile(
                    profile.copy(
                        xp = newXp,
                        level = rankInfo.level,
                        ninjaTitle = rankInfo.title
                    )
                )
            }
        }
        return xpDelta to rankUpTitle
    }

    private fun computeNextStreak(lastDateIso: String, currentStreak: Int, todayIso: String): Int {
        if (lastDateIso.isBlank()) return 1
        if (lastDateIso == todayIso) return currentStreak.coerceAtLeast(1)
        val last = runCatching { LocalDate.parse(lastDateIso) }.getOrNull() ?: return 1
        val today = runCatching { LocalDate.parse(todayIso) }.getOrNull() ?: return 1
        return if (last.plusDays(1) == today) currentStreak + 1 else 1
    }

    private suspend fun evaluateAchievements(uid: String, streak: Int, completedRank: String) {
        val current = historyDao.getAchievementsOnce(uid)
        if (current.isEmpty()) return
        val sessions = sessionDao.getSessionsOnce(uid)
        val totalFocusMins = sessions.sumOf { it.durationMinutes }
        val notes = notesDao.observeNotes(uid).first()
        val tasks = taskDao.getTasksOnce(uid)
        val scans = notesDao.observeScans(uid).first()

        val updated = current.map { ach ->
            val shouldUnlock = ach.unlocked || when (ach.badgeCode) {
                "FIRST_SCROLL" -> true
                "STREAK_3" -> streak >= 3
                "STREAK_7" -> streak >= 7
                "FOCUS_60" -> totalFocusMins >= 60
                "S_RANK" -> completedRank.equals("S", ignoreCase = true)
                "AI_SCAN", "SCROLL_MASTER" -> scans.isNotEmpty() || notes.size >= 3 || tasks.size >= 5
                else -> false
            }
            if (shouldUnlock && !ach.unlocked) {
                ach.copy(unlocked = true, unlockedAtEpochMs = System.currentTimeMillis())
            } else ach
        }
        historyDao.upsertAchievements(updated)
    }

    suspend fun deleteTask(task: PlannerTaskEntity) {
        ReminderScheduler.cancelTaskReminders(context, task.id)
        taskDao.deleteById(task.id)
    }

    suspend fun duplicateTask(task: PlannerTaskEntity) {
        saveTask(
            task.copy(
                id = UUID.randomUUID().toString(),
                title = "${task.title} (Copy)",
                completed = false,
                completedAtIso = "",
                synced = false
            )
        )
    }

    suspend fun reorderTasks(tasksInNewOrder: List<PlannerTaskEntity>) {
        tasksInNewOrder.forEachIndexed { index, task ->
            taskDao.upsertTask(task.copy(sortOrder = index))
        }
    }

    suspend fun recordStudySession(
        userId: String,
        subjectName: String,
        mode: String,
        durationMinutes: Int,
        taskId: String = ""
    ): Pair<Int, String?> {
        val xp = (durationMinutes * 3).coerceIn(10, 600)
        val now = System.currentTimeMillis()
        val session = StudySessionEntity(
            userId = userId,
            taskId = taskId,
            subjectName = subjectName.ifBlank { "General Study" },
            mode = mode,
            durationMinutes = durationMinutes,
            xpEarned = xp,
            startedAtEpochMs = now - durationMinutes * 60_000L,
            endedAtEpochMs = now,
            synced = false
        )
        sessionDao.insertSession(session)

        val todayIso = ShinobiTimeUtil.nowZoned().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val profile = profileDao.getProfileOnce(userId)
        var rankUpTitle: String? = null
        if (profile != null) {
            val newXp = profile.xp + xp
            val rankInfo = RankProgression.computeLevelInfo(newXp)
            if (rankInfo.level > profile.level) rankUpTitle = "Lv.${rankInfo.level} ${rankInfo.title}"
            val newStreak = computeNextStreak(profile.lastActiveDate, profile.currentStreak, todayIso)
            saveProfile(
                profile.copy(
                    xp = newXp,
                    level = rankInfo.level,
                    ninjaTitle = rankInfo.title,
                    currentStreak = newStreak,
                    longestStreak = maxOf(profile.longestStreak, newStreak),
                    lastActiveDate = todayIso
                )
            )
            evaluateAchievements(userId, newStreak, "C")
        }
        return xp to rankUpTitle
    }

    suspend fun saveNote(note: StudyNoteEntity) {
        val updated = note.copy(updatedAtEpochMs = System.currentTimeMillis(), synced = false)
        notesDao.upsertNote(updated)
        val profile = profileDao.getProfileOnce(note.userId)
        evaluateAchievements(note.userId, profile?.currentStreak ?: 0, "C")
    }

    suspend fun deleteNote(noteId: String) = notesDao.deleteNote(noteId)

    suspend fun upsertWallpaper(wallpaper: WallpaperItemEntity) = mediaDao.upsertWallpaper(wallpaper)

    suspend fun setBuiltinWallpaper(userId: String, screenTarget: String, resName: String) {
        val normalizedTarget = screenTarget.uppercase()
        val existing = mediaDao.observeWallpapers(userId).first()
        val existingBuiltIn = existing.firstOrNull {
            it.screenTarget.equals(normalizedTarget, ignoreCase = true) && it.uriString.startsWith("builtin://")
        }
        val entity = WallpaperItemEntity(
            id = existingBuiltIn?.id ?: "builtin_sel_${normalizedTarget}_$userId",
            userId = userId,
            screenTarget = normalizedTarget,
            uriString = "builtin://$resName",
            builtInResName = resName,
            brightness = existingBuiltIn?.brightness ?: 0.72f,
            blurRadius = existingBuiltIn?.blurRadius ?: 2f,
            overlayOpacity = existingBuiltIn?.overlayOpacity ?: 0.65f,
            parallaxEnabled = existingBuiltIn?.parallaxEnabled ?: true
        )
        mediaDao.upsertWallpaper(entity)
    }

    suspend fun deleteWallpaper(id: String) = mediaDao.deleteWallpaper(id)

    suspend fun upsertTrack(track: MusicTrackEntity) = mediaDao.upsertTrack(track)
    suspend fun deleteTrack(id: String) = mediaDao.deleteTrack(id)

    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        taskDao.deleteAllForUser(userId)
        subjectDao.deleteAllForUser(userId)
        sessionDao.deleteAllForUser(userId)
        historyDao.deleteHistoryForUser(userId)
        historyDao.deleteAchievementsForUser(userId)
        chatDao.deleteAllChatsForUser(userId)
        mediaDao.deleteAllWallpapersForUser(userId)
        notesDao.deleteAllNotesForUser(userId)
        settingsDao.deleteSettings(userId)
        profileDao.deleteProfile(userId)
        return Result.success(Unit)
    }

    fun detectConflicts(tasksForDay: List<PlannerTaskEntity>): List<TaskConflict> {
        val sorted = tasksForDay.filter { !it.completed }.sortedBy { it.startTimeMinutes }
        val conflicts = mutableListOf<TaskConflict>()
        for (i in 0 until sorted.size) {
            for (j in i + 1 until sorted.size) {
                val a = sorted[i]
                val b = sorted[j]
                if (b.startTimeMinutes < a.endTimeMinutes && a.startTimeMinutes < b.endTimeMinutes) {
                    conflicts.add(TaskConflict(a, b))
                }
            }
        }
        return conflicts
    }

    fun findFreeTimeSlots(
        tasksForDay: List<PlannerTaskEntity>,
        dayStartMinutes: Int = 8 * 60,
        dayEndMinutes: Int = 22 * 60,
        minDurationMinutes: Int = 30
    ): List<TimeSlot> {
        val busy = tasksForDay
            .filter { !it.completed }
            .sortedBy { it.startTimeMinutes }
            .map { it.startTimeMinutes.coerceAtLeast(dayStartMinutes) to it.endTimeMinutes.coerceAtMost(dayEndMinutes) }
            .filter { it.second > it.first }

        val free = mutableListOf<TimeSlot>()
        var cursor = dayStartMinutes
        for ((start, end) in busy) {
            if (start - cursor >= minDurationMinutes) {
                free.add(TimeSlot(cursor, start))
            }
            if (end > cursor) cursor = end
        }
        if (dayEndMinutes - cursor >= minDurationMinutes) {
            free.add(TimeSlot(cursor, dayEndMinutes))
        }
        return free
    }
}
