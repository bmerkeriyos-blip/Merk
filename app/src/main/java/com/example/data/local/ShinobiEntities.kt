package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val username: String,
    val email: String,
    val photoUrl: String = "",
    val university: String = "",
    val department: String = "",
    val academicYear: String = "",
    val xp: Int = 0,
    val level: Int = 1,
    val ninjaTitle: String = "Academy Student",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val onboardingCompleted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subjects",
    indices = [Index("userId")]
)
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val code: String = "",
    val instructor: String = "",
    val room: String = "",
    val colorHex: String = "#00E5FF",
    val targetHoursPerWeek: Int = 4,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(
    tableName = "planner_tasks",
    indices = [Index("userId"), Index("dateIso"), Index("subjectName")]
)
data class PlannerTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val notes: String = "",
    val subjectName: String = "General",
    val category: String = "STUDY",
    val dateIso: String,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val priority: String = "MEDIUM",
    val missionRank: String = "C",
    val recurrence: String = "NONE",
    val location: String = "",
    val reminderOffsetsCsv: String = "10",
    val xpReward: Int = 80,
    val colorHex: String = "#00E5FF",
    val completed: Boolean = false,
    val completedAtIso: String = "",
    val sortOrder: Int = 0,
    val attachmentUri: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = PlannerTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class SubtaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "study_sessions",
    indices = [Index("userId"), Index("startedAtEpochMs")]
)
data class StudySessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val taskId: String = "",
    val subjectName: String = "General",
    val mode: String = "POMODORO",
    val durationMinutes: Int,
    val xpEarned: Int,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val synced: Boolean = false
)

@Entity(
    tableName = "task_completion_history",
    indices = [Index("userId"), Index("completedAtEpochMs")]
)
data class TaskCompletionHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val taskId: String,
    val taskTitle: String,
    val subjectName: String,
    val missionRank: String,
    val xpEarned: Int,
    val completedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "achievements",
    indices = [Index("userId")]
)
data class AchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val badgeCode: String,
    val unlocked: Boolean = false,
    val xpBonus: Int = 100,
    val unlockedAtEpochMs: Long = 0L,
    val synced: Boolean = false
)

@Entity(
    tableName = "chat_sessions",
    indices = [Index("userId"), Index("updatedAtEpochMs")]
)
data class ChatSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val personality: String = "MENTOR",
    val lastMessagePreview: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId"), Index("userId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val userId: String,
    val role: String, // "user" or "model"
    val content: String,
    val actionSummary: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(
    tableName = "wallpapers",
    indices = [Index("userId"), Index("screenTarget")]
)
data class WallpaperItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val screenTarget: String = "GLOBAL", // GLOBAL, HOME, SCHEDULE, FOCUS, MISSIONS, AI, PROFILE
    val uriString: String,
    val builtInResName: String = "",
    val brightness: Float = 0.72f,
    val blurRadius: Float = 2f,
    val overlayOpacity: Float = 0.62f,
    val parallaxEnabled: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "music_tracks",
    indices = [Index("userId")]
)
data class MusicTrackEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val artist: String = "Shinobi Synth",
    val uriString: String,
    val durationMs: Long = 0L,
    val motivational: Boolean = false,
    val builtInSynth: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "study_notes",
    indices = [Index("userId"), Index("subjectName")]
)
data class StudyNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val content: String,
    val subjectName: String = "General",
    val linkedTaskId: String = "",
    val attachmentUri: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(
    tableName = "timetable_scans",
    indices = [Index("userId"), Index("scannedAtEpochMs")]
)
data class TimetableScanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val sourceType: String, // CAMERA, GALLERY, PDF
    val mode: String,       // QUICK, ACCURATE
    val extractedCount: Int,
    val summary: String,
    val scannedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val themePreset: String = "NARUTO",
    val appearanceMode: String = "DARK",
    val accentColorKey: String = "ORANGE",
    val glassmorphism: Boolean = true,
    val glowIntensity: Float = 0.8f,
    val fontScale: Float = 1.0f,
    val animationsEnabled: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val batterySaverMode: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val floatingAiEnabled: Boolean = true,
    val floatingAiDisableInFocus: Boolean = true,
    val floatingAiHideWhileTyping: Boolean = true,
    val aiEnabled: Boolean = true,
    val aiPersonality: String = "MENTOR",
    val autoSaveScans: Boolean = false,
    val timezoneId: String = "Africa/Addis_Ababa",
    val use24HourClock: Boolean = false,
    val showEthiopianTime: Boolean = true,
    val weekStartDay: String = "MONDAY",
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 6,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.8f,
    val motivationalVolume: Float = 0.85f,
    val musicOnOpen: Boolean = false,
    val musicDuringFocus: Boolean = true,
    val musicOnComplete: Boolean = true,
    val wallpaperSlideshowEnabled: Boolean = false,
    val wallpaperSlideshowIntervalSec: Int = 30,
    val wallpaperRandomOrder: Boolean = false,
    val offlineModeOnly: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricsEnabled: Boolean = false,
    val dashboardWidgetsCsv: String = "PROGRESS,NEXT_CLASS,TIMER,MISSIONS,AI_BRIEFING,WEEKLY,QUICK_ACTIONS",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
