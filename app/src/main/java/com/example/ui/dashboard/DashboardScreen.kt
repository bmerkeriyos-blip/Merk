package com.example.ui.dashboard

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.core.common.NarutoVisualCatalog
import com.example.core.common.RankProgression
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.PlannerTaskEntity
import com.example.ui.components.ChakraProgressRing
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.NarutoHeroArtBanner
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.ShurikenCheckbox
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.SurfaceStyle
import com.example.ui.theme.ThemeDesignSystem
import com.example.viewmodel.FocusTimerUiState
import com.example.viewmodel.PlannerUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ALL_WIDGET_KEYS = listOf(
    "PROGRESS" to "Daily Progress & Rank",
    "NEXT_CLASS" to "Next Mission Card",
    "TIMER" to "Quick Focus Dojo",
    "MISSIONS" to "Today's Missions",
    "WEEKLY" to "7-Day Completion Chart",
    "QUICK_ACTIONS" to "Quick Navigation"
)

@Composable
fun DashboardScreen(
    uiState: PlannerUiState,
    focusState: FocusTimerUiState,
    onToggleTask: (PlannerTaskEntity) -> Unit,
    onQuickFocusToggle: () -> Unit,
    onOpenScheduleAdd: () -> Unit,
    onOpenFocusDojo: () -> Unit,
    onOpenProgress: () -> Unit = {},
    onSaveWidgetsOrder: (String) -> Unit,
    onOpenScheduleView: () -> Unit = onOpenScheduleAdd,
    onOpenSettings: () -> Unit = {}
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
        settings.dashboardWidgetsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "AI_BRIEFING" }
            .toSet()
    }
    var showCustomizeDialog by remember { mutableStateOf(false) }

    val heroArtworkResName = remember(uiState.wallpapers, visuals.preset) {
        val homeSpecific = uiState.wallpapers.firstOrNull {
            it.screenTarget.equals("HOME", ignoreCase = true) && it.builtInResName.isNotBlank()
        }?.builtInResName
        homeSpecific ?: visuals.preset.defaultResName
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Personalized Shinobi Command Header
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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    listOf(visuals.primaryAccent, visuals.secondaryAccent, HolographicPurple, visuals.primaryAccent)
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
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_shinobi_avatar),
                                contentDescription = "Shinobi Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
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
                            text = "${visuals.preset.label} • ${profile?.ninjaTitle ?: "Academy Student"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                // Dual Clock Frosted Pill
                val clockPillStyle = ThemeDesignSystem.glassStyle(
                    accentColor = visuals.secondaryAccent,
                    tier = GlassTier.SUBTLE,
                    surfaceStyle = SurfaceStyle.GLASSMORPHISM,
                    cornerRadius = ThemeDesignSystem.CompactCornerRadius
                )
                Surface(
                    modifier = Modifier.ShinobiGlassModifier(style = clockPillStyle, showSpecularEdge = false),
                    shape = clockPillStyle.shape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = clockPillStyle.tonalElevation
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = visuals.secondaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ShinobiTimeUtil.formatDateHeader(todayDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = visuals.secondaryAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = visuals.primaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${ShinobiTimeUtil.formatTime(nowMinutes, settings.use24HourClock)}$ethTimeSuffix",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Cinematic Naruto Character Hero Banner
        item {
            val openCount = todayTasks.count { !it.completed }
            val heroTitle = when {
                openCount == 0 && totalToday > 0 -> "All Daily Missions Cleared"
                openCount > 0 -> "$openCount Active Missions Today"
                else -> "Hidden Leaf Study Command"
            }
            val heroSubtitle = if (nextClass != null) {
                "Next: ${nextClass.title} at ${ShinobiTimeUtil.formatTime(nextClass.startTimeMinutes, settings.use24HourClock)}"
            } else {
                "Tap to schedule classes, assignments, or enter Focus Dojo."
            }
            NarutoHeroArtBanner(
                drawableRes = NarutoVisualCatalog.resolveDrawable(heroArtworkResName),
                badgeLabel = visuals.preset.label.uppercase(),
                title = heroTitle,
                subtitle = heroSubtitle,
                accentColor = visuals.primaryAccent,
                height = 132.dp,
                onClick = onOpenScheduleView
            )
        }

        // Anime Cyber Ninja Rank Panel
        if ("PROGRESS" in enabledWidgets) {
            item {
                HoloCard(
                    accentColor = visuals.primaryAccent,
                    glassTier = GlassTier.HERO,
                    surfaceStyle = SurfaceStyle.ANIME_CYBER,
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
                            accentColor = visuals.primaryAccent
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
                                    color = visuals.primaryAccent,
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
                                    color = Color.Transparent,
                                    contentColor = GoldRankS
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
                                startColor = visuals.primaryAccent,
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
                                    tint = visuals.primaryAccent
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

        // Quick Offline Navigation Chips
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
                            tint = visuals.secondaryAccent,
                            tag = "quick_focus_dojo",
                            onClick = onOpenFocusDojo
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
                            icon = Icons.Filled.Insights,
                            label = "Progress",
                            tint = ChakraGreen,
                            tag = "quick_progress",
                            onClick = onOpenProgress
                        )
                    }
                    item {
                        ActionChipPill(
                            icon = Icons.Filled.Settings,
                            label = "Themes",
                            tint = GoldRankS,
                            tag = "quick_settings_themes",
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }

        // Next Scheduled Mission
        if ("NEXT_CLASS" in enabledWidgets) {
            item {
                HoloCard(
                    accentColor = visuals.secondaryAccent,
                    glassTier = GlassTier.STANDARD,
                    surfaceStyle = SurfaceStyle.GLASSMORPHISM,
                    onClick = onOpenScheduleView
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(visuals.secondaryAccent.copy(alpha = 0.16f))
                                .border(1.dp, visuals.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Next Scheduled Mission",
                                tint = visuals.secondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEXT SCHEDULED MISSION",
                                style = MaterialTheme.typography.labelLarge,
                                color = visuals.secondaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                            if (nextClass != null) {
                                Text(
                                    text = buildCountdownText(todayIso, nowMinutes, nextClass),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = visuals.primaryAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        nextClass?.let { RankBadgeChip(it.missionRank) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (nextClass == null) {
                        Text(
                            text = "No upcoming open missions. Tap to add a class, assignment, or study mission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(visuals.secondaryAccent, visuals.primaryAccent)))
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

        // Quick Focus Dojo Card
        if ("TIMER" in enabledWidgets) {
            item {
                val mins = focusState.remainingSeconds / 60
                val secs = focusState.remainingSeconds % 60
                val ringProgress = if (focusState.totalSeconds > 0) {
                    1f - (focusState.remainingSeconds.toFloat() / focusState.totalSeconds.toFloat())
                } else 0f

                HoloCard(
                    accentColor = visuals.secondaryAccent,
                    glassTier = GlassTier.HERO,
                    surfaceStyle = SurfaceStyle.GLASSMORPHISM,
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
                            accentColor = visuals.secondaryAccent,
                            isAnimating = focusState.isRunning
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCUS DOJO",
                                style = MaterialTheme.typography.labelLarge,
                                color = visuals.secondaryAccent,
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
                                containerColor = if (focusState.isRunning) visuals.primaryAccent else visuals.secondaryAccent,
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
                        accentColor = if (task.completed) ChakraGreen else visuals.primaryAccent,
                        glassTier = GlassTier.STANDARD
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShurikenCheckbox(
                                checked = task.completed,
                                onToggle = { onToggleTask(task) },
                                accentColor = if (task.completed) ChakraGreen else visuals.primaryAccent,
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

        // Weekly 7-Day Study Progress Chart
        if ("WEEKLY" in enabledWidgets) {
            item {
                WeeklyStudyBarCard(
                    todayDate = todayDate,
                    tasks = uiState.tasks,
                    accentColor = visuals.secondaryAccent,
                    primaryColor = visuals.primaryAccent
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
        color = Color.Transparent,
        contentColor = tint
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
        contentColor = MaterialTheme.colorScheme.onSurface,
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
    accentColor: Color,
    primaryColor: Color
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
                color = primaryColor,
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
                            listOf(primaryColor, accentColor)
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
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            counts.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
