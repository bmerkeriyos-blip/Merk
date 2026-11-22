package com.example

import com.example.core.common.ExportHelper
import com.example.core.common.MissionRank
import com.example.core.common.RankProgression
import com.example.core.common.ShinobiTimeUtil
import com.example.core.media.ShinobiSfx
import com.example.data.local.PlannerTaskEntity
import com.example.ui.theme.GlassTier
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiVisualConfig
import com.example.ui.theme.ThemeDesignSystem
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExampleUnitTest {

    @Test
    fun `standard date and time formatting supports am pm and 24 hour modes`() {
        assertEquals("Thu, Sep 11, 2025", ShinobiTimeUtil.formatDateHeader(LocalDate.of(2025, 9, 11)))
        assertEquals("14:30", ShinobiTimeUtil.formatTime(14 * 60 + 30, use24Hour = true))
        assertEquals("2:30 PM", ShinobiTimeUtil.formatTime(14 * 60 + 30, use24Hour = false))
        assertEquals("12:00 AM", ShinobiTimeUtil.formatTime(0, use24Hour = false))
        assertEquals("12:00 PM", ShinobiTimeUtil.formatTime(12 * 60, use24Hour = false))
        assertTrue(ShinobiSfx.entries.containsAll(listOf(ShinobiSfx.SHURIKEN, ShinobiSfx.JUTSU)))
    }

    @Test
    fun `ethiopian clock shifts 6 hours from standard day`() {
        val formatted = ShinobiTimeUtil.formatEthiopianClock(LocalTime.of(13, 30))
        assertEquals("7:30 ET (Afternoon)", formatted)
    }

    @Test
    fun `rank progression advances levels and titles deterministically`() {
        val lvl1 = RankProgression.computeLevelInfo(0)
        assertEquals(1, lvl1.level)
        assertEquals("Academy Student", lvl1.title)

        val higher = RankProgression.computeLevelInfo(4000)
        assertTrue(higher.level >= 5)
        assertTrue(RankProgression.titleForLevel(35) == "Grand Kage")
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

    @Test
    fun `theme design system provides consistent glassmorphism tokens and fallbacks`() {
        val darkGlass = ShinobiVisualConfig(isDark = true, glassmorphismEnabled = true, batterySaverMode = false)
        assertEquals(ThemeDesignSystem.HeroBlurRadius, ThemeDesignSystem.blurRadiusFor(GlassTier.HERO, darkGlass))
        assertEquals(ThemeDesignSystem.DefaultBlurRadius, ThemeDesignSystem.blurRadiusFor(GlassTier.STANDARD, darkGlass))
        assertEquals(ThemeDesignSystem.SubtleBlurRadius, ThemeDesignSystem.blurRadiusFor(GlassTier.SUBTLE, darkGlass))
        assertEquals(ThemeDesignSystem.FloatingBarBlurRadius, ThemeDesignSystem.blurRadiusFor(GlassTier.FLOATING_BAR, darkGlass))

        assertTrue(ThemeDesignSystem.containerAlphaFor(GlassTier.HERO, darkGlass) > ThemeDesignSystem.containerAlphaFor(GlassTier.SUBTLE, darkGlass))
        assertTrue(ThemeDesignSystem.TranslucentDarkBackground.alpha < 1f)

        val batterySaver = darkGlass.copy(batterySaverMode = true)
        assertEquals(0.dp, ThemeDesignSystem.blurRadiusFor(GlassTier.HERO, batterySaver))

        val strokeIdle = ThemeDesignSystem.borderStroke(NarutoOrange, darkGlass, GlassTier.STANDARD, glowActive = false)
        val strokeActive = ThemeDesignSystem.borderStroke(NarutoOrange, darkGlass, GlassTier.STANDARD, glowActive = true)
        assertEquals(ThemeDesignSystem.DefaultBorderWidth, strokeIdle.width)
        assertEquals(ThemeDesignSystem.ActiveBorderWidth, strokeActive.width)

        val globalGlowOn = darkGlass.copy(globalGlowEnabled = true, glowIntensity = 0.8f)
        val globalGlowOff = darkGlass.copy(globalGlowEnabled = false, glowIntensity = 0.8f)
        assertTrue(ThemeDesignSystem.glowHaloAlphaFor(GlassTier.STANDARD, globalGlowOn, glowActive = false) > 0f)
        assertEquals(0f, ThemeDesignSystem.glowHaloAlphaFor(GlassTier.STANDARD, globalGlowOff, glowActive = false), 0.0001f)
        assertTrue(ThemeDesignSystem.glowHaloAlphaFor(GlassTier.HERO, globalGlowOn, glowActive = true) >
            ThemeDesignSystem.glowHaloAlphaFor(GlassTier.STANDARD, globalGlowOn, glowActive = false))

        val lightMode = globalGlowOn.copy(isDark = false)
        assertEquals(0f, ThemeDesignSystem.glowHaloAlphaFor(GlassTier.HERO, lightMode, glowActive = true), 0.0001f)
        assertEquals(0f, ThemeDesignSystem.glowHaloAlphaFor(GlassTier.HERO, batterySaver.copy(globalGlowEnabled = true), glowActive = true), 0.0001f)
    }
}
