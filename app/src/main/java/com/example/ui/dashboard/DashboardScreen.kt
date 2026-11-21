package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.ShinobiTimeUtil
import com.example.core.common.RankProgression
import com.example.data.local.PlannerTaskEntity
import com.example.ui.components.ChakraProgressRing
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.ShurikenCheckbox
import com.example.ui.theme.*
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ALL_WIDGET_KEYS = listOf(
    "PROGRESS" to "Daily Progress & Rank",
    "NEXT_CLASS" to "Next Class Card",
    "TIMER" to "Quick Focus Timer",
    "AI_BRIEFING" to "AI Daily Briefing",
    "MISSIONS" to "Today's Missions",
    "WEEKLY" to "Weekly Progress Chart",
    "QUICK_ACTIONS" to "Quick Actions"
)

@Composable
fun DashboardScreen(
    uiState: PlannerUiState,
    focusState: FocusTimerUiState,
    dailySuggestion: String?,
    onRefreshSuggestion: () -> Unit,
    onToggleTask: (PlannerTaskEntity) -> Unit,
    onQuickFocusToggle: () -> Unit,
    onOpenScheduleAdd: () -> Unit,
    onOpenFocusDojo: () -> Unit,
    onOpenAiMentor: () -> Unit,
    onOpenScanner: () -> Unit,
    onSaveWidgetsOrder: (String) -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val settings = uiState.settings
    val profile = uiState.profile
    val nowZoned = remember(settings.timezoneId) { ShinobiTimeUtil.nowZoned(settings.timezoneId) }
    val todayDate = nowZoned.toLocalDate()
    val todayIso = todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todayTasks = remember(uiState.tasks, todayIso) {
        uiState.tasks.filter { it.dateIso == todayIso }.sortedBy { it.startTimeMinutes }
    }
    val completedToday = todayTasks.count { it.completed }
    val totalToday = todayTasks.size
    val dailyFraction = if (totalToday > 0) completedToday.toFloat() / totalToday.toFloat() else 0f

    val rankInfo = remember(profile?.xp) {
        RankProgression.computeLevelInfo(profile?.xp ?: 0)
    }

    val nowMinutes = nowZoned.hour * 60 + nowZoned.minute
    val nextClass = remember(uiState.tasks, todayIso, nowMinutes) {
        uiState.tasks
            .filter { !it.completed && (it.dateIso > todayIso || (it.dateIso == todayIso && it.endTimeMinutes >= nowMinutes)) }
            .sortedWith(compareBy<PlannerTaskEntity> { it.dateIso }.thenBy { it.startTimeMinutes })
            .firstOrNull()
    }

    val enabledWidgets = remember(settings.dashboardWidgetsCsv) {
        settings.dashboardWidgetsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    var showCustomizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.tasks.size) {
        if (settings.aiEnabled && dailySuggestion == null) {
            onRefreshSuggestion()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Greeting & Dual Calendar Header
        item {
            val greeting = when (nowZoned.hour) {
                in 5..11 -> "Good Morning"
                in 12..17 -> "Good Afternoon"
                else -> "Good Evening"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!profile?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.photoUrl,
                        contentDescription = "Scholar Avatar",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_shinobi_avatar),
                        contentDescription = "Scholar Avatar",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting, ${profile?.displayName?.ifBlank { "Shinobi" } ?: "Shinobi"}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ShinobiTimeUtil.formatDateHeader(todayDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = visuals.primaryAccent
                    )
                    val ethTimeSuffix = if (settings.showEthiopianTime) {
                        "  •  ${ShinobiTimeUtil.formatEthiopianClock(nowZoned.toLocalTime())}"
                    } else ""
                    Text(
                        text = "Time: ${ShinobiTimeUtil.formatTime(nowMinutes, settings.use24HourClock)}$ethTimeSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showCustomizeDialog = true },
                    modifier = Modifier.testTag("customize_dashboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Customize Dashboard Widgets",
                        tint = visuals.primaryAccent
                    )
                }
            }
        }

        if ("PROGRESS" in enabledWidgets) {
            item {
                HoloCard(accentColor = visuals.primaryAccent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChakraProgressRing(
                            progress = dailyFraction,
                            centerTitle = "${(dailyFraction * 100).toInt()}%",
                            centerSubtitle = "$completedToday/$totalToday Today",
                            accentColor = visuals.primaryAccent
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Lv.${rankInfo.level}  ${rankInfo.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldRankS,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { rankInfo.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = GoldRankS,
                                trackColor = visuals.primaryAccent.copy(alpha = 0.16f)
                            )
                            Text(
                                text = "${rankInfo.currentLevelXp} / ${rankInfo.nextLevelXpRequired} XP to next level",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = ChakraOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${profile?.currentStreak ?: 0}d Streak",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ChakraOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "Total XP",
                                        tint = JadeEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${profile?.xp ?: 0} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = JadeEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if ("QUICK_ACTIONS" in enabledWidgets) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.Add,
                            label = "New Mission",
                            tint = visuals.primaryAccent,
                            tag = "quick_new_mission",
                            onClick = onOpenScheduleAdd
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.Timer,
                            label = "Focus Dojo",
                            tint = ChakraOrange,
                            tag = "quick_focus_dojo",
                            onClick = onOpenFocusDojo
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.DocumentScanner,
                            label = "Scan Timetable",
                            tint = JadeEmerald,
                            tag = "quick_scan_timetable",
                            onClick = onOpenScanner
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.AutoAwesome,
                            label = "Ask Sensei AI",
                            tint = ArcaneViolet,
                            tag = "quick_ask_ai",
                            onClick = onOpenAiMentor
                        )
                    }
                }
            }
        }

        if ("NEXT_CLASS" in enabledWidgets) {
            item {
                HoloCard(accentColor = ChakraOrange, onClick = onOpenScheduleAdd) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Next Scheduled Class",
                            tint = ChakraOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEXT SCHEDULED MISSION",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChakraOrange,
                            modifier = Modifier.weight(1f)
                        )
                        nextClass?.let { RankBadgeChip(it.missionRank) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (nextClass == null) {
                        Text(
                            text = "No upcoming open missions. Tap to schedule a class or scan your university timetable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = nextClass.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${nextClass.subjectName} • ${nextClass.dateIso} • ${
                                ShinobiTimeUtil.formatTime(nextClass.startTimeMinutes, settings.use24HourClock)
                            } - ${
                                ShinobiTimeUtil.formatTime(nextClass.endTimeMinutes, settings.use24HourClock)
                            }" + if (nextClass.location.isNotBlank()) " • ${nextClass.location}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if ("TIMER" in enabledWidgets) {
            item {
                val mins = focusState.remainingSeconds / 60
                val secs = focusState.remainingSeconds % 60
                HoloCard(accentColor = JadeEmerald, onClick = onOpenFocusDojo) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCUS DOJO (${focusState.mode.label})",
                                style = MaterialTheme.typography.labelLarge,
                                color = JadeEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.ENGLISH, "%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Subject: ${focusState.selectedSubject} • ${focusState.completedPomodorosToday} sessions today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledIconButton(
                            onClick = onQuickFocusToggle,
                            modifier = Modifier.size(54.dp).testTag("dashboard_quick_timer_toggle"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = JadeEmerald,
                                contentColor = MidnightNavy
                            )
                        ) {
                            Icon(
                                imageVector = if (focusState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (focusState.isRunning) "Pause Focus Timer" else "Start Focus Timer"
                            )
                        }
                    }
                }
            }
        }

        if ("AI_BRIEFING" in enabledWidgets && settings.aiEnabled) {
            item {
                HoloCard(accentColor = ArcaneViolet, onClick = onOpenAiMentor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Shinobi Mentor Briefing",
                            tint = ArcaneViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SHINOBI SENSEI DAILY BRIEFING",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArcaneViolet,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onRefreshSuggestion, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Briefing", tint = ArcaneViolet)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dailySuggestion
                            ?: "Prioritize your highest-rank mission first and lock in one uninterrupted 25-minute Focus Dojo cycle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if ("MISSIONS" in enabledWidgets) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Missions ($totalToday)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onOpenScheduleAdd) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add / View All")
                    }
                }
            }

            if (todayTasks.isEmpty()) {
                item {
                    HoloCard(onClick = onOpenScheduleAdd) {
                        Text(
                            text = "No missions scheduled for today yet. Tap to add a class, assignment, or study mission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(todayTasks.take(6), key = { it.id }) { task ->
                    HoloCard(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        accentColor = if (task.completed) JadeEmerald else visuals.primaryAccent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShurikenCheckbox(
                                checked = task.completed,
                                onToggle = { onToggleTask(task) },
                                modifier = Modifier.testTag("dashboard_task_check_${task.id}")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.subjectName} • ${
                                        ShinobiTimeUtil.formatTime(task.startTimeMinutes, settings.use24HourClock)
                                    } (+${task.xpReward} XP)",
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

        if ("WEEKLY" in enabledWidgets) {
            item {
                WeeklyStudyBarCard(
                    todayDate = todayDate,
                    tasks = uiState.tasks,
                    accentColor = visuals.primaryAccent
                )
            }
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }

    if (showCustomizeDialog) {
        var selected by remember(settings.dashboardWidgetsCsv) { mutableStateOf(enabledWidgets) }
        AlertDialog(
            onDismissRequest = { showCustomizeDialog = false },
            title = { Text("Customize Dashboard Widgets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_WIDGET_KEYS.forEach { (key, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = key in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + key else selected - key
                                }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderedCsv = ALL_WIDGET_KEYS.map { it.first }.filter { it in selected }.joinToString(",")
                        onSaveWidgetsOrder(orderedCsv)
                        showCustomizeDialog = false
                    }
                ) {
                    Text("Save Layout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomizeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionChipPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(48.dp).testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun WeeklyStudyBarCard(
    todayDate: LocalDate,
    tasks: List<PlannerTaskEntity>,
    accentColor: androidx.compose.ui.graphics.Color
) {
    val startOfWeek = remember(todayDate) { todayDate.minusDays(6) }
    val counts = remember(tasks, startOfWeek) {
        (0..6).map { offset ->
            val d = startOfWeek.plusDays(offset.toLong())
            val iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val done = tasks.count { it.dateIso == iso && it.completed }
            val label = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
            label to done
        }
    }
    val maxCount = (counts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    HoloCard(accentColor = accentColor) {
        Text(
            text = "7-DAY MISSION COMPLETION",
            style = MaterialTheme.typography.labelLarge,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            val barSlot = size.width / counts.size
            val barWidth = (barSlot * 0.45f).coerceAtMost(32.dp.toPx())
            counts.forEachIndexed { idx, (_, count) ->
                val frac = count.toFloat() / maxCount.toFloat()
                val h = (size.height * 0.85f * frac).coerceAtLeast(6.dp.toPx())
                val x = idx * barSlot + (barSlot - barWidth) / 2f
                val y = size.height - h
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.20f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = if (count > 0) accentColor else accentColor.copy(alpha = 0.35f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            counts.forEach { (dayLabel, cnt) ->
                Text(
                    text = "$dayLabel($cnt)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
