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
    version = 2,
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
                db.execSQL("DROP TABLE IF EXISTS user_settings")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_settings` (
                        `userId` TEXT NOT NULL,
                        `themePreset` TEXT NOT NULL,
                        `appearanceMode` TEXT NOT NULL,
                        `accentColorKey` TEXT NOT NULL,
                        `glassmorphism` INTEGER NOT NULL,
                        `glowIntensity` REAL NOT NULL,
                        `fontScale` REAL NOT NULL,
                        `animationsEnabled` INTEGER NOT NULL,
                        `animationSpeed` REAL NOT NULL,
                        `reduceMotion` INTEGER NOT NULL,
                        `batterySaverMode` INTEGER NOT NULL,
                        `hapticsEnabled` INTEGER NOT NULL,
                        `floatingAiEnabled` INTEGER NOT NULL,
                        `floatingAiDisableInFocus` INTEGER NOT NULL,
                        `floatingAiHideWhileTyping` INTEGER NOT NULL,
                        `aiEnabled` INTEGER NOT NULL,
                        `aiPersonality` TEXT NOT NULL,
                        `autoSaveScans` INTEGER NOT NULL,
                        `timezoneId` TEXT NOT NULL,
                        `use24HourClock` INTEGER NOT NULL,
                        `showEthiopianTime` INTEGER NOT NULL,
                        `weekStartDay` TEXT NOT NULL,
                        `quietHoursEnabled` INTEGER NOT NULL,
                        `quietStartHour` INTEGER NOT NULL,
                        `quietEndHour` INTEGER NOT NULL,
                        `soundEnabled` INTEGER NOT NULL,
                        `vibrationEnabled` INTEGER NOT NULL,
                        `musicVolume` REAL NOT NULL,
                        `sfxVolume` REAL NOT NULL,
                        `motivationalVolume` REAL NOT NULL,
                        `musicOnOpen` INTEGER NOT NULL,
                        `musicDuringFocus` INTEGER NOT NULL,
                        `musicOnComplete` INTEGER NOT NULL,
                        `wallpaperSlideshowEnabled` INTEGER NOT NULL,
                        `wallpaperSlideshowIntervalSec` INTEGER NOT NULL,
                        `wallpaperRandomOrder` INTEGER NOT NULL,
                        `offlineModeOnly` INTEGER NOT NULL,
                        `appLockEnabled` INTEGER NOT NULL,
                        `appLockPin` TEXT NOT NULL,
                        `biometricsEnabled` INTEGER NOT NULL,
                        `dashboardWidgetsCsv` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )
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
