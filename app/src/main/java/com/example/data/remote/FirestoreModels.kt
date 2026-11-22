package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileCloud(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val university: String = "",
    val department: String = "",
    val academicYear: String = "",
    val xp: Long = 0L,
    val level: Long = 1L,
    val ninjaTitle: String = "Academy Student",
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val onboardingCompleted: Boolean = false,
    val photoUrl: String? = null,
    val lastActiveDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(100),
        "username" to username.take(60),
        "email" to email.take(150),
        "university" to university.take(120),
        "department" to department.take(120),
        "academicYear" to academicYear.take(40),
        "xp" to xp.coerceAtLeast(0L),
        "level" to level.coerceAtLeast(1L),
        "ninjaTitle" to ninjaTitle.take(60),
        "currentStreak" to currentStreak.coerceAtLeast(0L),
        "longestStreak" to longestStreak.coerceAtLeast(0L),
        "onboardingCompleted" to onboardingCompleted,
        "photoUrl" to photoUrl?.take(500),
        "lastActiveDate" to lastActiveDate?.take(30),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}

@Keep
@IgnoreExtraProperties
data class CloudTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val subjectName: String? = null,
    val category: String = "STUDY",
    val dateIso: String = "",
    val startTimeMinutes: Long = 540L,
    val endTimeMinutes: Long = 600L,
    val priority: String = "MEDIUM",
    val missionRank: String = "C",
    val recurrence: String = "NONE",
    val location: String? = null,
    val reminderOffsets: String? = null,
    val xpReward: Long = 80L,
    val colorHex: String = "#00E5FF",
    val completed: Boolean = false,
    val subtasksSummary: String? = null,
    val completedAtIso: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(200),
        "notes" to notes?.take(2000),
        "subjectName" to subjectName?.take(100),
        "category" to category,
        "dateIso" to dateIso.take(20),
        "startTimeMinutes" to startTimeMinutes.coerceIn(0L, 1440L),
        "endTimeMinutes" to endTimeMinutes.coerceIn(0L, 1440L),
        "priority" to priority,
        "missionRank" to missionRank,
        "recurrence" to recurrence,
        "location" to location?.take(150),
        "reminderOffsets" to reminderOffsets?.take(100),
        "xpReward" to xpReward.coerceIn(0L, 5000L),
        "colorHex" to colorHex.take(20),
        "completed" to completed,
        "subtasksSummary" to subtasksSummary?.take(2000),
        "completedAtIso" to completedAtIso?.take(40),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}

@Keep
@IgnoreExtraProperties
data class CloudSubject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val instructor: String? = null,
    val room: String? = null,
    val colorHex: String = "#00E5FF",
    val targetHoursPerWeek: Long = 4L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(120),
        "code" to code.take(40),
        "instructor" to instructor?.take(120),
        "room" to room?.take(80),
        "colorHex" to colorHex.take(20),
        "targetHoursPerWeek" to targetHoursPerWeek.coerceIn(0L, 168L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}

@Keep
@IgnoreExtraProperties
data class CloudStudySession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectName: String = "",
    val mode: String = "POMODORO",
    val durationMinutes: Long = 25L,
    val xpEarned: Long = 50L,
    val startedAtEpochMs: Long = 0L,
    val taskId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "subjectName" to subjectName.take(120),
        "mode" to mode,
        "durationMinutes" to durationMinutes.coerceIn(0L, 1440L),
        "xpEarned" to xpEarned.coerceIn(0L, 10000L),
        "startedAtEpochMs" to startedAtEpochMs.coerceAtLeast(0L),
        "taskId" to taskId?.take(128),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}

@Keep
@IgnoreExtraProperties
data class CloudAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val badgeCode: String = "",
    val unlocked: Boolean = false,
    val xpBonus: Long = 100L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(120),
        "description" to description.take(300),
        "badgeCode" to badgeCode.take(60),
        "unlocked" to unlocked,
        "xpBonus" to xpBonus.coerceIn(0L, 10000L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}

@Keep
@IgnoreExtraProperties
data class CloudNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val subjectName: String = "",
    val linkedTaskId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(200),
        "content" to content.take(10000),
        "subjectName" to subjectName.take(120),
        "linkedTaskId" to linkedTaskId?.take(128),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}
