package com.example

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
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
import com.example.core.ai.AiMentorService
import com.example.core.ai.AiPersonality
import com.example.core.ai.ParsedAiCommand
import com.example.core.ai.ShinobiAiBackend
import com.example.core.common.ShinobiTimeUtil
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiSfx
import com.example.core.media.ShinobiSoundEffectManager
import com.example.core.notifications.ReminderScheduler
import com.example.core.notifications.ShinobiNotificationManager
import com.example.data.local.ChatMessageEntity
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
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

private class FakeShinobiAiBackend(
    private val contentHandler: suspend (modelName: String, promptText: String, jsonSchema: Schema?) -> String? = { _, _, _ -> "Focus on Algorithms for 25 minutes." },
    private val multimodalHandler: suspend (modelName: String, promptText: String, images: List<Bitmap>, jsonSchema: Schema) -> String? = { _, _, _, _ -> """{"events":[]}""" },
    private val streamHandler: (modelName: String, systemInstruction: String, history: List<Content>, userMessage: String) -> Flow<String> = { _, _, _, _ -> flow { emit("Shinobi plan ready.") } }
) : ShinobiAiBackend {
    val calledModels = mutableListOf<String>()

    override suspend fun generateContent(modelName: String, promptText: String, jsonSchema: Schema?): String? {
        calledModels += modelName
        return contentHandler(modelName, promptText, jsonSchema)
    }

    override suspend fun generateMultimodalContent(modelName: String, promptText: String, images: List<Bitmap>, jsonSchema: Schema): String? {
        calledModels += modelName
        return multimodalHandler(modelName, promptText, images, jsonSchema)
    }

    override fun streamChatReply(
        modelName: String,
        systemInstruction: String,
        history: List<Content>,
        userMessage: String
    ): Flow<String> {
        calledModels += modelName
        return streamHandler(modelName, systemInstruction, history, userMessage)
    }
}

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
    fun `app name matches Shinobi Planner`() {
        assertEquals("Shinobi Planner", context.getString(R.string.app_name))
    }

    @Test
    fun `room database persists profile tasks and cascades subtasks`() = runTest {
        val uid = "shinobi_test_user"
        db.userProfileDao().upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Kakashi",
                username = "kakashi",
                email = "kakashi@example.com",
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
    fun `dashboard renders scholar greeting and quick actions`() {
        val state = PlannerUiState(
            profile = UserProfileEntity(
                uid = "u1",
                displayName = "Naruto Scholar",
                username = "naruto",
                email = "scholar@example.com",
                xp = 650,
                level = 4,
                currentStreak = 5
            ),
            settings = UserSettingsEntity(userId = "u1", aiEnabled = true)
        )

        composeRule.setContent {
            ShinobiPlannerTheme {
                DashboardScreen(
                    uiState = state,
                    focusState = FocusTimerUiState(),
                    dailySuggestion = "Tackle Algorithms first.",
                    onRefreshSuggestion = {},
                    onToggleTask = {},
                    onQuickFocusToggle = {},
                    onOpenScheduleAdd = {},
                    onOpenFocusDojo = {},
                    onOpenAiMentor = {},
                    onOpenScanner = {},
                    onSaveWidgetsOrder = {}
                )
            }
        }

        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithText("5d Streak").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_new_mission").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_focus_dojo").assertIsDisplayed()
    }

    @Test
    fun `settings screen shows optional google sign in when skipped`() {
        var signInClicked = false
        val guestState = PlannerUiState(
            profile = UserProfileEntity(
                uid = "local_shinobi",
                displayName = "Shinobi Scholar",
                username = "local_shinobi",
                email = ""
            ),
            settings = UserSettingsEntity(userId = "local_shinobi", aiEnabled = true)
        )

        composeRule.setContent {
            ShinobiPlannerTheme {
                SettingsScreen(
                    uiState = guestState,
                    audioState = AudioPlaybackState(),
                    isSignedInWithGoogle = false,
                    onSignInWithGoogle = { signInClicked = true },
                    onUpdateProfile = {},
                    onUpdateSettings = {},
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
                    onTriggerSync = {},
                    onExportJson = { "{}" },
                    onExportCsv = { "" },
                    onWritePdf = { true },
                    onSignOut = {},
                    onDeleteAccountData = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_google_sign_in_button").assertIsDisplayed().performClick()
        assertTrue(signInClicked)
    }

    @Test
    fun `settings screen toggles am pm clock global glow and previews shuriken and jutsu sfx`() {
        var latestSettings by mutableStateOf(
            UserSettingsEntity(
                userId = "local_shinobi",
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
            ShinobiPlannerTheme(globalGlowEnabled = latestSettings.globalGlowEnabled) {
                SettingsScreen(
                    uiState = state,
                    audioState = AudioPlaybackState(),
                    isSignedInWithGoogle = false,
                    onSignInWithGoogle = {},
                    onUpdateProfile = {},
                    onUpdateSettings = { transform -> latestSettings = transform(latestSettings) },
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
                    onTriggerSync = {},
                    onExportJson = { "{}" },
                    onExportCsv = { "" },
                    onWritePdf = { true },
                    onSignOut = {},
                    onDeleteAccountData = {}
                )
            }
        }

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

    @Test
    fun `ai mentor parses commands and falls back cleanly on 429 quota errors without burning real quota`() = runTest {
        val commandBackend = FakeShinobiAiBackend(
            contentHandler = { _, _, _ ->
                """{"actionType":"CREATE_TASK","title":"Chakra Physics Review","subjectName":"PHY201","category":"STUDY","dateIso":"2026-11-22","startTimeMinutes":900,"endTimeMinutes":990,"missionRank":"A","priority":"HIGH","location":"Leaf Hall"}"""
            }
        )
        val cmdService = AiMentorService(backend = commandBackend, retryDelayMs = 0L)
        val parsed = cmdService.classifyCommand("Schedule Chakra Physics Review at 3 PM", "2026-11-22")
        assertTrue(parsed is ParsedAiCommand.CreateTask)
        val created = parsed as ParsedAiCommand.CreateTask
        assertEquals("Chakra Physics Review", created.title)
        assertEquals(900, created.startTimeMinutes)

        val fallbackStreamBackend = FakeShinobiAiBackend(
            streamHandler = { modelName, _, _, _ ->
                flow {
                    if (modelName == AiMentorService.PRIMARY_MODEL) {
                        throw IllegalStateException("HTTP 429 RESOURCE_EXHAUSTED quota exceeded")
                    }
                    emit("Focus on ")
                    emit("Distributed Systems first!")
                }
            }
        )
        val streamService = AiMentorService(backend = fallbackStreamBackend, retryDelayMs = 0L)
        val chunks = streamService.streamMentorReply(
            personality = AiPersonality.MENTOR,
            history = emptyList(),
            userMessage = "What should I study?",
            scheduleContext = "Algorithms at 2 PM"
        ).toList()
        assertEquals(listOf("Focus on ", "Focus on Distributed Systems first!"), chunks)
        assertEquals(listOf(AiMentorService.PRIMARY_MODEL, AiMentorService.LITE_MODEL), fallbackStreamBackend.calledModels)

        val fallbackScanBackend = FakeShinobiAiBackend(
            multimodalHandler = { modelName, _, _, _ ->
                if (modelName == AiMentorService.PRIMARY_MODEL) {
                    throw IllegalStateException("429 Too Many Requests: Rate limit")
                }
                """{"events":[{"subject":"CS301","title":"Algorithms Lecture","dayOfWeek":"MONDAY","dateIso":"2026-11-23","startHour":14,"startMinute":30,"endHour":16,"endMinute":0,"location":"Room 4B","category":"CLASS","missionRank":"B","recurrence":"WEEKLY"}]}"""
            }
        )
        val scanService = AiMentorService(backend = fallbackScanBackend, retryDelayMs = 0L)
        val testBmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val events = scanService.scanTimetableImages(listOf(testBmp), "2026-11-22", accurateMode = true)
        assertEquals(1, events.size)
        assertEquals("Algorithms Lecture", events.first().title)
        assertEquals(14 * 60 + 30, events.first().startTimeMinutes)
        assertEquals(listOf(AiMentorService.PRIMARY_MODEL, AiMentorService.LITE_MODEL), fallbackScanBackend.calledModels)
    }

    @Test
    fun `ai mentor sanitizes multi turn history to strictly alternate user and model roles`() = runTest {
        var capturedSystem = ""
        var capturedHistory: List<Content> = emptyList()
        val captureBackend = FakeShinobiAiBackend(
            streamHandler = { _, systemInstruction, history, _ ->
                capturedSystem = systemInstruction
                capturedHistory = history
                flow { emit("Ready for training!") }
            }
        )
        val service = AiMentorService(backend = captureBackend, retryDelayMs = 0L)
        val noisyHistory = listOf(
            ChatMessageEntity(chatId = "c1", userId = "u1", role = "model", content = "Orphaned intro"),
            ChatMessageEntity(chatId = "c1", userId = "u1", role = "user", content = "Hi Sensei"),
            ChatMessageEntity(chatId = "c1", userId = "u1", role = "model", content = "Greetings, shinobi!"),
            ChatMessageEntity(chatId = "c1", userId = "u1", role = "user", content = "What next?")
        )
        val reply = service.streamMentorReply(
            personality = AiPersonality.STRICT,
            history = noisyHistory,
            userMessage = "What next?",
            scheduleContext = "OS Lab at 4 PM"
        ).toList()

        assertEquals(listOf("Ready for training!"), reply)
        assertTrue(capturedSystem.contains("Jonin squad captain"))
        assertEquals(2, capturedHistory.size)
        assertEquals("user", capturedHistory[0].role)
        assertEquals("model", capturedHistory[1].role)
    }

    @Test
    fun `ai mentor surfaces authentic error when both primary and fallback models fail`() = runTest {
        val exhaustedBackend = FakeShinobiAiBackend(
            streamHandler = { _, _, _, _ ->
                flow {
                    throw IllegalStateException("429 RESOURCE_EXHAUSTED all quota exhausted")
                }
            }
        )
        val service = AiMentorService(backend = exhaustedBackend, retryDelayMs = 0L)
        var thrown: Throwable? = null
        try {
            service.streamMentorReply(
                personality = AiPersonality.STRICT,
                history = emptyList(),
                userMessage = "Help",
                scheduleContext = "Empty"
            ).toList()
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
        assertTrue(thrown?.message.orEmpty().contains("429"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `ai viewmodel deduplicates identical daily suggestion calls to conserve quota`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val countingBackend = FakeShinobiAiBackend(
                contentHandler = { _, _, _ -> "Complete your Rank-B mission before sunset." }
            )
            val service = AiMentorService(backend = countingBackend, retryDelayMs = 0L)
            val repo = ShinobiRepository(context, db, remote = null)
            val vm = AiViewModel(repo, service, ShinobiRepository.LOCAL_GUEST_UID)
            val tasks = listOf(
                PlannerTaskEntity(
                    id = "t_algo",
                    userId = ShinobiRepository.LOCAL_GUEST_UID,
                    title = "Algorithms",
                    subjectName = "CS301",
                    dateIso = "2026-11-22",
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 10 * 60
                )
            )
            vm.refreshDailySuggestion(tasks, ShinobiTimeUtil.DEFAULT_TIMEZONE_ID)
            advanceUntilIdle()
            vm.refreshDailySuggestion(tasks, ShinobiTimeUtil.DEFAULT_TIMEZONE_ID)
            advanceUntilIdle()
            assertEquals(1, countingBackend.calledModels.size)
            assertEquals("Complete your Rank-B mission before sunset.", vm.dailySuggestion.value)

            vm.refreshDailySuggestion(tasks, ShinobiTimeUtil.DEFAULT_TIMEZONE_ID, force = true)
            advanceUntilIdle()
            assertEquals(2, countingBackend.calledModels.size)
        } finally {
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
        var latestSettings by mutableStateOf(
            UserSettingsEntity(
                userId = "s31",
                globalGlowEnabled = true,
                use24HourClock = false,
                quietHoursEnabled = true
            )
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
        // Verify Android 12 starts at 9:00 AM default, switches start time to PM & advances +5m -> 9:05 PM (21:05 -> 1265)
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
    fun `cold launch MainActivity on Android 12 traverses Auth Skip Onboarding and all main tabs cleanly`() {
        activityRule.waitForIdle()
        activityRule.onNodeWithTag("skip_sign_in_button").assertIsDisplayed().performClick()

        activityRule.waitUntil(timeoutMillis = 5_000L) {
            activityRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()
        }
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
