package com.example.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.EthiopicCalendarUtil
import com.example.core.common.MissionRank
import com.example.core.common.RankProgression
import com.example.core.common.TaskCategory
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.SubtaskEntity
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.theme.*
import com.example.viewmodel.PlannerUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val RANK_LADDER = listOf(
    1 to "Academy Initiate",
    5 to "Shadow Scholar",
    10 to "Scroll Tactician",
    16 to "Chakra Vanguard",
    25 to "Cyber Jonin",
    35 to "Grand Cyber Kage"
)

@Composable
fun ProgressScreen(
    uiState: PlannerUiState,
    onSaveTask: (PlannerTaskEntity, List<SubtaskEntity>) -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val profile = uiState.profile
    val rankInfo = remember(profile?.xp) { RankProgression.computeLevelInfo(profile?.xp ?: 0) }
    val today = remember(uiState.settings.timezoneId) {
        EthiopicCalendarUtil.nowZoned(uiState.settings.timezoneId).toLocalDate()
    }
    val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Upcoming exams & deadlines
    val upcomingExams = remember(uiState.tasks, todayIso) {
        uiState.tasks.filter {
            !it.completed &&
                (it.category.equals("EXAM", true) || it.category.equals("DEADLINE", true)) &&
                it.dateIso >= todayIso
        }.sortedBy { it.dateIso }
    }

    // Weekly Intelligence metrics
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

    // Subject balance analysis
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
                text = "SHINOBI INTELLIGENCE & PROGRESS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Rank Progression Card & Ladder
        item {
            HoloCard(accentColor = GoldRankS) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = GoldRankS, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Lv.${rankInfo.level}  ${rankInfo.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldRankS,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.xp ?: 0} Total XP • Best Streak: ${profile?.longestStreak ?: 0} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { rankInfo.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = GoldRankS,
                    trackColor = visuals.primaryAccent.copy(alpha = 0.16f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                RANK_LADDER.forEach { (reqLv, title) ->
                    val reached = rankInfo.level >= reqLv
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lv.$reqLv  $title",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (reached) JadeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = if (reached) "UNLOCKED" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reached) JadeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Weekly Intelligence Report + Canvas Chart
        item {
            HoloCard(accentColor = visuals.primaryAccent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timeline, contentDescription = null, tint = visuals.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WEEKLY INTELLIGENCE REPORT", style = MaterialTheme.typography.labelLarge, color = visuals.primaryAccent)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatMiniBlock("Productivity", "$productivityScore/100", visuals.primaryAccent)
                    StatMiniBlock("Completed", "$completedTasks", JadeEmerald)
                    StatMiniBlock("Overdue", "$missedPastTasks", CrimsonAlert)
                    StatMiniBlock("Focus Hrs", String.format("%.1fh", totalFocusMins / 60f), ChakraOrange)
                }

                if (subjectMinutes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Study Balance by Subject (minutes)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val maxMins = (subjectMinutes.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((subjectMinutes.size * 26).dp)
                    ) {
                        val rowH = size.height / subjectMinutes.size
                        subjectMinutes.forEachIndexed { idx, (_, mins) ->
                            val frac = mins.toFloat() / maxMins.toFloat()
                            val barW = size.width * 0.92f * frac
                            val y = idx * rowH + rowH * 0.25f
                            drawRoundRect(
                                color = visuals.primaryAccent.copy(alpha = 0.2f),
                                topLeft = Offset(0f, y),
                                size = Size(size.width * 0.92f, rowH * 0.5f),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                color = if (idx % 2 == 0) visuals.primaryAccent else ChakraOrange,
                                topLeft = Offset(0f, y),
                                size = Size(barW.coerceAtLeast(10f), rowH * 0.5f),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                    subjectMinutes.forEach { (subj, mins) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subj, style = MaterialTheme.typography.labelSmall)
                            Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = visuals.primaryAccent)
                        }
                    }
                }
            }
        }

        // Exam Countdowns & Revision Plan Generator
        item {
            HoloCard(accentColor = CrimsonAlert) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXAM COUNTDOWN & REVISION GENERATOR", style = MaterialTheme.typography.labelLarge, color = CrimsonAlert)
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
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                        color = if (daysLeft <= 3) CrimsonAlert else GoldRankS
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
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
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
            HoloCard(accentColor = ArcaneViolet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldRankS)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACHIEVEMENTS (${uiState.achievements.count { it.unlocked }}/${uiState.achievements.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldRankS
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                uiState.achievements.forEach { ach ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ach.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (ach.unlocked) GoldRankS else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
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
                            color = if (ach.unlocked) JadeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text("Recent Mission Completion Log", style = MaterialTheme.typography.titleSmall)
                }
            }
            items(uiState.history.take(8), key = { it.id }) { h ->
                HoloCard(contentPadding = PaddingValues(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(h.taskTitle, fontWeight = FontWeight.Medium)
                            Text(h.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RankBadgeChip(h.missionRank)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+${h.xpEarned} XP", color = JadeEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@Composable
private fun StatMiniBlock(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
