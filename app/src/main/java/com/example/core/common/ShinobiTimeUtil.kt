package com.example.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ShinobiTimeUtil {
    const val DEFAULT_TIMEZONE_ID = "Africa/Addis_Ababa"

    fun safeZoneId(timezoneId: String): ZoneId =
        try {
            ZoneId.of(timezoneId.ifBlank { DEFAULT_TIMEZONE_ID })
        } catch (_: Exception) {
            ZoneId.of(DEFAULT_TIMEZONE_ID)
        }

    fun nowZoned(timezoneId: String = DEFAULT_TIMEZONE_ID): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.now(), safeZoneId(timezoneId))

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

    fun formatDateHeader(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH))
}
