package com.example.data.repository

import android.content.Context
import com.example.core.common.EthiopicCalendarUtil
import com.example.core.common.MissionRank
import com.example.core.common.RankProgression
import com.example.core.common.RecurrenceRule
import com.example.core.notifications.ReminderScheduler
import com.example.data.local.*
import com.example.data.remote.*
import com.google.firebase.auth.FirebaseUser
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
    private val db: ShinobiDatabase,
    private val remote: FirestoreSyncRepository
) {
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
    fun observeChats(uid: String): Flow<List<ChatSessionEntity>> = chatDao.observeChats(uid)
    fun observeMessages(chatId: String): Flow<List<ChatMessageEntity>> = chatDao.observeMessages(chatId)
    fun observeWallpapers(uid: String): Flow<List<WallpaperItemEntity>> = mediaDao.observeWallpapers(uid)
    fun observeTracks(uid: String): Flow<List<MusicTrackEntity>> = mediaDao.observeTracks(uid)
    fun observeNotes(uid: String): Flow<List<StudyNoteEntity>> = notesDao.observeNotes(uid)
    fun observeScans(uid: String): Flow<List<TimetableScanEntity>> = notesDao.observeScans(uid)
    fun observeSettings(uid: String): Flow<UserSettingsEntity?> = settingsDao.observeSettings(uid)

    suspend fun ensureUserInitialized(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        val existingProfile = profileDao.getProfileOnce(uid)
        if (existingProfile == null) {
            val remoteProfile = remote.fetchUserProfile(uid).getOrNull()
            val newProfile = if (remoteProfile != null) {
                UserProfileEntity(
                    uid = uid,
                    displayName = remoteProfile.displayName.ifBlank { firebaseUser.displayName ?: "Shinobi Scholar" },
                    username = remoteProfile.username.ifBlank { "shinobi_${uid.take(6)}" },
                    email = remoteProfile.email.ifBlank { firebaseUser.email ?: "" },
                    photoUrl = remoteProfile.photoUrl ?: firebaseUser.photoUrl?.toString().orEmpty(),
                    university = remoteProfile.university,
                    department = remoteProfile.department,
                    academicYear = remoteProfile.academicYear,
                    xp = remoteProfile.xp.toInt(),
                    level = remoteProfile.level.toInt().coerceAtLeast(1),
                    ninjaTitle = remoteProfile.ninjaTitle,
                    currentStreak = remoteProfile.currentStreak.toInt(),
                    longestStreak = remoteProfile.longestStreak.toInt(),
                    lastActiveDate = remoteProfile.lastActiveDate.orEmpty(),
                    onboardingCompleted = remoteProfile.onboardingCompleted
                )
            } else {
                UserProfileEntity(
                    uid = uid,
                    displayName = firebaseUser.displayName ?: "Shinobi Scholar",
                    username = (firebaseUser.email?.substringBefore("@") ?: "shinobi_${uid.take(5)}"),
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
                    xp = 0,
                    level = 1,
                    ninjaTitle = RankProgression.titleForLevel(1),
                    onboardingCompleted = false
                )
            }
            profileDao.upsertProfile(newProfile)
            if (remoteProfile == null) {
                syncProfileToCloud(newProfile)
            }
        }

        if (settingsDao.getSettingsOnce(uid) == null) {
            settingsDao.upsertSettings(UserSettingsEntity(userId = uid))
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
                    uriString = "builtin://bg_ninja_village",
                    builtInResName = "bg_ninja_village",
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
        AchievementEntity("ach_scanner_$uid", uid, "Optical Jutsu", "Import or scan a timetable with AI", "AI_SCAN", false, 100)
    )

    suspend fun saveProfile(profile: UserProfileEntity) {
        val updated = profile.copy(updatedAtEpochMs = System.currentTimeMillis())
        profileDao.upsertProfile(updated)
        val offlineOnly = settingsDao.getSettingsOnce(profile.uid)?.offlineModeOnly ?: false
        if (!offlineOnly) {
            syncProfileToCloud(updated)
        }
    }

    private suspend fun syncProfileToCloud(profile: UserProfileEntity) {
        remote.upsertUserProfile(
            UserProfileCloud(
                userId = profile.uid,
                displayName = profile.displayName,
                username = profile.username,
                email = profile.email,
                university = profile.university,
                department = profile.department,
                academicYear = profile.academicYear,
                xp = profile.xp.toLong(),
                level = profile.level.toLong(),
                ninjaTitle = profile.ninjaTitle,
                currentStreak = profile.currentStreak.toLong(),
                longestStreak = profile.longestStreak.toLong(),
                onboardingCompleted = profile.onboardingCompleted,
                photoUrl = profile.photoUrl.ifBlank { null },
                lastActiveDate = profile.lastActiveDate.ifBlank { null }
            )
        )
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        settingsDao.upsertSettings(settings.copy(updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun addOrUpdateSubject(subject: SubjectEntity) {
        val updated = subject.copy(updatedAtEpochMs = System.currentTimeMillis(), synced = false)
        subjectDao.upsertSubject(updated)
        val offlineOnly = settingsDao.getSettingsOnce(subject.userId)?.offlineModeOnly ?: false
        if (!offlineOnly) {
            val res = remote.upsertSubject(
                updated.id,
                CloudSubject(
                    userId = updated.userId,
                    name = updated.name,
                    code = updated.code,
                    instructor = updated.instructor.ifBlank { null },
                    room = updated.room.ifBlank { null },
                    colorHex = updated.colorHex,
                    targetHoursPerWeek = updated.targetHoursPerWeek.toLong()
                )
            )
            if (res.isSuccess) {
                subjectDao.upsertSubject(updated.copy(synced = true))
            }
        }
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

        val offlineOnly = settingsDao.getSettingsOnce(entity.userId)?.offlineModeOnly ?: false
        if (!offlineOnly) {
            val summary = subtasks.joinToString(" | ") { "${if (it.completed) "[x]" else "[ ]"} ${it.title}" }
                .ifBlank { null }
            val res = remote.upsertTask(
                entity.id,
                CloudTask(
                    userId = entity.userId,
                    title = entity.title,
                    notes = entity.notes.ifBlank { null },
                    subjectName = entity.subjectName.ifBlank { null },
                    category = entity.category,
                    dateIso = entity.dateIso,
                    startTimeMinutes = entity.startTimeMinutes.toLong(),
                    endTimeMinutes = entity.endTimeMinutes.toLong(),
                    priority = entity.priority,
                    missionRank = entity.missionRank,
                    recurrence = entity.recurrence,
                    location = entity.location.ifBlank { null },
                    reminderOffsets = entity.reminderOffsetsCsv.ifBlank { null },
                    xpReward = entity.xpReward.toLong(),
                    colorHex = entity.colorHex,
                    completed = entity.completed,
                    subtasksSummary = summary,
                    completedAtIso = entity.completedAtIso.ifBlank { null }
                )
            )
            if (res.isSuccess) {
                taskDao.upsertTask(entity.copy(synced = true))
            }
        }
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
        val todayIso = EthiopicCalendarUtil.nowZoned().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
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

                // Auto-create next occurrence if recurring and completing for the first time
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
        val scans = notesDao.observeScans(uid).first()

        val updated = current.map { ach ->
            val shouldUnlock = ach.unlocked || when (ach.badgeCode) {
                "FIRST_SCROLL" -> true
                "STREAK_3" -> streak >= 3
                "STREAK_7" -> streak >= 7
                "FOCUS_60" -> totalFocusMins >= 60
                "S_RANK" -> completedRank.equals("S", ignoreCase = true)
                "AI_SCAN" -> scans.isNotEmpty()
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
        remote.deleteTask(task.id)
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

        val offlineOnly = settingsDao.getSettingsOnce(userId)?.offlineModeOnly ?: false
        if (!offlineOnly) {
            remote.insertStudySession(
                session.id,
                CloudStudySession(
                    userId = userId,
                    subjectName = session.subjectName,
                    mode = mode,
                    durationMinutes = durationMinutes.toLong(),
                    xpEarned = xp.toLong(),
                    startedAtEpochMs = session.startedAtEpochMs,
                    taskId = taskId.ifBlank { null }
                )
            )
        }

        val todayIso = EthiopicCalendarUtil.nowZoned().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
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
        val offlineOnly = settingsDao.getSettingsOnce(note.userId)?.offlineModeOnly ?: false
        if (!offlineOnly) {
            remote.upsertNote(
                updated.id,
                CloudNote(
                    userId = updated.userId,
                    title = updated.title,
                    content = updated.content,
                    subjectName = updated.subjectName,
                    linkedTaskId = updated.linkedTaskId.ifBlank { null }
                )
            )
        }
    }

    suspend fun deleteNote(noteId: String) = notesDao.deleteNote(noteId)

    suspend fun recordTimetableScan(scan: TimetableScanEntity) {
        notesDao.insertScan(scan)
        val profile = profileDao.getProfileOnce(scan.userId)
        evaluateAchievements(scan.userId, profile?.currentStreak ?: 0, "C")
    }

    suspend fun upsertWallpaper(wallpaper: WallpaperItemEntity) = mediaDao.upsertWallpaper(wallpaper)
    suspend fun deleteWallpaper(id: String) = mediaDao.deleteWallpaper(id)

    suspend fun upsertTrack(track: MusicTrackEntity) = mediaDao.upsertTrack(track)
    suspend fun upsertTracks(tracks: List<MusicTrackEntity>) = mediaDao.upsertAllTracks(tracks)
    suspend fun deleteTrack(id: String) = mediaDao.deleteTrack(id)

    suspend fun createOrUpdateChat(chat: ChatSessionEntity) = chatDao.upsertChat(chat)
    suspend fun deleteChat(chatId: String) = chatDao.deleteChat(chatId)
    suspend fun deleteAllChats(userId: String) = chatDao.deleteAllChatsForUser(userId)
    suspend fun saveChatMessage(message: ChatMessageEntity) = chatDao.upsertMessage(message)

    suspend fun syncPendingToCloud(userId: String): Result<Int> {
        var syncedCount = 0
        val unsyncedTasks = taskDao.getUnsyncedTasks(userId)
        for (task in unsyncedTasks) {
            val res = remote.upsertTask(
                task.id,
                CloudTask(
                    userId = task.userId,
                    title = task.title,
                    notes = task.notes.ifBlank { null },
                    subjectName = task.subjectName.ifBlank { null },
                    category = task.category,
                    dateIso = task.dateIso,
                    startTimeMinutes = task.startTimeMinutes.toLong(),
                    endTimeMinutes = task.endTimeMinutes.toLong(),
                    priority = task.priority,
                    missionRank = task.missionRank,
                    recurrence = task.recurrence,
                    location = task.location.ifBlank { null },
                    reminderOffsets = task.reminderOffsetsCsv.ifBlank { null },
                    xpReward = task.xpReward.toLong(),
                    colorHex = task.colorHex,
                    completed = task.completed,
                    completedAtIso = task.completedAtIso.ifBlank { null }
                )
            )
            if (res.isSuccess) {
                taskDao.upsertTask(task.copy(synced = true))
                syncedCount++
            }
        }
        profileDao.getProfileOnce(userId)?.let { syncProfileToCloud(it) }
        val allTasks = taskDao.getTasksOnce(userId)
        val profile = profileDao.getProfileOnce(userId)
        remote.createBackupSnapshot(
            summary = "Shinobi Sync (${allTasks.size} missions)",
            taskCount = allTasks.size,
            xpSnapshot = profile?.xp ?: 0
        )
        return Result.success(syncedCount)
    }

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
        return remote.deleteAllUserCloudData()
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
