package com.example

import com.example.core.common.EthiopicCalendarUtil
import com.example.core.common.ExportHelper
import com.example.core.common.MissionRank
import com.example.core.common.RankProgression
import com.example.data.local.PlannerTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExampleUnitTest {

    @Test
    fun `ethiopian enkutatash conversion is accurate`() {
        val eth = EthiopicCalendarUtil.toEthiopic(LocalDate.of(2025, 9, 11))
        assertEquals(2018, eth.year)
        assertEquals(1, eth.month)
        assertEquals(1, eth.day)
        assertEquals("Meskerem 1, 2018 E.C.", eth.formatShort())

        val pagume6 = EthiopicCalendarUtil.toEthiopic(LocalDate.of(2023, 9, 11))
        assertEquals("Pagume 6, 2015 E.C.", pagume6.formatShort())
    }

    @Test
    fun `ethiopian clock shifts 6 hours from standard day`() {
        val formatted = EthiopicCalendarUtil.formatEthiopianClock(LocalTime.of(13, 30))
        assertEquals("7:30 ET (Afternoon)", formatted)
    }

    @Test
    fun `rank progression advances levels and titles deterministically`() {
        val lvl1 = RankProgression.computeLevelInfo(0)
        assertEquals(1, lvl1.level)
        assertEquals("Academy Initiate", lvl1.title)

        val higher = RankProgression.computeLevelInfo(4000)
        assertTrue(higher.level >= 5)
        assertTrue(RankProgression.titleForLevel(35) == "Grand Cyber Kage")
        assertEquals(400, MissionRank.S.baseXp)
    }

    @Test
    fun `csv export escapes quotes and preserves columns`() {
        val csv = ExportHelper.buildCsvExport(
            listOf(
                PlannerTaskEntity(
                    userId = "u1",
                    title = "Read \"Algorithm\" Notes, Ch 1",
                    subjectName = "CS301",
                    category = "STUDY",
                    dateIso = "2026-04-02",
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 10 * 60,
                    missionRank = "A",
                    priority = "HIGH",
                    xpReward = 240
                )
            )
        )
        assertTrue(csv.contains("Title,Subject,Category"))
        assertTrue(csv.contains("\"Read \"\"Algorithm\"\" Notes, Ch 1\""))
        assertTrue(csv.contains("\"CS301\""))
    }
}
