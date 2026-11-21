package com.example.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EthiopicDate(
    val year: Int,
    val month: Int, // 1..13
    val day: Int    // 1..30 (or 1..5/6 for Pagume)
) {
    val monthName: String
        get() = ETHIOPIC_MONTHS.getOrElse(month - 1) { "Pagume" }

    fun formatShort(): String = "$monthName $day, $year E.C."

    companion object {
        val ETHIOPIC_MONTHS = listOf(
            "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit",
            "Megabit", "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
        )
    }
}

object EthiopicCalendarUtil {
    const val DEFAULT_TIMEZONE_ID = "Africa/Addis_Ababa"
    private const val JD_EPOCH_OFFSET_ETHIOPIC = 1723856L
    private const val UNIX_EPOCH_JDN = 2440588L

    fun safeZoneId(timezoneId: String): ZoneId =
        try {
            ZoneId.of(timezoneId.ifBlank { DEFAULT_TIMEZONE_ID })
        } catch (_: Exception) {
            ZoneId.of(DEFAULT_TIMEZONE_ID)
        }

    fun nowZoned(timezoneId: String = DEFAULT_TIMEZONE_ID): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.now(), safeZoneId(timezoneId))

    fun toEthiopic(date: LocalDate): EthiopicDate {
        val jdn = date.toEpochDay() + UNIX_EPOCH_JDN
        return jdnToEthiopic(jdn)
    }

    private fun jdnToEthiopic(jdn: Long): EthiopicDate {
        val r = (jdn - JD_EPOCH_OFFSET_ETHIOPIC).mod(1461L)
        val n = (r % 365L) + 365L * (r / 1460L)
        val year = (4L * ((jdn - JD_EPOCH_OFFSET_ETHIOPIC) / 1461L) + (r / 365L) - (r / 1460L)).toInt()
        val month = (n / 30L).toInt() + 1
        val day = (n % 30L).toInt() + 1
        return EthiopicDate(year = year, month = month, day = day)
    }

    fun formatTime(minutesFromMidnight: Int, use24Hour: Boolean): String {
        val clamped = minutesFromMidnight.coerceIn(0, 1439)
        val hour = clamped / 60
        val minute = clamped % 60
        val time = LocalTime.of(hour, minute)
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }

    fun formatEthiopianClock(time: LocalTime): String {
        val shiftedHour24 = (time.hour - 6 + 24) % 24
        val displayHour = if (shiftedHour24 % 12 == 0) 12 else shiftedHour24 % 12
        val period = when (time.hour) {
            in 6..11 -> "Morning"
            in 12..17 -> "Afternoon"
            in 18..23 -> "Evening"
            else -> "Night"
        }
        return String.format(Locale.ENGLISH, "%d:%02d ET (%s)", displayHour, time.minute, period)
    }

    fun formatDualDateHeader(
        date: LocalDate,
        useEthiopian: Boolean,
        showDual: Boolean
    ): String {
        val greg = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH))
        val eth = toEthiopic(date).formatShort()
        return when {
            showDual -> "$greg  •  $eth"
            useEthiopian -> eth
            else -> greg
        }
    }
}
