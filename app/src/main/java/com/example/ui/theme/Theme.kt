package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemePreset(val label: String, val subtitle: String) {
    NARUTO("Hidden Leaf Chakra", "Futuristic orange, chakra cyan & dark obsidian glass"),
    CYBER_SHINOBI("Midnight ANBU", "Chakra cyan, holographic violet & night glass"),
    ACADEMIC("Academy Scroll", "Crisp structured study view with low glow"),
    AMOLED("Shadow Clone Black", "Pure AMOLED #030508 battery-conscious glass"),
    MINIMAL("Sage Zen", "Calm minimal glass panels, zero particles"),
    CUSTOM("Custom Chakra", "Full user chakra accent color control")
}

enum class AppearanceMode(val label: String) {
    DARK("Dark Navy"),
    AMOLED("AMOLED Black"),
    LIGHT("Light"),
    SYSTEM("System")
}

@Immutable
data class ShinobiVisualConfig(
    val preset: ThemePreset = ThemePreset.NARUTO,
    val appearanceMode: AppearanceMode = AppearanceMode.DARK,
    val isDark: Boolean = true,
    val primaryAccent: Color = ChakraOrange,
    val secondaryAccent: Color = ChakraCyan,
    val tertiaryAccent: Color = HolographicPurple,
    val glassmorphismEnabled: Boolean = true,
    val globalGlowEnabled: Boolean = true,
    val glowIntensity: Float = 0.8f,
    val showScanlines: Boolean = false,
    val showParticles: Boolean = true,
    val animationsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val batterySaverMode: Boolean = false,
    val cardCornerRadiusDp: Int = 20
)

val LocalShinobiVisuals = staticCompositionLocalOf { ShinobiVisualConfig() }

val ShinobiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun resolveAccentColor(key: String): Color = when (key.uppercase()) {
    "CYAN" -> ChakraCyan
    "VIOLET" -> HolographicPurple
    "EMERALD" -> ChakraGreen
    "GOLD" -> GoldRankS
    "CRIMSON" -> CrimsonAlert
    else -> NarutoOrange
}

@Composable
fun ShinobiPlannerTheme(
    preset: ThemePreset = ThemePreset.NARUTO,
    appearanceMode: AppearanceMode = AppearanceMode.DARK,
    accentKey: String = "ORANGE",
    glassmorphism: Boolean = true,
    globalGlowEnabled: Boolean = true,
    glowIntensity: Float = 0.8f,
    animationsEnabled: Boolean = true,
    reduceMotion: Boolean = false,
    batterySaver: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearanceMode) {
        AppearanceMode.DARK, AppearanceMode.AMOLED -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.SYSTEM -> systemDark
    }
    val useAmoled = appearanceMode == AppearanceMode.AMOLED || preset == ThemePreset.AMOLED

    val primaryAccent = when (preset) {
        ThemePreset.NARUTO -> NarutoOrange
        ThemePreset.CYBER_SHINOBI -> ChakraCyan
        ThemePreset.ACADEMIC -> if (useDark) Color(0xFF60A5FA) else AcademicSecondary
        ThemePreset.MINIMAL -> if (useDark) Color(0xFF94A3B8) else Color(0xFF334155)
        ThemePreset.CUSTOM -> resolveAccentColor(accentKey)
        ThemePreset.AMOLED -> NarutoOrange
    }
    val secondaryAccent = when (preset) {
        ThemePreset.NARUTO -> ChakraCyan
        ThemePreset.CYBER_SHINOBI -> NarutoOrange
        ThemePreset.ACADEMIC -> if (useDark) NarutoOrange else AcademicPrimary
        ThemePreset.MINIMAL -> if (useDark) ChakraCyan.copy(alpha = 0.7f) else Color(0xFF475569)
        ThemePreset.AMOLED -> ChakraCyan
        ThemePreset.CUSTOM -> if (accentKey.uppercase() == "CYAN") NarutoOrange else ChakraCyan
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = MidnightNavy,
            primaryContainer = primaryAccent.copy(alpha = 0.18f),
            onPrimaryContainer = CyberTextPrimary,
            secondary = secondaryAccent,
            onSecondary = MidnightNavy,
            secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onSecondaryContainer = CyberTextPrimary,
            tertiary = HolographicPurple,
            onTertiary = MidnightNavy,
            background = if (useAmoled) PureBlackAmoled else MidnightNavy,
            onBackground = CyberTextPrimary,
            surface = if (useAmoled) AmoledCard else DeepObsidian,
            onSurface = CyberTextPrimary,
            surfaceVariant = if (useAmoled) Color(0xFF101726) else HoloSurface,
            onSurfaceVariant = CyberTextSecondary,
            outline = CyberBorderGlow,
            error = CrimsonAlert,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = if (preset == ThemePreset.NARUTO || preset == ThemePreset.CUSTOM) AcademicPrimary else AcademicSecondary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEAD8),
            onPrimaryContainer = AcademicTextPrimary,
            secondary = AcademicSecondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0F2FE),
            onSecondaryContainer = AcademicTextPrimary,
            tertiary = Color(0xFF7C3AED),
            onTertiary = Color.White,
            background = AcademicBgLight,
            onBackground = AcademicTextPrimary,
            surface = AcademicSurfaceLight,
            onSurface = AcademicTextPrimary,
            surfaceVariant = AcademicSurfaceVariant,
            onSurfaceVariant = AcademicTextSecondary,
            outline = Color(0xFFCBD5E1),
            error = CrimsonAlert,
            onError = Color.White
        )
    }

    val effectiveMotion = animationsEnabled && !reduceMotion && !batterySaver
    val visuals = ShinobiVisualConfig(
        preset = preset,
        appearanceMode = appearanceMode,
        isDark = useDark,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        tertiaryAccent = HolographicPurple,
        glassmorphismEnabled = glassmorphism && preset != ThemePreset.ACADEMIC && preset != ThemePreset.MINIMAL,
        globalGlowEnabled = globalGlowEnabled && useDark && !batterySaver && preset != ThemePreset.MINIMAL,
        glowIntensity = if (preset == ThemePreset.MINIMAL || preset == ThemePreset.ACADEMIC || batterySaver || !useDark) {
            0.20f
        } else {
            glowIntensity
        },
        showScanlines = false,
        showParticles = (preset == ThemePreset.NARUTO || preset == ThemePreset.CYBER_SHINOBI || preset == ThemePreset.AMOLED) &&
            effectiveMotion && useDark,
        animationsEnabled = effectiveMotion,
        reduceMotion = reduceMotion,
        batterySaverMode = batterySaver
    )

    CompositionLocalProvider(LocalShinobiVisuals provides visuals) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildShinobiTypography(fontScale),
            shapes = ShinobiShapes,
            content = content
        )
    }
}
