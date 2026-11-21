package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        SubjectEntity::class,
        PlannerTaskEntity::class,
        SubtaskEntity::class,
        StudySessionEntity::class,
        TaskCompletionHistoryEntity::class,
        AchievementEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        WallpaperItemEntity::class,
        MusicTrackEntity::class,
        StudyNoteEntity::class,
        TimetableScanEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShinobiDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun plannerTaskDao(): PlannerTaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun historyAndAchievementDao(): HistoryAndAchievementDao
    abstract fun chatDao(): ChatDao
    abstract fun mediaAndWallpaperDao(): MediaAndWallpaperDao
    abstract fun notesAndScansDao(): NotesAndScansDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: ShinobiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema baseline v1 -> future v2 migrations go here cleanly without dropping user data.
            }
        }

        fun getInstance(context: Context): ShinobiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShinobiDatabase::class.java,
                    "shinobi_planner.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
