package com.example.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.*
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.SubtaskEntity
import com.example.ui.components.GlassTier
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorModalSheet(
    existingTask: PlannerTaskEntity?,
    existingSubtasks: List<SubtaskEntity>,
    subjects: List<SubjectEntity>,
    defaultDateIso: String,
    use24HourClock: Boolean,
    onDismiss: () -> Unit,
    onSave: (PlannerTaskEntity, List<SubtaskEntity>) -> Unit
) {
    val taskId = remember(existingTask) { existingTask?.id ?: UUID.randomUUID().toString() }
    var title by remember { mutableStateOf(existingTask?.title.orEmpty()) }
    var notes by remember { mutableStateOf(existingTask?.notes.orEmpty()) }
    var subjectName by remember {
        mutableStateOf(existingTask?.subjectName ?: subjects.firstOrNull()?.name ?: "General")
    }
    var category by remember { mutableStateOf(TaskCategory.fromCode(existingTask?.category ?: "STUDY")) }
    var dateIso by remember { mutableStateOf(existingTask?.dateIso ?: defaultDateIso) }
    var startMinutes by remember { mutableIntStateOf(existingTask?.startTimeMinutes ?: 9 * 60) }
    var endMinutes by remember { mutableIntStateOf(existingTask?.endTimeMinutes ?: 10 * 60) }
    var priority by remember { mutableStateOf(TaskPriority.fromCode(existingTask?.priority ?: "MEDIUM")) }
    var missionRank by remember { mutableStateOf(MissionRank.fromCode(existingTask?.missionRank ?: "C")) }
    var recurrence by remember { mutableStateOf(RecurrenceRule.fromCode(existingTask?.recurrence ?: "NONE")) }
    var location by remember { mutableStateOf(existingTask?.location.orEmpty()) }
    var reminderCsv by remember { mutableStateOf(existingTask?.reminderOffsetsCsv ?: "10") }
    var selectedColorHex by remember { mutableStateOf(existingTask?.colorHex ?: category.defaultColorHex) }
    var use24HourLocal by remember(use24HourClock) { mutableStateOf(use24HourClock) }
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    var subtasks by remember { mutableStateOf(existingSubtasks) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (LocalShinobiVisuals.current.isDark) HoloSurfaceElevated else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTask == null) "CREATE SHINOBI MISSION" else "EDIT MISSION",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalShinobiVisuals.current.primaryAccent,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Task Editor")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Mission Title *") },
                placeholder = { Text("e.g. Algorithms Lecture / Lab Prep") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Tactical Notes") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("task_notes_input")
            )

            // Subject Selector Chips + custom input
            Text("Subject", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSubjects = (listOf("General") + subjects.map { it.name }).distinct()
                allSubjects.forEach { subj ->
                    FilterChip(
                        selected = subjectName.equals(subj, ignoreCase = true),
                        onClick = { subjectName = subj },
                        label = { Text(subj) }
                    )
                }
            }
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            category = cat
                            selectedColorHex = cat.defaultColorHex
                        },
                        label = { Text(cat.label) }
                    )
                }
            }

            // Date Quick Picks + manual ISO input
            Text("Date (yyyy-MM-dd)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = remember { LocalDate.now() }
                listOf("Today" to 0L, "Tomorrow" to 1L, "+3 Days" to 3L, "+7 Days" to 7L).forEach { (lbl, offset) ->
                    val iso = today.plusDays(offset).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    FilterChip(
                        selected = dateIso == iso,
                        onClick = { dateIso = iso },
                        label = { Text(lbl) }
                    )
                }
            }
            OutlinedTextField(
                value = dateIso,
                onValueChange = { dateIso = it },
                label = { Text("ISO Date") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task_date_input")
            )

            // Plan Time Setting with AM/PM & Custom TimePicker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plan Time (Custom AM / PM)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !use24HourLocal,
                        onClick = { use24HourLocal = false },
                        label = { Text("AM / PM") },
                        modifier = Modifier.testTag("task_time_format_am_pm")
                    )
                    FilterChip(
                        selected = use24HourLocal,
                        onClick = { use24HourLocal = true },
                        label = { Text("24h") },
                        modifier = Modifier.testTag("task_time_format_24h")
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeStepperCard(
                    label = "Start Time",
                    tagPrefix = "start",
                    minutes = startMinutes,
                    use24Hour = use24HourLocal,
                    onChange = { next ->
                        startMinutes = next.coerceIn(0, 1424)
                        if (endMinutes <= startMinutes) {
                            endMinutes = (startMinutes + 30).coerceAtMost(1439)
                        }
                    },
                    onOpenPicker = { pickerTarget = "START" },
                    modifier = Modifier.fillMaxWidth()
                )
                TimeStepperCard(
                    label = "End Time",
                    tagPrefix = "end",
                    minutes = endMinutes,
                    use24Hour = use24HourLocal,
                    onChange = { next ->
                        endMinutes = next.coerceIn(startMinutes + 15, 1439)
                    },
                    onOpenPicker = { pickerTarget = "END" },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mission Rank (D-S) & Priority
            Text("Mission Rank & XP Reward", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MissionRank.entries.forEach { rank ->
                    FilterChip(
                        selected = missionRank == rank,
                        onClick = { missionRank = rank },
                        label = { Text("${rank.label} (+${rank.baseXp} XP)") }
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) }
                    )
                }
            }

            Text("Repeat Rule", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceRule.entries.forEach { rule ->
                    FilterChip(
                        selected = recurrence == rule,
                        onClick = { recurrence = rule },
                        label = { Text(rule.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Room") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reminderCsv,
                    onValueChange = { reminderCsv = it },
                    label = { Text("Alerts (mins CSV)") },
                    placeholder = { Text("10,30") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Color Tag dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tag Color:", style = MaterialTheme.typography.labelMedium)
                listOf("#55D9FF", "#FF8C32", "#A879FF", "#45E6A0", "#FFCF4A", "#FF4B6E").forEach { hex ->
                    val parsed = Color(android.graphics.Color.parseColor(hex))
                    val selected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MidnightNavy)
                            )
                        }
                    }
                }
            }

            // Subtasks builder
            Text("Subtasks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            subtasks.forEach { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sub.completed,
                        onCheckedChange = { checked ->
                            subtasks = subtasks.map { if (it.id == sub.id) it.copy(completed = checked) else it }
                        }
                    )
                    Text(sub.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { subtasks = subtasks.filterNot { it.id == sub.id } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove subtask", tint = CrimsonAlert)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskTitle,
                    onValueChange = { newSubtaskTitle = it },
                    label = { Text("Add subtask step") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            subtasks = subtasks + SubtaskEntity(
                                taskId = taskId,
                                title = newSubtaskTitle.trim(),
                                sortOrder = subtasks.size
                            )
                            newSubtaskTitle = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Subtask")
                }
            }

            Button(
                onClick = {
                    val safeDate = runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull()
                        ?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: defaultDateIso
                    val entity = PlannerTaskEntity(
                        id = taskId,
                        userId = existingTask?.userId.orEmpty(),
                        title = title.trim().ifBlank { "Shinobi Study Mission" },
                        notes = notes.trim(),
                        subjectName = subjectName.trim().ifBlank { "General" },
                        category = category.code,
                        dateIso = safeDate,
                        startTimeMinutes = startMinutes,
                        endTimeMinutes = endMinutes,
                        priority = priority.code,
                        missionRank = missionRank.code,
                        recurrence = recurrence.code,
                        location = location.trim(),
                        reminderOffsetsCsv = reminderCsv.trim(),
                        xpReward = missionRank.baseXp,
                        colorHex = selectedColorHex,
                        completed = existingTask?.completed ?: false,
                        completedAtIso = existingTask?.completedAtIso.orEmpty(),
                        sortOrder = existingTask?.sortOrder ?: 0
                    )
                    onSave(entity, subtasks)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_task_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalShinobiVisuals.current.primaryAccent,
                    contentColor = MidnightNavy
                )
            ) {
                Text(if (existingTask == null) "Deploy Mission" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }

    pickerTarget?.let { target ->
        val initialMinutes = if (target == "START") startMinutes else endMinutes
        val pickerState = rememberTimePickerState(
            initialHour = (initialMinutes / 60).coerceIn(0, 23),
            initialMinute = (initialMinutes % 60).coerceIn(0, 59),
            is24Hour = use24HourLocal
        )
        AlertDialog(
            onDismissRequest = { pickerTarget = null },
            title = {
                Text(
                    text = if (target == "START") "Set Plan Start Time" else "Set Plan End Time",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedMinutes = pickerState.hour * 60 + pickerState.minute
                        if (target == "START") {
                            startMinutes = pickedMinutes.coerceIn(0, 1424)
                            if (endMinutes <= startMinutes) {
                                endMinutes = (startMinutes + 30).coerceAtMost(1439)
                            }
                        } else {
                            endMinutes = pickedMinutes.coerceIn(startMinutes + 15, 1439)
                        }
                        pickerTarget = null
                    },
                    modifier = Modifier.testTag("confirm_time_picker_button")
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TimeStepperCard(
    label: String,
    tagPrefix: String,
    minutes: Int,
    use24Hour: Boolean,
    onChange: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    val style = ThemeDesignSystem.glassStyle(
        accentColor = visuals.primaryAccent,
        tier = GlassTier.SUBTLE,
        glowActive = true,
        cornerRadius = ThemeDesignSystem.CompactCornerRadius
    )
    val clamped = minutes.coerceIn(0, 1439)
    val isPm = clamped >= 12 * 60
    Surface(
        modifier = modifier.ShinobiGlassModifier(style = style, showSpecularEdge = false),
        shape = style.shape,
        color = Color.Transparent,
        border = null,
        tonalElevation = style.tonalElevation
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = ShinobiTimeUtil.formatTime(clamped, use24Hour),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("${tagPrefix}_time_display")
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isPm,
                        onClick = {
                            val next = if (isPm) clamped - 12 * 60 else clamped
                            onChange(next)
                        },
                        label = { Text("AM") },
                        modifier = Modifier.testTag("${tagPrefix}_time_am")
                    )
                    FilterChip(
                        selected = isPm,
                        onClick = {
                            val next = if (!isPm) clamped + 12 * 60 else clamped
                            onChange(next)
                        },
                        label = { Text("PM") },
                        modifier = Modifier.testTag("${tagPrefix}_time_pm")
                    )
                    OutlinedButton(
                        onClick = onOpenPicker,
                        modifier = Modifier.height(48.dp).testTag("pick_${tagPrefix}_time_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Pick")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onChange(clamped - 60) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("${tagPrefix}_hour_minus"),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("-1h") }
                OutlinedButton(
                    onClick = { onChange(clamped + 60) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("${tagPrefix}_hour_plus"),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+1h") }
                OutlinedButton(
                    onClick = { onChange(clamped - 5) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("${tagPrefix}_min_minus"),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("-5m") }
                OutlinedButton(
                    onClick = { onChange(clamped + 5) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("${tagPrefix}_min_plus"),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+5m") }
            }
        }
    }
}
