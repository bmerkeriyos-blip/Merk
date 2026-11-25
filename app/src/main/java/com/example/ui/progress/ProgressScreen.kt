package com.example.ui.progress

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.MissionRank
import com.example.core.common.RankProgression
import com.example.core.common.ShinobiTimeUtil
import com.example.core.common.TaskCategory
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.SubtaskEntity
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.NarutoHeroArtBanner
import com.example.ui.components.RankBadgeChip
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.SurfaceStyle
import com.example.ui.theme.ThemeDesignSystem
import com.example.ui.theme.VermilionTorii
import com.example.ui.theme.WashiGold
import com.example.viewmodel.PlannerUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val RANK_LADDER = listOf(
    1 to "Academy Student",
    5 to "Genin Scholar",
    10 to "Chunin Tactician",
    16 to "Tokubetsu Jonin",
    25 to "Jonin Master",
    35 to "Grand Kage"
)

private enum class StatsWindow(val label: String) {
    WEEKLY("Weekly (7d)"),
    MONTHLY("Monthly (4w)")
}

@Composable
fun ProgressScreen(
    uiState: PlannerUiState,
    onSaveTask: (PlannerTaskEntity, List<SubtaskEntity>) -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val profile = uiState.profile
    val rankInfo = remember(profile?.xp) { RankProgression.computeLevelInfo(profile?.xp ?: 0) }
    val today = remember(uiState.settings.timezoneId) {
        ShinobiTimeUtil.nowZoned(uiState.settings.timezoneId).toLocalDate()
    }
    val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    var statsWindow by remember { mutableStateOf(StatsWindow.WEEKLY) }

    val upcomingExams = remember(uiState.tasks, todayIso) {
        uiState.tasks.filter {
            !it.completed &&
                (it.category.equals("EXAM", true) || it.category.equals("DEADLINE", true)) &&
                it.dateIso >= todayIso
        }.sortedBy { it.dateIso }
    }

    val totalFocusMins = remember(uiState.sessions) { uiState.sessions.sumOf { it.durationMinutes } }
    val totalTasks = uiState.tasks.size
    val completedTasks = uiState.tasks.count { it.completed }
    val missedPastTasks = remember(uiState.tasks, todayIso) {
        uiState.tasks.count { !it.completed && it.dateIso < todayIso }
    }
    val productivityScore = remember(totalTasks, completedTasks, totalFocusMins, profile?.currentStreak) {
        val taskRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks.toFloat()) * 60f else 30f
        val focusPart = (totalFocusMins.toFloat() / 300f).coerceIn(0f, 1f) * 25f
        val streakPart = ((profile?.currentStreak ?: 0).toFloat() / 7f).coerceIn(0f, 1f) * 15f
        (taskRate + focusPart + streakPart).toInt().coerceIn(0, 100)
    }

    // Weekly (7 days) vs Monthly (4 weeks) mission completion data
    val chartBuckets = remember(uiState.tasks, today, statsWindow) {
        if (statsWindow == StatsWindow.WEEKLY) {
            (6 downTo 0).map { offset ->
                val d = today.minusDays(offset.toLong())
                val iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val count = uiState.tasks.count { it.dateIso == iso && it.completed }
                d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) to count
            }
        } else {
            (3 downTo 0).map { weekOffset ->
                val endDay = today.minusDays(weekOffset.toLong() * 7L)
                val startDay = endDay.minusDays(6)
                val startIso = startDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endIso = endDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val count = uiState.tasks.count { it.completed && it.dateIso in startIso..endIso }
                val label = "W${4 - weekOffset}"
                label to count
            }
        }
    }

    val subjectMinutes = remember(uiState.sessions, uiState.tasks) {
        val map = mutableMapOf<String, Int>()
        uiState.sessions.forEach { s ->
            map[s.subjectName] = (map[s.subjectName] ?: 0) + s.durationMinutes
        }
        uiState.tasks.filter { it.completed }.forEach { t ->
            val dur = (t.endTimeMinutes - t.startTimeMinutes).coerceAtLeast(30)
            map[t.subjectName] = (map[t.subjectName] ?: 0) + dur
        }
        map.entries.sortedByDescending { it.value }.take(6)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "SHINOBI PATH & PROGRESS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            NarutoHeroArtBanner(
                drawableRes = if (statsWindow == StatsWindow.WEEKLY) R.drawable.img_naruto_sage_mode else R.drawable.img_itachi_uchiha,
                badgeLabel = "SENJUTSU MASTERY",
                title = "${rankInfo.title.uppercase()} • LEVEL ${rankInfo.level}",
                subtitle = "${profile?.xp ?: 0} Total XP • Best Streak: ${profile?.longestStreak ?: 0} days",
                accentColor = WashiGold,
                height = 128.dp
            )
        }

        // Dark Japanese RPG Rank Progression Card
        item {
            HoloCard(
                accentColor = WashiGold,
                glassTier = GlassTier.HERO,
                surfaceStyle = SurfaceStyle.DARK_JAPANESE,
                glowActive = true
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldRankS.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = GoldRankS, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Lv.${rankInfo.level}  ${rankInfo.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldRankS,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${rankInfo.currentLevelXp} / ${rankInfo.nextLevelXpRequired} XP to next rank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                XpProgressBar(
                    progress = rankInfo.progressFraction,
                    startColor = VermilionTorii,
                    endColor = GoldRankS,
                    height = 9.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                RANK_LADDER.forEach { (reqLv, title) ->
                    val reached = rankInfo.level >= reqLv
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lv.$reqLv  $title",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (reached) ChakraGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = if (reached) "UNLOCKED" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reached) ChakraGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Weekly & Monthly Training Report + Charts
        item {
            HoloCard(accentColor = ChakraCyan, surfaceStyle = SurfaceStyle.DARK_JAPANESE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timeline, contentDescription = null, tint = ChakraCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRODUCTIVITY STATS",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChakraCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsWindow.entries.forEach { window ->
                        FilterChip(
                            selected = statsWindow == window,
                            onClick = { statsWindow = window },
                            label = { Text(window.label) },
                            modifier = Modifier.testTag("stats_window_${window.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniBlock("Productivity", "$productivityScore/100", ChakraCyan, Modifier.weight(1f))
                    StatMiniBlock("Completed", "$completedTasks", ChakraGreen, Modifier.weight(1f))
                    StatMiniBlock("Overdue", "$missedPastTasks", CrimsonAlert, Modifier.weight(1f))
                    StatMiniBlock("Focus Hrs", String.format(Locale.ENGLISH, "%.1fh", totalFocusMins / 60f), NarutoOrange, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))
                val maxBucket = (chartBuckets.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (statsWindow == StatsWindow.WEEKLY) "Daily Missions Cleared (Last 7 Days)" else "Weekly Missions Cleared (Last 4 Weeks)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${chartBuckets.sumOf { it.second }} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = WashiGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                ) {
                    val slotW = size.width / chartBuckets.size
                    val barW = (slotW * 0.45f).coerceAtMost(32.dp.toPx())
                    chartBuckets.forEachIndexed { idx, (_, count) ->
                        val frac = count.toFloat() / maxBucket.toFloat()
                        val h = (size.height * 0.85f * frac).coerceAtLeast(8.dp.toPx())
                        val x = idx * slotW + (slotW - barW) / 2f
                        val y = size.height - h
                        drawRoundRect(
                            color = ChakraCyan.copy(alpha = 0.14f),
                            topLeft = Offset(x, 0f),
                            size = Size(barW, size.height),
                            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (count > 0) listOf(WashiGold, VermilionTorii) else listOf(ChakraCyan.copy(alpha = 0.25f), ChakraCyan.copy(alpha = 0.12f))
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    chartBuckets.forEach { (label, count) ->
                        Text(
                            text = "$label ($count)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (subjectMinutes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Study Balance by Subject (minutes)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val maxMins = (subjectMinutes.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((subjectMinutes.size * 28).dp)
                    ) {
                        val rowH = size.height / subjectMinutes.size
                        subjectMinutes.forEachIndexed { idx, (_, mins) ->
                            val frac = mins.toFloat() / maxMins.toFloat()
                            val barW = size.width * 0.92f * frac
                            val y = idx * rowH + rowH * 0.22f
                            drawRoundRect(
                                color = ChakraCyan.copy(alpha = 0.14f),
                                topLeft = Offset(0f, y),
                                size = Size(size.width * 0.92f, rowH * 0.54f),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = if (idx % 2 == 0) {
                                        listOf(ChakraCyan, HolographicPurple)
                                    } else {
                                        listOf(NarutoOrange, GoldRankS)
                                    }
                                ),
                                topLeft = Offset(0f, y),
                                size = Size(barW.coerceAtLeast(12f), rowH * 0.54f),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                            )
                        }
                    }
                    subjectMinutes.forEach { (subj, mins) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subj, style = MaterialTheme.typography.labelSmall)
                            Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = ChakraCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Exam Countdowns & Revision Plan Generator
        item {
            HoloCard(accentColor = CrimsonAlert, surfaceStyle = SurfaceStyle.DARK_JAPANESE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXAM COUNTDOWN & REVISION GENERATOR",
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (upcomingExams.isEmpty()) {
                    Text(
                        text = "No upcoming Exam or Deadline missions scheduled. Add one in Schedule to track countdown days and generate 3-day revision plans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    upcomingExams.take(4).forEach { exam ->
                        val examDate = runCatching { LocalDate.parse(exam.dateIso) }.getOrNull() ?: today
                        val daysLeft = ChronoUnit.DAYS.between(today, examDate).coerceAtLeast(0)
                        val examAccent = if (daysLeft <= 3) CrimsonAlert else GoldRankS
                        val examStyle = ThemeDesignSystem.glassStyle(
                            accentColor = examAccent,
                            tier = GlassTier.SUBTLE,
                            glowActive = daysLeft <= 3,
                            cornerRadius = ThemeDesignSystem.CompactCornerRadius
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .ShinobiGlassModifier(style = examStyle, showSpecularEdge = false),
                            shape = examStyle.shape,
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = examStyle.tonalElevation
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(exam.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${exam.subjectName} • ${exam.dateIso} • $daysLeft days remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = examAccent
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        for (offset in 1..3) {
                                            val revDate = examDate.minusDays(offset.toLong())
                                            val targetDate = if (revDate.isBefore(today)) today else revDate
                                            onSaveTask(
                                                PlannerTaskEntity(
                                                    userId = exam.userId,
                                                    title = "Revision #$offset: ${exam.title}",
                                                    subjectName = exam.subjectName,
                                                    category = TaskCategory.STUDY.code,
                                                    dateIso = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                                    startTimeMinutes = (17 * 60) + (offset * 30),
                                                    endTimeMinutes = (18 * 60) + (offset * 30),
                                                    priority = "HIGH",
                                                    missionRank = MissionRank.B.code,
                                                    xpReward = MissionRank.B.baseXp
                                                ),
                                                emptyList()
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("generate_revision_button_${exam.id}")
                                ) {
                                    Icon(Icons.Filled.EventNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("3-Day Plan")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Achievements Grid
        item {
            HoloCard(accentColor = WashiGold, surfaceStyle = SurfaceStyle.DARK_JAPANESE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldRankS)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACHIEVEMENTS (${uiState.achievements.count { it.unlocked }}/${uiState.achievements.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldRankS,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                uiState.achievements.forEach { ach ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (ach.unlocked) GoldRankS.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (ach.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (ach.unlocked) GoldRankS else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ach.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(ach.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (ach.unlocked) "+${ach.xpBonus} XP" else "Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ach.unlocked) ChakraGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Completion History
        if (uiState.history.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = visuals.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recent Mission Completion Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            items(uiState.history.take(8), key = { it.id }) { h ->
                HoloCard(
                    accentColor = ChakraGreen,
                    glassTier = GlassTier.SUBTLE,
                    surfaceStyle = SurfaceStyle.DARK_JAPANESE,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(h.taskTitle, fontWeight = FontWeight.Medium)
                            Text(h.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RankBadgeChip(h.missionRank)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+${h.xpEarned} XP", color = ChakraGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@Composable
private fun StatMiniBlock(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = color,
        tier = GlassTier.SUBTLE,
        surfaceStyle = SurfaceStyle.DARK_JAPANESE,
        cornerRadius = ThemeDesignSystem.CompactCornerRadius,
        tintColor = color
    )
    Surface(
        modifier = modifier.ShinobiGlassModifier(style = style, showSpecularEdge = false),
        shape = style.shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
