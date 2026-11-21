package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.notifications.ShinobiNotificationManager
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.ShinobiDatabase
import com.example.data.local.SubtaskEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import androidx.compose.ui.test.performClick
import com.example.core.media.AudioPlaybackState
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
