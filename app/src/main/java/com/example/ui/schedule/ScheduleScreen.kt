package com.example.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.ShinobiTimeUtil
import com.example.core.common.TaskCategory
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.SubtaskEntity
import com.example.data.repository.TaskConflict
import com.example.data.repository.TimeSlot
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.ShurikenCheckbox
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.JadeEmerald
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.viewmodel.PlannerUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class ScheduleViewMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    AGENDA("Agenda")
}

@Composable
fun ScheduleScreen(
    uiState: PlannerUiState,
    openCreateInitially: Boolean,
    onConsumedCreateFlag: () -> Unit,
    detectConflicts: (String) -> List<TaskConflict>,
    findFreeTimeSlots: (String) -> List<TimeSlot>,
    onSaveTask: (PlannerTaskEntity, List<SubtaskEntity>) -> Unit,
    onToggleTask: (PlannerTaskEntity) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteTask: (PlannerTaskEntity) -> Unit,
    onDuplicateTask: (PlannerTaskEntity) -> Unit,
    onMoveTaskUp: (PlannerTaskEntity, List<PlannerTaskEntity>) -> Unit,
    onSaveSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenScanner: () -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val settings = uiState.settings
    val nowZoned = remember(settings.timezoneId) { ShinobiTimeUtil.nowZoned(settings.timezoneId) }
    val today = nowZoned.toLocalDate()
    val nowMinutes = nowZoned.hour * 60 + nowZoned.minute

    var selectedDate by remember { mutableStateOf(today) }
    val selectedIso = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    var viewMode by remember { mutableStateOf(ScheduleViewMode.DAILY) }
    var editingTask by remember { mutableStateOf<PlannerTaskEntity?>(null) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var showSubjectsDialog by remember { mutableStateOf(false) }
    var showFreeSlots by remember { mutableStateOf(false) }

    LaunchedEffect(openCreateInitially) {
        if (openCreateInitially) {
            editingTask = null
            showTaskSheet = true
            onConsumedCreateFlag()
        }
    }

    val dayTasks = remember(uiState.tasks, selectedIso) {
        uiState.tasks.filter { it.dateIso == selectedIso }
            .sortedWith(compareBy<PlannerTaskEntity> { it.sortOrder }.thenBy { it.startTimeMinutes })
    }
    val conflicts = remember(dayTasks) { detectConflicts(selectedIso) }
    val freeSlots = remember(dayTasks) { findFreeTimeSlots(selectedIso) }
    val displayedTasks = remember(uiState.tasks, selectedDate, viewMode, dayTasks, selectedIso) {
        when (viewMode) {
            ScheduleViewMode.DAILY, ScheduleViewMode.MONTHLY -> dayTasks
            ScheduleViewMode.WEEKLY -> {
                val weekStart = selectedDate.with(DayOfWeek.MONDAY)
                val weekEnd = weekStart.plusDays(6)
                val startIso = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endIso = weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
                uiState.tasks.filter { it.dateIso in startIso..endIso }
                    .sortedWith(compareBy<PlannerTaskEntity> { it.dateIso }.thenBy { it.startTimeMinutes })
            }
            ScheduleViewMode.AGENDA -> {
                uiState.tasks.filter { it.dateIso >= selectedIso }
                    .sortedWith(compareBy<PlannerTaskEntity> { it.dateIso }.thenBy { it.startTimeMinutes })
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("schedule_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Futuristic Header Toolbar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHINOBI SCHEDULE",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ShinobiTimeUtil.formatDateHeader(selectedDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = ChakraCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showSubjectsDialog = true },
                            modifier = Modifier.size(48.dp).testTag("manage_subjects_button")
                        ) {
                            Icon(Icons.Filled.Book, contentDescription = "Manage Subjects", tint = NarutoOrange)
                        }
                        IconButton(
                            onClick = onOpenScanner,
                            modifier = Modifier.size(48.dp).testTag("schedule_scan_button")
                        ) {
                            Icon(Icons.Filled.DocumentScanner, contentDescription = "Scan Timetable", tint = ChakraCyan)
                        }
                    }
                }
            }

            // Segmented View Switcher (Daily / Weekly / Monthly / Agenda / Free Time)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScheduleViewMode.entries) { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("schedule_mode_${mode.name.lowercase()}")
                        )
                    }
                    item {
                        FilterChip(
                            selected = showFreeSlots,
                            onClick = { showFreeSlots = !showFreeSlots },
                            label = { Text("Free Time (${freeSlots.size})") },
                            modifier = Modifier.testTag("toggle_free_slots_chip")
                        )
                    }
                }
            }

            // Glass Date Navigator Strip or Monthly Calendar Grid
            item {
                when (viewMode) {
                    ScheduleViewMode.MONTHLY -> MonthlyCalendarGrid(
                        selectedDate = selectedDate,
                        tasks = uiState.tasks,
                        accentColor = ChakraCyan,
                        onSelectDate = { selectedDate = it }
                    )
                    else -> WeeklyStripSelector(
                        selectedDate = selectedDate,
                        tasks = uiState.tasks,
                        accentColor = ChakraCyan,
                        onSelectDate = { selectedDate = it }
                    )
                }
            }

            // Holographic Current-Time Timeline Indicator when viewing Today
            if (viewMode == ScheduleViewMode.DAILY && selectedDate == today) {
                item {
                    CurrentTimeTimelineBar(
                        nowLabel = ShinobiTimeUtil.formatTime(nowMinutes, settings.use24HourClock),
                        ethLabel = if (settings.showEthiopianTime) {
                            ShinobiTimeUtil.formatEthiopianClock(nowZoned.toLocalTime())
                        } else null
                    )
                }
            }

            // Conflict Detector Banner
            if (conflicts.isNotEmpty()) {
                item {
                    HoloCard(accentColor = CrimsonAlert, glowActive = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Conflict", tint = CrimsonAlert)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Schedule Conflict Detected (${conflicts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        conflicts.take(2).forEach { c ->
                            Text(
                                text = "• ${c.first.title} overlaps with ${c.second.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Free-time Slots Card
            if (showFreeSlots) {
                item {
                    HoloCard(accentColor = ChakraGreen) {
                        Text(
                            text = "FREE STUDY SLOTS ($selectedIso)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChakraGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (freeSlots.isEmpty()) {
                            Text("No free blocks >= 30 mins remaining between 08:00 and 22:00.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(freeSlots) { slot ->
                                    AssistChip(
                                        onClick = {
                                            editingTask = PlannerTaskEntity(
                                                userId = "",
                                                title = "Focus Block",
                                                dateIso = selectedIso,
                                                startTimeMinutes = slot.startMinutes,
                                                endTimeMinutes = (slot.startMinutes + 60).coerceAtMost(slot.endMinutes)
                                            )
                                            showTaskSheet = true
                                        },
                                        label = {
                                            Text(
                                                "${ShinobiTimeUtil.formatTime(slot.startMinutes, settings.use24HourClock)} - ${
                                                    ShinobiTimeUtil.formatTime(slot.endMinutes, settings.use24HourClock)
                                                }"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Task list depending on Daily / Weekly / Agenda / Monthly
            if (displayedTasks.isEmpty()) {
                item {
                    HoloCard(
                        accentColor = ChakraCyan,
                        glassTier = GlassTier.SUBTLE,
                        onClick = {
                            editingTask = null
                            showTaskSheet = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = ChakraCyan)
                            Text(
                                text = "No missions scheduled for this view. Tap + to create a mission or scan your timetable.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedTasks, key = { it.id }) { task ->
                    val taskSubtasks = uiState.subtasks.filter { it.taskId == task.id }
                    SwipeableTaskRow(
                        task = task,
                        subtasks = taskSubtasks,
                        showDate = viewMode == ScheduleViewMode.WEEKLY || viewMode == ScheduleViewMode.AGENDA,
                        use24HourClock = settings.use24HourClock,
                        canMoveUp = viewMode == ScheduleViewMode.DAILY && dayTasks.indexOfFirst { it.id == task.id } > 0,
                        onToggleComplete = { onToggleTask(task) },
                        onToggleSubtask = onToggleSubtask,
                        onEdit = {
                            editingTask = task
                            showTaskSheet = true
                        },
                        onDuplicate = { onDuplicateTask(task) },
                        onMoveUp = { onMoveTaskUp(task, dayTasks) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(92.dp)) }
        }

        FloatingActionButton(
            onClick = {
                editingTask = null
                showTaskSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 92.dp)
                .testTag("schedule_add_fab"),
            containerColor = visuals.primaryAccent,
            contentColor = MidnightNavy
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create New Mission")
        }
    }

    if (showTaskSheet) {
        val editingSubtasks = editingTask?.let { t -> uiState.subtasks.filter { it.taskId == t.id } }.orEmpty()
        TaskEditorModalSheet(
            existingTask = editingTask,
            existingSubtasks = editingSubtasks,
            subjects = uiState.subjects,
            defaultDateIso = selectedIso,
            use24HourClock = settings.use24HourClock,
            onDismiss = { showTaskSheet = false },
            onSave = { task, subs ->
                onSaveTask(task, subs)
                showTaskSheet = false
            }
        )
    }

    if (showSubjectsDialog) {
        SubjectManagerDialog(
            subjects = uiState.subjects,
            onDismiss = { showSubjectsDialog = false },
            onSaveSubject = onSaveSubject,
            onDeleteSubject = onDeleteSubject
        )
    }
}

@Composable
private fun CurrentTimeTimelineBar(
    nowLabel: String,
    ethLabel: String?
) {
    val badgeStyle = ThemeDesignSystem.glassStyle(
        accentColor = NarutoOrange,
        tier = GlassTier.SUBTLE,
        glowActive = true,
        cornerRadius = 10.dp,
        tintColor = NarutoOrange
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.ShinobiGlassModifier(style = badgeStyle, showSpecularEdge = false),
            shape = badgeStyle.shape,
            color = Color.Transparent
        ) {
            Text(
                text = if (ethLabel != null) "NOW $nowLabel • $ethLabel" else "NOW $nowLabel",
                style = MaterialTheme.typography.labelSmall,
                color = NarutoOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NarutoOrange)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(NarutoOrange, ChakraCyan.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun WeeklyStripSelector(
    selectedDate: LocalDate,
    tasks: List<PlannerTaskEntity>,
    accentColor: Color,
    onSelectDate: (LocalDate) -> Unit
) {
    val weekStart = remember(selectedDate) { selectedDate.with(DayOfWeek.MONDAY) }
    val days = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    HoloCard(
        accentColor = accentColor,
        glassTier = GlassTier.SUBTLE,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onSelectDate(selectedDate.minusWeeks(1)) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Week")
            }
            days.forEach { day ->
                val iso = day.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isSelected = day == selectedDate
                val hasTasks = tasks.any { it.dateIso == iso && !it.completed }
                val selectedStyle = ThemeDesignSystem.glassStyle(
                    accentColor = accentColor,
                    tier = GlassTier.STANDARD,
                    glowActive = true,
                    cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                    tintColor = accentColor
                )
                Surface(
                    onClick = { onSelectDate(day) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .then(
                            if (isSelected) Modifier.ShinobiGlassModifier(style = selectedStyle, showSpecularEdge = false)
                            else Modifier
                        ),
                    shape = selectedStyle.shape,
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasTasks) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NarutoOrange)
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = { onSelectDate(selectedDate.plusWeeks(1)) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Week")
            }
        }
    }
}

@Composable
private fun MonthlyCalendarGrid(
    selectedDate: LocalDate,
    tasks: List<PlannerTaskEntity>,
    accentColor: Color,
    onSelectDate: (LocalDate) -> Unit
) {
    val ym = YearMonth.from(selectedDate)
    val firstDay = ym.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - 1)
    val totalDays = ym.lengthOfMonth()

    HoloCard(accentColor = accentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSelectDate(selectedDate.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onSelectDate(selectedDate.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val cells = (0 until leadingBlanks).map { null } + (1..totalDays).map { ym.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val d = week.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (d == selectedDate) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable(enabled = d != null) { d?.let(onSelectDate) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (d != null) {
                            val iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val count = tasks.count { it.dateIso == iso && !it.completed }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = d.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (d == selectedDate) FontWeight.Bold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(NarutoOrange)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categoryAccentColor(categoryCode: String): Color = when (categoryCode.uppercase()) {
    TaskCategory.CLASS.code -> ChakraCyan
    TaskCategory.STUDY.code -> HolographicPurple
    TaskCategory.ASSIGNMENT.code -> NarutoOrange
    TaskCategory.EXAM.code, TaskCategory.DEADLINE.code -> CrimsonAlert
    TaskCategory.PERSONAL.code -> ChakraGreen
    else -> ChakraCyan
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: PlannerTaskEntity,
    subtasks: List<SubtaskEntity>,
    showDate: Boolean,
    use24HourClock: Boolean,
    canMoveUp: Boolean,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val tagColor = remember(task.colorHex, task.category) {
        runCatching { Color(android.graphics.Color.parseColor(task.colorHex)) }
            .getOrElse { categoryAccentColor(task.category) }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isCompleting = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            val bg = if (isCompleting) JadeEmerald.copy(alpha = 0.28f) else CrimsonAlert.copy(alpha = 0.28f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isCompleting) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (isCompleting) Icons.Filled.Check else Icons.Filled.Delete,
                    contentDescription = if (isCompleting) "Complete" else "Delete",
                    tint = if (isCompleting) JadeEmerald else CrimsonAlert
                )
            }
        }
    ) {
        HoloCard(
            accentColor = if (task.completed) JadeEmerald else tagColor,
            glassTier = GlassTier.STANDARD,
            onClick = onEdit,
            contentPadding = PaddingValues(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(if (task.completed) JadeEmerald else tagColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                ShurikenCheckbox(
                    checked = task.completed,
                    onToggle = onToggleComplete,
                    accentColor = tagColor,
                    modifier = Modifier.testTag("task_complete_${task.id}")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val timeSpan = "${ShinobiTimeUtil.formatTime(task.startTimeMinutes, use24HourClock)} - ${
                        ShinobiTimeUtil.formatTime(task.endTimeMinutes, use24HourClock)
                    }"
                    Text(
                        text = buildString {
                            if (showDate) append("${task.dateIso} • ")
                            append(timeSpan)
                            append(" • ${task.subjectName}")
                            if (task.location.isNotBlank()) append(" • ${task.location}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RankBadgeChip(task.missionRank)
            }

            if (subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSubtask(sub) }
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Checkbox(checked = sub.completed, onCheckedChange = { onToggleSubtask(sub) })
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (canMoveUp) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Mission Up", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate Mission", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Mission", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Mission", tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SubjectManagerDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSaveSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("University Subjects") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${s.name} ${if (s.code.isNotBlank()) "(${s.code})" else ""}", fontWeight = FontWeight.SemiBold)
                            if (s.instructor.isNotBlank() || s.room.isNotBlank()) {
                                Text("${s.instructor} ${s.room}".trim(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { onDeleteSubject(s.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Subject", tint = CrimsonAlert)
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSaveSubject(
                            SubjectEntity(
                                userId = "",
                                name = name.trim(),
                                code = code.trim(),
                                instructor = instructor.trim(),
                                room = room.trim()
                            )
                        )
                        name = ""; code = ""; instructor = ""; room = ""
                    }
                }
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
