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
    CYBER_SHINOBI("Cyber Shinobi", "Neon cyan & chakra orange HUD"),
    NARUTO("Chakra Mode", "Vibrant orange & anime glow"),
    ACADEMIC("Academic Focus", "Clean minimal study interface"),
    AMOLED("AMOLED Stealth", "Pure black battery-conscious"),
    MINIMAL("Minimal Zen", "Calm cards, zero particles"),
    CUSTOM("Custom Shinobi", "Full user control")
}

enum class AppearanceMode(val label: String) {
    DARK("Dark Navy"),
    AMOLED("AMOLED Black"),
    LIGHT("Light"),
    SYSTEM("System")
}

@Immutable
data class ShinobiVisualConfig(
    val preset: ThemePreset = ThemePreset.CYBER_SHINOBI,
    val appearanceMode: AppearanceMode = AppearanceMode.DARK,
    val primaryAccent: Color = NeonCyan,
    val secondaryAccent: Color = ChakraOrange,
    val tertiaryAccent: Color = ArcaneViolet,
    val glassmorphismEnabled: Boolean = true,
    val glowIntensity: Float = 0.8f,
    val showScanlines: Boolean = true,
    val showParticles: Boolean = true,
    val animationsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val batterySaverMode: Boolean = false,
    val cardCornerRadiusDp: Int = 18
)

val LocalShinobiVisuals = staticCompositionLocalOf { ShinobiVisualConfig() }

val ShinobiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun resolveAccentColor(key: String): Color = when (key.uppercase()) {
    "ORANGE" -> ChakraOrange
    "VIOLET" -> ArcaneViolet
    "EMERALD" -> JadeEmerald
    "GOLD" -> GoldRankS
    "CRIMSON" -> CrimsonAlert
    else -> NeonCyan
}

@Composable
fun ShinobiPlannerTheme(
    preset: ThemePreset = ThemePreset.CYBER_SHINOBI,
    appearanceMode: AppearanceMode = AppearanceMode.DARK,
    accentKey: String = "CYAN",
    glassmorphism: Boolean = true,
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
        ThemePreset.NARUTO -> ChakraOrange
        ThemePreset.ACADEMIC -> if (useDark) Color(0xFF60A5FA) else AcademicPrimary
        ThemePreset.MINIMAL -> if (useDark) Color(0xFF94A3B8) else Color(0xFF334155)
        ThemePreset.CUSTOM -> resolveAccentColor(accentKey)
        else -> NeonCyan
    }
    val secondaryAccent = when (preset) {
        ThemePreset.NARUTO -> GoldRankS
        ThemePreset.ACADEMIC -> if (useDark) Color(0xFF38BDF8) else AcademicSecondary
        ThemePreset.MINIMAL -> if (useDark) Color(0xFF64748B) else Color(0xFF475569)
        else -> ChakraOrange
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
            tertiary = ArcaneViolet,
            onTertiary = Color.White,
            background = if (useAmoled) PureBlackAmoled else MidnightNavy,
            onBackground = CyberTextPrimary,
            surface = if (useAmoled) AmoledCard else DeepObsidian,
            onSurface = CyberTextPrimary,
            surfaceVariant = if (useAmoled) Color(0xFF111624) else HoloSurface,
            onSurfaceVariant = CyberTextSecondary,
            outline = CyberBorderGlow,
            error = CrimsonAlert,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = if (preset == ThemePreset.NARUTO) Color(0xFFE65100) else AcademicPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEBFF),
            onPrimaryContainer = AcademicTextPrimary,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFE5CC),
            onSecondaryContainer = AcademicTextPrimary,
            tertiary = ArcaneViolet,
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
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        tertiaryAccent = ArcaneViolet,
        glassmorphismEnabled = glassmorphism && preset != ThemePreset.ACADEMIC && preset != ThemePreset.MINIMAL,
        glowIntensity = if (preset == ThemePreset.MINIMAL || preset == ThemePreset.ACADEMIC || batterySaver) 0.15f else glowIntensity,
        showScanlines = preset == ThemePreset.CYBER_SHINOBI && !batterySaver && !reduceMotion,
        showParticles = (preset == ThemePreset.CYBER_SHINOBI || preset == ThemePreset.NARUTO) && effectiveMotion,
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
