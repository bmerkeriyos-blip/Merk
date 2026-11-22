package com.example.ui.missions

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.MissionRank
import com.example.core.common.ShinobiTimeUtil
import com.example.core.media.AudioPlaybackState
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.StudyNoteEntity
import com.example.ui.components.ChakraProgressRing
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.ShurikenCheckbox
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.JadeEmerald
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.viewmodel.FocusModeType
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import java.util.Locale

private enum class MissionsTab(val label: String) {
    FOCUS_DOJO("Focus Dojo"),
    MISSION_BOARD("Mission Board"),
    STUDY_NOTES("Study Notes")
}

@Composable
fun MissionsScreen(
    uiState: PlannerUiState,
    focusState: FocusTimerUiState,
    audioState: AudioPlaybackState,
    initialTabIndex: Int = 0,
    onSelectFocusMode: (FocusModeType) -> Unit,
    onSetCustomMinutes: (Int) -> Unit,
    onSelectFocusSubject: (String) -> Unit,
    onToggleFocusStartPause: () -> Unit,
    onStopAndSaveFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onToggleDistractionFree: () -> Unit,
    onToggleKeepAwake: () -> Unit,
    onToggleMusic: () -> Unit,
    onNextTrack: () -> Unit,
    onToggleTask: (PlannerTaskEntity) -> Unit,
    onSaveNote: (StudyNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    var selectedTab by remember(initialTabIndex) {
        mutableStateOf(MissionsTab.entries.getOrElse(initialTabIndex) { MissionsTab.FOCUS_DOJO })
    }

    // Keep Screen Awake while Focus Dojo timer runs & toggle enabled
    val context = LocalContext.current
    DisposableEffect(focusState.keepScreenAwake, focusState.isRunning) {
        val window = (context as? Activity)?.window
        val shouldKeep = focusState.keepScreenAwake && focusState.isRunning
        if (shouldKeep) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("missions_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (visuals.isDark) 0.78f else 0.94f),
            contentColor = visuals.primaryAccent
        ) {
            MissionsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("missions_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            MissionsTab.FOCUS_DOJO -> FocusDojoTab(
                uiState = uiState,
                focusState = focusState,
                audioState = audioState,
                onSelectFocusMode = onSelectFocusMode,
                onSetCustomMinutes = onSetCustomMinutes,
                onSelectFocusSubject = onSelectFocusSubject,
                onToggleFocusStartPause = onToggleFocusStartPause,
                onStopAndSaveFocus = onStopAndSaveFocus,
                onResetFocus = onResetFocus,
                onToggleDistractionFree = onToggleDistractionFree,
                onToggleKeepAwake = onToggleKeepAwake,
                onToggleMusic = onToggleMusic,
                onNextTrack = onNextTrack
            )
            MissionsTab.MISSION_BOARD -> MissionBoardTab(
                tasks = uiState.tasks,
                use24HourClock = uiState.settings.use24HourClock,
                onToggleTask = onToggleTask
            )
            MissionsTab.STUDY_NOTES -> StudyNotesTab(
                uiState = uiState,
                onSaveNote = onSaveNote,
                onDeleteNote = onDeleteNote
            )
        }
    }
}

@Composable
private fun FocusDojoTab(
    uiState: PlannerUiState,
    focusState: FocusTimerUiState,
    audioState: AudioPlaybackState,
    onSelectFocusMode: (FocusModeType) -> Unit,
    onSetCustomMinutes: (Int) -> Unit,
    onSelectFocusSubject: (String) -> Unit,
    onToggleFocusStartPause: () -> Unit,
    onStopAndSaveFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onToggleDistractionFree: () -> Unit,
    onToggleKeepAwake: () -> Unit,
    onToggleMusic: () -> Unit,
    onNextTrack: () -> Unit
) {
    val totalFocusMins = uiState.sessions.sumOf { it.durationMinutes }
    val goalFraction = (totalFocusMins.toFloat() / focusState.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!focusState.distractionFreeMode) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FocusModeType.entries) { mode ->
                        FilterChip(
                            selected = focusState.mode == mode,
                            onClick = { onSelectFocusMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("focus_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (focusState.mode == FocusModeType.CUSTOM && !focusState.distractionFreeMode) {
            item {
                HoloCard(accentColor = ChakraCyan) {
                    Text(
                        text = "Custom Duration: ${focusState.customMinutes} minutes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = focusState.customMinutes.toFloat(),
                        onValueChange = { onSetCustomMinutes(it.toInt()) },
                        valueRange = 5f..180f
                    )
                }
            }
        }

        // Major Futuristic Cyan Chakra Energy Focus Dojo Timer Card
        item {
            val displaySec = if (focusState.mode == FocusModeType.STOPWATCH) {
                focusState.elapsedStopwatchSeconds
            } else {
                focusState.remainingSeconds
            }
            val progress = when {
                focusState.mode == FocusModeType.STOPWATCH -> ((displaySec % 60) / 60f)
                focusState.totalSeconds > 0 -> 1f - (focusState.remainingSeconds.toFloat() / focusState.totalSeconds.toFloat())
                else -> 0f
            }
            val timeStr = String.format(Locale.ENGLISH, "%02d:%02d", displaySec / 60, displaySec % 60)

            HoloCard(
                accentColor = ChakraCyan,
                glassTier = GlassTier.HERO,
                glowActive = focusState.isRunning || focusState.lastCompletedMessage != null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FOCUS DOJO • ${focusState.mode.label.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChakraCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${focusState.completedPomodorosToday} sessions completed today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleDistractionFree,
                        modifier = Modifier.size(48.dp).testTag("toggle_distraction_free_button")
                    ) {
                        Icon(
                            imageVector = if (focusState.distractionFreeMode) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Toggle Distraction Free Mode",
                            tint = ChakraCyan
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChakraProgressRing(
                        progress = progress,
                        centerTitle = timeStr,
                        centerSubtitle = focusState.selectedSubject,
                        size = 216.dp,
                        strokeWidth = 14.dp,
                        accentColor = ChakraCyan,
                        isAnimating = focusState.isRunning,
                        showCompletionPulse = focusState.lastCompletedMessage != null
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = onToggleFocusStartPause,
                        modifier = Modifier.height(50.dp).testTag("focus_start_pause_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (focusState.isRunning) NarutoOrange else ChakraCyan,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Icon(if (focusState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (focusState.isRunning) "Pause" else "Start Training", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStopAndSaveFocus,
                        modifier = Modifier.height(50.dp).testTag("focus_complete_save_button")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log XP")
                    }
                    IconButton(onClick = onResetFocus, modifier = Modifier.size(50.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset Timer")
                    }
                }
            }
        }

        // Integrated Media3 Frosted Glass Mini-Player
        item {
            HoloCard(
                accentColor = HolographicPurple,
                glowActive = audioState.isPlaying
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HolographicPurple.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Focus Music", tint = HolographicPurple)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audioState.currentTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = audioState.currentArtist,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleMusic, modifier = Modifier.size(48.dp).testTag("dojo_music_play_pause")) {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play or Pause Focus Music",
                            tint = HolographicPurple
                        )
                    }
                    IconButton(onClick = onNextTrack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next Track")
                    }
                }
            }
        }

        if (!focusState.distractionFreeMode) {
            item {
                HoloCard(accentColor = NarutoOrange) {
                    Text("Subject Tag", style = MaterialTheme.typography.labelLarge, color = NarutoOrange, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val subjectOptions = (listOf("General Study") + uiState.subjects.map { it.name }).distinct()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjectOptions) { subj ->
                            FilterChip(
                                selected = focusState.selectedSubject == subj,
                                onClick = { onSelectFocusSubject(subj) },
                                label = { Text(subj) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Screen Awake While Training", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = focusState.keepScreenAwake, onCheckedChange = { onToggleKeepAwake() })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Daily Study Goal: $totalFocusMins / ${focusState.dailyGoalMinutes} min", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    XpProgressBar(
                        progress = goalFraction,
                        startColor = ChakraCyan,
                        endColor = ChakraGreen,
                        height = 8.dp
                    )
                }
            }

            if (uiState.sessions.isNotEmpty()) {
                item {
                    Text("Recent Focus Logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(uiState.sessions.take(5), key = { it.id }) { s ->
                    HoloCard(
                        accentColor = ChakraGreen,
                        glassTier = GlassTier.SUBTLE,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${s.subjectName} (${s.mode})", fontWeight = FontWeight.SemiBold)
                                Text("${s.durationMinutes} min focus session", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("+${s.xpEarned} XP", color = ChakraGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionBoardTab(
    tasks: List<PlannerTaskEntity>,
    use24HourClock: Boolean,
    onToggleTask: (PlannerTaskEntity) -> Unit
) {
    var selectedRankFilter by remember { mutableStateOf<String?>(null) }
    var showCompleted by remember { mutableStateOf(false) }

    val filtered = remember(tasks, selectedRankFilter, showCompleted) {
        tasks.filter { task ->
            (selectedRankFilter == null || task.missionRank.equals(selectedRankFilter, ignoreCase = true)) &&
                (showCompleted || !task.completed)
        }.sortedByDescending { MissionRank.fromCode(it.missionRank).baseXp }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedRankFilter == null,
                        onClick = { selectedRankFilter = null },
                        label = { Text("All Ranks") }
                    )
                }
                items(MissionRank.entries) { r ->
                    FilterChip(
                        selected = selectedRankFilter == r.code,
                        onClick = { selectedRankFilter = r.code },
                        label = { Text("${r.label} (+${r.baseXp})") }
                    )
                }
                item {
                    FilterChip(
                        selected = showCompleted,
                        onClick = { showCompleted = !showCompleted },
                        label = { Text("Show Cleared") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                HoloCard(glassTier = GlassTier.SUBTLE) {
                    Text(
                        text = "No active missions match this rank filter. Add tasks in Schedule to populate the Mission Board.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { task ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.StartToEnd) {
                            onToggleTask(task)
                            false
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromEndToStart = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(ChakraGreen.copy(alpha = 0.25f))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Toggle Complete", tint = ChakraGreen)
                        }
                    }
                ) {
                    HoloCard(
                        accentColor = if (task.completed) JadeEmerald else NarutoOrange,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                if (task.completed) ChakraGreen else NarutoOrange,
                                                ChakraCyan
                                            )
                                        )
                                    )
                            )
                            ShurikenCheckbox(checked = task.completed, onToggle = { onToggleTask(task) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val startLabel = ShinobiTimeUtil.formatTime(task.startTimeMinutes, use24HourClock)
                                Text(
                                    "${task.subjectName} • ${task.dateIso} $startLabel • +${task.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RankBadgeChip(task.missionRank)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@Composable
private fun StudyNotesTab(
    uiState: PlannerUiState,
    onSaveNote: (StudyNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNoteEntity?>(null) }

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteSubject by remember { mutableStateOf("General") }
    var noteAttachmentUri by remember { mutableStateOf("") }

    val attachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            noteAttachmentUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = {
                    editingNote = null
                    noteTitle = ""; noteContent = ""; noteSubject = "General"; noteAttachmentUri = ""
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("new_study_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NarutoOrange,
                    contentColor = MidnightNavy
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Shinobi Study Scroll / Note", fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.notes.isEmpty()) {
            item {
                HoloCard(glassTier = GlassTier.SUBTLE) {
                    Text(
                        text = "Save formula sheets, lecture summaries, and PDF/file references linked to your subjects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.notes, key = { it.id }) { note ->
                HoloCard(
                    accentColor = ChakraCyan,
                    onClick = {
                        editingNote = note
                        noteTitle = note.title
                        noteContent = note.content
                        noteSubject = note.subjectName
                        noteAttachmentUri = note.attachmentUri
                        showEditor = true
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(note.subjectName, style = MaterialTheme.typography.labelSmall, color = ChakraCyan)
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Note", tint = CrimsonAlert)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(note.content, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    if (note.attachmentUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Attachment linked", style = MaterialTheme.typography.labelSmall, color = ChakraGreen)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(76.dp)) }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingNote == null) "New Study Note" else "Edit Study Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteSubject,
                        onValueChange = { noteSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Study Content") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { attachLauncher.launch(arrayOf("application/pdf", "image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (noteAttachmentUri.isBlank()) "Attach PDF / Image" else "Attachment Linked")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(
                            StudyNoteEntity(
                                id = editingNote?.id ?: java.util.UUID.randomUUID().toString(),
                                userId = editingNote?.userId.orEmpty(),
                                title = noteTitle.trim().ifBlank { "Untitled Note" },
                                content = noteContent.trim(),
                                subjectName = noteSubject.trim().ifBlank { "General" },
                                attachmentUri = noteAttachmentUri
                            )
                        )
                        showEditor = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Cancel") }
            }
        )
    }
}
