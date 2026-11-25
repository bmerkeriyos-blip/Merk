package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.NarutoVisualCatalog
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiAudioEngine
import com.example.core.media.ShinobiSfx
import com.example.core.media.ShinobiSoundEffectManager
import com.example.core.notifications.ReminderScheduler
import com.example.core.notifications.ShinobiNotificationManager
import com.example.data.local.MusicTrackEntity
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.ShinobiDatabase
import com.example.data.local.SubtaskEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.repository.ShinobiRepository
import com.example.ui.components.GlassTier
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.schedule.TaskEditorModalSheet
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.ui.theme.ThemePreset
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import com.example.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ShinobiDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ShinobiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches Shinobi Planner and offline manifest omits internet permission`() {
        assertEquals("Shinobi Planner", context.getString(R.string.app_name))
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(requested.contains(android.Manifest.permission.INTERNET))
        assertFalse(requested.contains(android.Manifest.permission.CAMERA))
    }

    @Test
    fun `bundled naruto visual catalog includes 12 offline artworks resolving to real drawables`() {
        assertTrue(NarutoVisualCatalog.artworks.size >= 12)
        NarutoVisualCatalog.artworks.forEach { artwork ->
            assertTrue("Expected non-zero drawable for ${artwork.resName}", artwork.drawableRes != 0)
            assertEquals(artwork.drawableRes, NarutoVisualCatalog.resolveDrawable("builtin://${artwork.resName}"))
        }
    }

    @Test
    fun `room database persists profile tasks and cascades subtasks`() = runTest {
        val uid = "shinobi_test_user"
        db.userProfileDao().upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Kakashi",
                username = "kakashi",
                email = "",
                university = "Hidden Leaf Tech"
            )
        )
        val task = PlannerTaskEntity(
            id = "t1",
            userId = uid,
            title = "Distributed Systems Lab",
            subjectName = "CS402",
            dateIso = "2026-04-02",
            startTimeMinutes = 540,
            endTimeMinutes = 630
        )
        db.plannerTaskDao().upsertTask(task)
        db.subtaskDao().upsertSubtask(
            SubtaskEntity(id = "s1", taskId = "t1", title = "Run Raft consensus test")
        )

        val savedTasks = db.plannerTaskDao().getTasksOnce(uid)
        assertEquals(1, savedTasks.size)
        assertEquals(1, db.subtaskDao().observeSubtasks("t1").first().size)

        db.plannerTaskDao().deleteById("t1")
        assertTrue(db.subtaskDao().observeSubtasks("t1").first().isEmpty())
    }

    @Test
    fun `repository preserves existing local profile and unlocks offline achievements on task completion`() = runTest {
        val repo = ShinobiRepository(context, db)
        val legacyUid = "legacy_user_42"
        db.userProfileDao().upsertProfile(
            UserProfileEntity(
                uid = legacyUid,
                displayName = "Itachi",
                username = "itachi",
                email = "",
                xp = 150,
                level = 2,
                onboardingCompleted = true
            )
        )
        val resolvedUid = repo.resolveActiveLocalUid()
        assertEquals(legacyUid, resolvedUid)

        repo.ensureUserInitialized(resolvedUid)
        val task = PlannerTaskEntity(
            id = "mission_1",
            userId = resolvedUid,
            title = "Sage Mode Calculus",
            subjectName = "MATH202",
            dateIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTimeMinutes = 600,
            endTimeMinutes = 690,
            xpReward = 200
        )
        repo.saveTask(task, emptyList())
        repo.toggleTaskCompletion(task)

        val updatedProfile = db.userProfileDao().getProfileOnce(resolvedUid)
        assertNotNull(updatedProfile)
        assertTrue((updatedProfile?.xp ?: 0) >= 350)
        val achievements = db.historyAndAchievementDao().getAchievementsOnce(resolvedUid)
        assertTrue(achievements.first { it.badgeCode == "FIRST_SCROLL" }.unlocked)
    }

    @Test
    fun `notification channels are registered cleanly`() {
        ShinobiNotificationManager.ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(manager.getNotificationChannel(ShinobiNotificationManager.CHANNEL_CLASSES))
        assertNotNull(manager.getNotificationChannel(ShinobiNotificationManager.CHANNEL_ASSIGNMENTS))
        assertNotNull(manager.getNotificationChannel(ShinobiNotificationManager.CHANNEL_EXAMS))
        assertNotNull(manager.getNotificationChannel(ShinobiNotificationManager.CHANNEL_FOCUS))
        assertNotNull(manager.getNotificationChannel(ShinobiNotificationManager.CHANNEL_BRIEFING))
    }

    @Test
    fun `dashboard renders scholar greeting streak and quick navigation`() {
        val state = PlannerUiState(
            profile = UserProfileEntity(
                uid = "u1",
                displayName = "Naruto Scholar",
                username = "naruto",
                email = "",
                xp = 650,
                level = 4,
                currentStreak = 5
            ),
            settings = UserSettingsEntity(userId = "u1", themePreset = ThemePreset.NARUTO_CHAKRA.name)
        )

        composeRule.setContent {
            ShinobiPlannerTheme(preset = ThemePreset.NARUTO_CHAKRA) {
                DashboardScreen(
                    uiState = state,
                    focusState = FocusTimerUiState(),
                    onToggleTask = {},
                    onQuickFocusToggle = {},
                    onOpenScheduleAdd = {},
                    onOpenFocusDojo = {},
                    onOpenProgress = {},
                    onSaveWidgetsOrder = {}
                )
            }
        }

        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithText("5d Streak").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_new_mission").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_focus_dojo").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_progress").assertIsDisplayed()
    }

    @Test
    fun `holo card uses light onSurface content color in dark theme instead of black`() {
        var resolvedContentColor: Color? = null
        composeRule.setContent {
            ShinobiPlannerTheme {
                com.example.ui.components.HoloCard {
                    resolvedContentColor = androidx.compose.material3.LocalContentColor.current
                    Text("Contrast Check")
                }
            }
        }
        composeRule.waitForIdle()
        assertNotNull(resolvedContentColor)
        assertTrue(resolvedContentColor != Color.Black)
        assertEquals(CyberTextPrimary, resolvedContentColor)
    }

    @Test
    fun `settings screen switches themes toggles am pm glow and previews shuriken and jutsu sfx`() {
        var latestSettings by mutableStateOf(
            UserSettingsEntity(
                userId = "local_shinobi",
                themePreset = ThemePreset.NARUTO_CHAKRA.name,
                use24HourClock = false,
                globalGlowEnabled = true
            )
        )
        val playedSfx = mutableListOf<ShinobiSfx>()

        composeRule.setContent {
            val state = PlannerUiState(
                profile = UserProfileEntity(
                    uid = "local_shinobi",
                    displayName = "Shinobi Scholar",
                    username = "local_shinobi",
                    email = ""
                ),
                settings = latestSettings
            )
            ShinobiPlannerTheme(
                preset = ThemePreset.fromStorage(latestSettings.themePreset),
                globalGlowEnabled = latestSettings.globalGlowEnabled
            ) {
                SettingsScreen(
                    uiState = state,
                    audioState = AudioPlaybackState(),
                    onUpdateProfile = {},
                    onUpdateSettings = { transform -> latestSettings = transform(latestSettings) },
                    onSelectBuiltinWallpaper = { _, _ -> },
                    onAddWallpaper = { _, _ -> },
                    onUpdateWallpaperTuning = { _, _, _, _, _ -> },
                    onDeleteWallpaper = {},
                    onResetWallpapers = {},
                    onAddTrack = { _, _, _ -> },
                    onDeleteTrack = {},
                    onPlayTrackAt = {},
                    onTogglePlayPauseMusic = {},
                    onNextTrack = {},
                    onPreviousTrack = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onSpeakMotivation = {},
                    onPlaySfx = { playedSfx += it },
                    onExportJson = { "{}" },
                    onExportCsv = { "" },
                    onWritePdf = { true },
                    onResetLocalData = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("theme_preset_row"))
        composeRule.onNodeWithTag("theme_preset_row").performScrollToNode(hasTestTag("theme_preset_akatsuki"))
        composeRule.onNodeWithTag("theme_preset_akatsuki").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemePreset.AKATSUKI.name, latestSettings.themePreset)

        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("global_glow_switch"))
        composeRule.onNodeWithTag("global_glow_switch").performClick()
        composeRule.waitForIdle()
        assertEquals(false, latestSettings.globalGlowEnabled)

        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("am_pm_time_switch"))
        composeRule.onNodeWithTag("am_pm_time_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(latestSettings.use24HourClock)

        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("test_shuriken_sfx_button"))
        composeRule.onNodeWithTag("test_shuriken_sfx_button").performClick()
        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("test_jutsu_sfx_button"))
        composeRule.onNodeWithTag("test_jutsu_sfx_button").performClick()
        assertEquals(listOf(ShinobiSfx.SHURIKEN, ShinobiSfx.JUTSU), playedSfx)
    }

    @Test
    fun `sound effect manager initializes and plays shuriken and jutsu safely`() {
        val manager = ShinobiSoundEffectManager(context)
        manager.updateConfig(volume = 0.7f, soundEnabled = true, hapticsEnabled = false, quietMuted = false)
        manager.play(ShinobiSfx.SHURIKEN)
        manager.play(ShinobiSfx.JUTSU)
        manager.release()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `audio playlist purges legacy built in theme songs and keeps only user imported tracks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val audioEngine = ShinobiAudioEngine(context)
        try {
            val repo = ShinobiRepository(context, db)
            val uid = ShinobiRepository.LOCAL_GUEST_UID
            repo.upsertTrack(
                MusicTrackEntity(
                    id = "synth_dojo_$uid",
                    userId = uid,
                    title = "Hidden Village Dawn (Original)",
                    uriString = "file:///tmp/synth.wav",
                    builtInSynth = true,
                    sortOrder = 0
                )
            )
            repo.upsertTrack(
                MusicTrackEntity(
                    id = "user_custom_1",
                    userId = uid,
                    title = "My Own Track",
                    uriString = "content://media/external/audio/1",
                    builtInSynth = false,
                    sortOrder = 1
                )
            )
            val vm = PlannerViewModel(repo, audioEngine, uid)
            vm.initializeSession()
            var attempts = 0
            while (repo.observeTracks(uid).first().any { it.builtInSynth } && attempts < 100) {
                Thread.sleep(20)
                advanceUntilIdle()
                attempts++
            }
            val remaining = repo.observeTracks(uid).first()
            assertEquals(1, remaining.size)
            assertEquals("My Own Track", remaining.first().title)
            assertEquals(1, audioEngine.playbackState.value.trackCount)
        } finally {
            audioEngine.release()
            Dispatchers.resetMain()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], qualifiers = "w411dp-h891dp")
class Android12CompatibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `android 12 renders ShinobiGlassModifier global glow and custom AM PM plan time editor cleanly`() {
        var savedTask: PlannerTaskEntity? = null
        val latestSettings = UserSettingsEntity(
            userId = "s31",
            globalGlowEnabled = true,
            use24HourClock = false,
            quietHoursEnabled = true
        )

        composeRule.setContent {
            ShinobiPlannerTheme(
                globalGlowEnabled = latestSettings.globalGlowEnabled,
                glowIntensity = 0.85f
            ) {
                Surface(
                    modifier = Modifier
                        .ShinobiGlassModifier(accentColor = NarutoOrange, tier = GlassTier.HERO, glowActive = true)
                        .padding(8.dp)
                        .testTag("sdk31_glass_surface"),
                    color = Color.Transparent
                ) {
                    Text("Android 12 Glass Active")
                }
                TaskEditorModalSheet(
                    existingTask = null,
                    existingSubtasks = emptyList(),
                    subjects = emptyList(),
                    defaultDateIso = "2026-11-22",
                    use24HourClock = latestSettings.use24HourClock,
                    onDismiss = {},
                    onSave = { task, _ -> savedTask = task }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_time_display").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start_time_pm").performScrollTo().performClick()
        composeRule.onNodeWithTag("start_min_plus").performScrollTo().performClick()
        composeRule.onNodeWithTag("end_hour_plus").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_task_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNotNull(savedTask)
        assertEquals(21 * 60 + 5, savedTask!!.startTimeMinutes)
    }

    @Test
    fun `android 12 notifications sound effects and Room MIGRATION_2_3 succeed cleanly`() {
        assertTrue(ShinobiNotificationManager.hasNotificationPermission(context))
        ShinobiNotificationManager.ensureChannels(context)
        ShinobiNotificationManager.showTaskReminder(
            context = context,
            taskId = "sdk31_alarm",
            title = "OS Exam Prep",
            category = "EXAM",
            notifId = 1099
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = Shadows.shadowOf(nm).allNotifications
        assertTrue(posted.isNotEmpty())
        assertEquals(R.drawable.ic_shinobi_notification, posted.first().smallIcon.resId)

        val sfxManager = ShinobiSoundEffectManager(context)
        sfxManager.updateConfig(volume = 0.7f, soundEnabled = true, hapticsEnabled = true, quietMuted = false)
        sfxManager.play(ShinobiSfx.SHURIKEN)
        sfxManager.play(ShinobiSfx.JUTSU)
        sfxManager.release()

        val futureDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val task = PlannerTaskEntity(
            id = "sdk31_alarm",
            userId = "s31",
            title = "OS Exam Prep",
            subjectName = "CS401",
            dateIso = futureDate,
            startTimeMinutes = 15 * 60,
            endTimeMinutes = 16 * 60,
            reminderOffsetsCsv = "10"
        )
        ReminderScheduler.scheduleTaskReminders(context, task)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
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
                        db.execSQL(
                            "INSERT INTO `user_settings` VALUES ('s31','NARUTO','DARK','ORANGE',1,0.8,1.0,1,1.0,0,0,1,1,1,1,1,'MENTOR',0,'UTC',0,1,'MONDAY',1,22,6,1,1,0.35,0.65,0.75,1,1,1,0,300,0,0,0,'',0,'FOCUS,NEXT,PROGRESS,STREAK,TIMELINE,AI',1000)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.use {
            val rawDb = it.writableDatabase
            ShinobiDatabase.MIGRATION_2_3.migrate(rawDb)
            val cursor = rawDb.query("SELECT globalGlowEnabled FROM user_settings WHERE userId = 's31'")
            cursor.use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], qualifiers = "w411dp-h891dp")
class MainActivityAndroid12LaunchTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `cold launch MainActivity on Android 12 starts offline Onboarding and traverses all main tabs cleanly`() {
        activityRule.waitUntil(timeoutMillis = 5_000L) {
            activityRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(activityRule.onAllNodesWithTag("skip_sign_in_button").fetchSemanticsNodes().isEmpty())
        activityRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed().performClick()

        activityRule.waitUntil(timeoutMillis = 5_000L) {
            activityRule.onAllNodesWithTag("dashboard_screen").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()

        activityRule.onNodeWithTag("nav_schedule").performClick()
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("nav_missions").performClick()
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("nav_progress").performClick()
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("nav_settings").performClick()
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("nav_home").performClick()
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    }
}
