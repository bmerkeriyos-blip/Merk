package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.RankProgression
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.PlannerTaskEntity
import com.example.ui.components.ChakraProgressRing
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.ShurikenCheckbox
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
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
    onSaveWidgetsOrder: (String) -> Unit,
    onOpenScheduleView: () -> Unit = onOpenScheduleAdd,
    onOpenSettings: () -> Unit = {},
    onForceRefreshSuggestion: () -> Unit = onRefreshSuggestion
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
        // Futuristic Shinobi Command Center Header
        item {
            val greeting = when (nowZoned.hour) {
                in 5..11 -> "GOOD MORNING"
                in 12..17 -> "GOOD AFTERNOON"
                else -> "GOOD EVENING"
            }
            val userName = profile?.displayName?.ifBlank { "Shinobi" } ?: "Shinobi"
            val ethTimeSuffix = if (settings.showEthiopianTime) {
                " • ${ShinobiTimeUtil.formatEthiopianClock(nowZoned.toLocalTime())}"
            } else ""

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(NarutoOrange, ChakraCyan, HolographicPurple, NarutoOrange)
                                )
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = profile?.photoUrl
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Shinobi Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_shinobi_avatar),
                                contentDescription = "Shinobi Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting, ${userName.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Your training continues today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onForceRefreshSuggestion,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = "Refresh Briefing Alerts",
                            tint = ChakraCyan
                        )
                    }
                    IconButton(
                        onClick = { showCustomizeDialog = true },
                        modifier = Modifier.size(48.dp).testTag("customize_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Customize Dashboard Widgets",
                            tint = visuals.primaryAccent
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Quick Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact Glass Date + Dual Clock Pill
                val clockPillStyle = ThemeDesignSystem.glassStyle(
                    accentColor = ChakraCyan,
                    tier = GlassTier.SUBTLE,
                    cornerRadius = ThemeDesignSystem.CompactCornerRadius
                )
                Surface(
                    modifier = Modifier.ShinobiGlassModifier(style = clockPillStyle, showSpecularEdge = false),
                    shape = clockPillStyle.shape,
                    color = Color.Transparent,
                    tonalElevation = clockPillStyle.tonalElevation
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = ChakraCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ShinobiTimeUtil.formatDateHeader(todayDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = ChakraCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = NarutoOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Time: ${ShinobiTimeUtil.formatTime(nowMinutes, settings.use24HourClock)}$ethTimeSuffix",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Holographic Ninja Rank Panel
        if ("PROGRESS" in enabledWidgets) {
            item {
                HoloCard(
                    accentColor = NarutoOrange,
                    glassTier = GlassTier.HERO,
                    glowActive = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChakraProgressRing(
                            progress = dailyFraction,
                            centerTitle = "${(dailyFraction * 100).toInt()}%",
                            centerSubtitle = "$completedToday/$totalToday Today",
                            size = 104.dp,
                            strokeWidth = 9.dp,
                            accentColor = NarutoOrange
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rankInfo.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NarutoOrange,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                val levelBadgeStyle = ThemeDesignSystem.glassStyle(
                                    accentColor = GoldRankS,
                                    tier = GlassTier.SUBTLE,
                                    glowActive = true,
                                    cornerRadius = 8.dp,
                                    tintColor = GoldRankS
                                )
                                Surface(
                                    modifier = Modifier.ShinobiGlassModifier(style = levelBadgeStyle, showSpecularEdge = false),
                                    shape = levelBadgeStyle.shape,
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = "LEVEL ${rankInfo.level.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldRankS,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            XpProgressBar(
                                progress = rankInfo.progressFraction,
                                startColor = NarutoOrange,
                                endColor = GoldRankS,
                                height = 8.dp
                            )

                            Text(
                                text = "${rankInfo.currentLevelXp} / ${rankInfo.nextLevelXpRequired} XP to next rank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatPill(
                                    icon = Icons.Filled.LocalFireDepartment,
                                    text = "${profile?.currentStreak ?: 0}d Streak",
                                    tint = NarutoOrange
                                )
                                StatPill(
                                    icon = Icons.Filled.Bolt,
                                    text = "${profile?.xp ?: 0} XP",
                                    tint = ChakraGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Glass Quick Actions
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
                            tint = NarutoOrange,
                            tag = "quick_new_mission",
                            onClick = onOpenScheduleAdd
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.Timer,
                            label = "Focus Dojo",
                            tint = ChakraCyan,
                            tag = "quick_focus_dojo",
                            onClick = onOpenFocusDojo
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.DocumentScanner,
                            label = "AI Scan",
                            tint = ChakraGreen,
                            tag = "quick_scan_timetable",
                            onClick = onOpenScanner
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.CalendarMonth,
                            label = "Schedule",
                            tint = ChakraCyan,
                            tag = "quick_schedule",
                            onClick = onOpenScheduleView
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.AutoAwesome,
                            label = "Sensei AI",
                            tint = HolographicPurple,
                            tag = "quick_ask_ai",
                            onClick = onOpenAiMentor
                        )
                    }
                }
            }
        }

        // Next Scheduled Mission with Holographic Timeline Indicator
        if ("NEXT_CLASS" in enabledWidgets) {
            item {
                HoloCard(
                    accentColor = ChakraCyan,
                    glassTier = GlassTier.STANDARD,
                    onClick = onOpenScheduleAdd
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ChakraCyan.copy(alpha = 0.16f))
                                .border(1.dp, ChakraCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Next Scheduled Mission",
                                tint = ChakraCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEXT SCHEDULED MISSION",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChakraCyan,
                                fontWeight = FontWeight.Bold
                            )
                            if (nextClass != null) {
                                val countdownText = buildCountdownText(todayIso, nowMinutes, nextClass)
                                Text(
                                    text = countdownText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NarutoOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        nextClass?.let { RankBadgeChip(it.missionRank) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (nextClass == null) {
                        Text(
                            text = "No upcoming open missions. Tap to schedule a class or scan your university timetable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Holographic vertical timeline bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(ChakraCyan, NarutoOrange)))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nextClass.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                val durationMins = (nextClass.endTimeMinutes - nextClass.startTimeMinutes).coerceAtLeast(0)
                                Text(
                                    text = "${nextClass.subjectName} • ${durationMins}m • ${
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
            }
        }

        // Focus Dojo Widget with Cyan Chakra Ring
        if ("TIMER" in enabledWidgets) {
            item {
                val mins = focusState.remainingSeconds / 60
                val secs = focusState.remainingSeconds % 60
                val ringProgress = if (focusState.totalSeconds > 0) {
                    1f - (focusState.remainingSeconds.toFloat() / focusState.totalSeconds.toFloat())
                } else 0f

                HoloCard(
                    accentColor = ChakraCyan,
                    glassTier = GlassTier.HERO,
                    glowActive = focusState.isRunning,
                    onClick = onOpenFocusDojo
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChakraProgressRing(
                            progress = ringProgress,
                            centerTitle = String.format(Locale.ENGLISH, "%02d:%02d", mins, secs),
                            centerSubtitle = if (focusState.isRunning) "ACTIVE" else "READY",
                            size = 88.dp,
                            strokeWidth = 7.dp,
                            accentColor = ChakraCyan,
                            isAnimating = focusState.isRunning
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCUS DOJO",
                                style = MaterialTheme.typography.labelLarge,
                                color = ChakraCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = focusState.mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${focusState.selectedSubject} • ${focusState.completedPomodorosToday} sessions today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledIconButton(
                            onClick = onQuickFocusToggle,
                            modifier = Modifier.size(54.dp).testTag("dashboard_quick_timer_toggle"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (focusState.isRunning) NarutoOrange else ChakraCyan,
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

        // AI Daily Briefing with Holographic Purple Glow
        if ("AI_BRIEFING" in enabledWidgets && settings.aiEnabled) {
            item {
                HoloCard(
                    accentColor = HolographicPurple,
                    glassTier = GlassTier.STANDARD,
                    onClick = onOpenAiMentor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Shinobi Mentor Briefing",
                            tint = HolographicPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI DAILY BRIEFING",
                            style = MaterialTheme.typography.labelLarge,
                            color = HolographicPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onForceRefreshSuggestion, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Briefing", tint = HolographicPurple)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dailySuggestion
                            ?: "Prioritize your highest-rank mission first and lock in one uninterrupted 25-minute Focus Dojo cycle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Today's Missions Section
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
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
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
                    HoloCard(
                        glassTier = GlassTier.SUBTLE,
                        onClick = onOpenScheduleAdd
                    ) {
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        accentColor = if (task.completed) ChakraGreen else NarutoOrange,
                        glassTier = GlassTier.STANDARD
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShurikenCheckbox(
                                checked = task.completed,
                                onToggle = { onToggleTask(task) },
                                accentColor = if (task.completed) ChakraGreen else NarutoOrange,
                                modifier = Modifier.testTag("dashboard_task_check_${task.id}")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
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

        // Weekly Neon Study Progress Chart
        if ("WEEKLY" in enabledWidgets) {
            item {
                WeeklyStudyBarCard(
                    todayDate = todayDate,
                    tasks = uiState.tasks,
                    accentColor = ChakraCyan
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

private fun buildCountdownText(todayIso: String, nowMinutes: Int, task: PlannerTaskEntity): String {
    if (task.dateIso > todayIso) return "Scheduled ${task.dateIso}"
    val diffStart = task.startTimeMinutes - nowMinutes
    return when {
        diffStart > 60 -> "Starts in ${diffStart / 60}h ${diffStart % 60}m"
        diffStart > 0 -> "Starts in ${diffStart}m"
        else -> {
            val rem = (task.endTimeMinutes - nowMinutes).coerceAtLeast(1)
            "Active now • ${rem}m remaining"
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = tint,
        tier = GlassTier.SUBTLE,
        cornerRadius = 10.dp,
        tintColor = tint
    )
    Surface(
        modifier = Modifier.ShinobiGlassModifier(style = style, showSpecularEdge = false),
        shape = style.shape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionChipPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = tint,
        tier = GlassTier.STANDARD,
        glowActive = true,
        cornerRadius = ThemeDesignSystem.PillCornerRadius
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .ShinobiGlassModifier(style = style)
            .testTag(tag),
        shape = style.shape,
        color = Color.Transparent,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeeklyStudyBarCard(
    todayDate: LocalDate,
    tasks: List<PlannerTaskEntity>,
    accentColor: Color
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

    HoloCard(accentColor = accentColor, glassTier = GlassTier.STANDARD) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-DAY MISSION COMPLETION",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${counts.sumOf { it.second }} cleared",
                style = MaterialTheme.typography.labelSmall,
                color = NarutoOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp)
        ) {
            val barSlot = size.width / counts.size
            val barWidth = (barSlot * 0.44f).coerceAtMost(28.dp.toPx())
            counts.forEachIndexed { idx, (_, count) ->
                val frac = count.toFloat() / maxCount.toFloat()
                val h = (size.height * 0.85f * frac).coerceAtLeast(7.dp.toPx())
                val x = idx * barSlot + (barSlot - barWidth) / 2f
                val y = size.height - h
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.14f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (count > 0) {
                            listOf(NarutoOrange, accentColor)
                        } else {
                            listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.12f))
                        }
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
