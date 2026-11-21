package com.example.core.common

enum class MissionRank(
    val code: String,
    val label: String,
    val baseXp: Int,
    val difficultyLabel: String
) {
    D("D", "D-Rank", 40, "Routine"),
    C("C", "C-Rank", 80, "Standard"),
    B("B", "B-Rank", 140, "Tactical"),
    A("A", "A-Rank", 240, "Elite"),
    S("S", "S-Rank", 400, "Mastery");

    companion object {
        fun fromCode(raw: String): MissionRank =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: C
    }
}

enum class TaskCategory(val code: String, val label: String, val defaultColorHex: String) {
    CLASS("CLASS", "University Class", "#00E5FF"),
    STUDY("STUDY", "Study Session", "#38BDF8"),
    ASSIGNMENT("ASSIGNMENT", "Assignment", "#FF6D00"),
    EXAM("EXAM", "Examination", "#FF3D5A"),
    DEADLINE("DEADLINE", "Deadline", "#FFD600"),
    PERSONAL("PERSONAL", "Personal", "#9D4EDD"),
    BREAK("BREAK", "Recovery Break", "#00E676"),
    EXERCISE("EXERCISE", "Physical Training", "#10B981");

    companion object {
        fun fromCode(raw: String): TaskCategory =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: STUDY
    }
}

enum class TaskPriority(val code: String, val label: String) {
    LOW("LOW", "Low"),
    MEDIUM("MEDIUM", "Medium"),
    HIGH("HIGH", "High"),
    CRITICAL("CRITICAL", "Critical");

    companion object {
        fun fromCode(raw: String): TaskPriority =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class RecurrenceRule(val code: String, val label: String) {
    NONE("NONE", "One-time"),
    DAILY("DAILY", "Daily"),
    WEEKLY("WEEKLY", "Weekly"),
    WEEKDAYS("WEEKDAYS", "Weekdays");

    companion object {
        fun fromCode(raw: String): RecurrenceRule =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: NONE
    }
}

data class NinjaRankInfo(
    val level: Int,
    val title: String,
    val currentLevelXp: Int,
    val nextLevelXpRequired: Int,
    val progressFraction: Float
)

object RankProgression {
    fun xpRequiredForLevel(level: Int): Int = 200 + (level - 1) * 120

    fun computeLevelInfo(totalXp: Int): NinjaRankInfo {
        var remaining = totalXp.coerceAtLeast(0)
        var level = 1
        var req = xpRequiredForLevel(level)
        while (remaining >= req && level < 500) {
            remaining -= req
            level++
            req = xpRequiredForLevel(level)
        }
        val title = titleForLevel(level)
        val fraction = if (req > 0) (remaining.toFloat() / req.toFloat()).coerceIn(0f, 1f) else 0f
        return NinjaRankInfo(
            level = level,
            title = title,
            currentLevelXp = remaining,
            nextLevelXpRequired = req,
            progressFraction = fraction
        )
    }

    fun titleForLevel(level: Int): String = when {
        level >= 35 -> "Grand Cyber Kage"
        level >= 25 -> "Cyber Jonin"
        level >= 16 -> "Chakra Vanguard"
        level >= 10 -> "Scroll Tactician"
        level >= 5 -> "Shadow Scholar"
        else -> "Academy Initiate"
    }

    fun streakBonusXp(streakDays: Int): Int = (streakDays.coerceAtMost(14)) * 10
}
