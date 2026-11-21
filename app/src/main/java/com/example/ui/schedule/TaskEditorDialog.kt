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

    var subtasks by remember { mutableStateOf(existingSubtasks) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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

            // Start & End Time quick steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeStepperCard(
                    label = "Start Time",
                    minutes = startMinutes,
                    use24Hour = use24HourClock,
                    onChange = { next ->
                        startMinutes = next.coerceIn(0, 23 * 60 + 30)
                        if (endMinutes <= startMinutes) {
                            endMinutes = (startMinutes + 60).coerceAtMost(1439)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                TimeStepperCard(
                    label = "End Time",
                    minutes = endMinutes,
                    use24Hour = use24HourClock,
                    onChange = { next ->
                        endMinutes = next.coerceIn(startMinutes + 15, 1439)
                    },
                    modifier = Modifier.weight(1f)
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
                listOf("#00E5FF", "#FF6D00", "#9D4EDD", "#00E676", "#FFD600", "#FF3D5A").forEach { hex ->
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
}

@Composable
private fun TimeStepperCard(
    label: String,
    minutes: Int,
    use24Hour: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ShinobiTimeUtil.formatTime(minutes, use24Hour),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onChange(minutes - 30) }) { Text("-30m") }
                TextButton(onClick = { onChange(minutes + 30) }) { Text("+30m") }
            }
        }
    }
}
