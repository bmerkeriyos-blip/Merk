package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getProfileOnce(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun deleteProfile(uid: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun observeSubjects(userId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE userId = :userId")
    suspend fun getSubjectsOnce(userId: String): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subjects WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface PlannerTaskDao {
    @Query("SELECT * FROM planner_tasks WHERE userId = :userId ORDER BY dateIso ASC, startTimeMinutes ASC, sortOrder ASC")
    fun observeTasks(userId: String): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE userId = :userId")
    suspend fun getTasksOnce(userId: String): List<PlannerTaskEntity>

    @Query("SELECT * FROM planner_tasks WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedTasks(userId: String): List<PlannerTaskEntity>

    @Query("SELECT * FROM planner_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): PlannerTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: PlannerTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<PlannerTaskEntity>)

    @Update
    suspend fun updateTask(task: PlannerTaskEntity)

    @Query("DELETE FROM planner_tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM planner_tasks WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId IN (SELECT id FROM planner_tasks WHERE userId = :userId) ORDER BY sortOrder ASC")
    fun observeAllSubtasksForUser(userId: String): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: String)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY startedAtEpochMs DESC")
    fun observeSessions(userId: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId")
    suspend fun getSessionsOnce(userId: String): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface HistoryAndAchievementDao {
    @Query("SELECT * FROM task_completion_history WHERE userId = :userId ORDER BY completedAtEpochMs DESC LIMIT 100")
    fun observeHistory(userId: String): Flow<List<TaskCompletionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: TaskCompletionHistoryEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlocked DESC, xpBonus ASC")
    fun observeAchievements(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAchievementsOnce(userId: String): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(items: List<AchievementEntity>)

    @Query("DELETE FROM task_completion_history WHERE userId = :userId")
    suspend fun deleteHistoryForUser(userId: String)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteAchievementsForUser(userId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY updatedAtEpochMs DESC")
    fun observeChats(userId: String): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(chat: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chat_sessions WHERE userId = :userId")
    suspend fun deleteAllChatsForUser(userId: String)

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestampEpochMs ASC")
    fun observeMessages(chatId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)
}

@Dao
interface MediaAndWallpaperDao {
    @Query("SELECT * FROM wallpapers WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeWallpapers(userId: String): Flow<List<WallpaperItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallpaper(wallpaper: WallpaperItemEntity)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteWallpaper(id: String)

    @Query("DELETE FROM wallpapers WHERE userId = :userId")
    suspend fun deleteAllWallpapersForUser(userId: String)

    @Query("SELECT * FROM music_tracks WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeTracks(userId: String): Flow<List<MusicTrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE userId = :userId")
    suspend fun getTracksOnce(userId: String): List<MusicTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: MusicTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllTracks(tracks: List<MusicTrackEntity>)

    @Query("DELETE FROM music_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)
}

@Dao
interface NotesAndScansDao {
    @Query("SELECT * FROM study_notes WHERE userId = :userId ORDER BY updatedAtEpochMs DESC")
    fun observeNotes(userId: String): Flow<List<StudyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: StudyNoteEntity)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("DELETE FROM study_notes WHERE userId = :userId")
    suspend fun deleteAllNotesForUser(userId: String)

    @Query("SELECT * FROM timetable_scans WHERE userId = :userId ORDER BY scannedAtEpochMs DESC")
    fun observeScans(userId: String): Flow<List<TimetableScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: TimetableScanEntity)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun observeSettings(userId: String): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsOnce(userId: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteSettings(userId: String)
}
