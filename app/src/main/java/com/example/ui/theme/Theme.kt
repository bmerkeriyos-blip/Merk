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

enum class SurfaceStyle(val label: String) {
    ANIME_CYBER("Anime Cyber"),
    GLASSMORPHISM("Frosted Glass"),
    CLAYMORPHISM("Soft 3D Clay"),
    NEUMORPHISM("Neumorphic"),
    DARK_JAPANESE("Dark Japanese")
}

enum class ThemePreset(
    val label: String,
    val subtitle: String,
    val surfaceStyle: SurfaceStyle,
    val defaultResName: String,
    val swatchPrimary: Color,
    val swatchSecondary: Color,
    val swatchSurface: Color
) {
    NARUTO_CHAKRA(
        label = "Naruto Chakra",
        subtitle = "Hidden Leaf Rasengan orange, cyan & anime cyber HUD",
        surfaceStyle = SurfaceStyle.ANIME_CYBER,
        defaultResName = "img_naruto_uzumaki",
        swatchPrimary = NarutoOrange,
        swatchSecondary = ChakraCyan,
        swatchSurface = DeepObsidian
    ),
    SAGE_MODE(
        label = "Sage Mode",
        subtitle = "Mount Myoboku senjutsu emerald, gold & frosted glass",
        surfaceStyle = SurfaceStyle.GLASSMORPHISM,
        defaultResName = "img_naruto_sage_mode",
        swatchPrimary = SageEmerald,
        swatchSecondary = SageAmber,
        swatchSurface = Color(0xFF0B1918)
    ),
    KURAMA(
        label = "Kurama Mode",
        subtitle = "Nine-Tails radiant gold blaze & dark obsidian cyber glow",
        surfaceStyle = SurfaceStyle.ANIME_CYBER,
        defaultResName = "img_naruto_kurama_mode",
        swatchPrimary = KuramaGold,
        swatchSecondary = ChakraOrangeDeep,
        swatchSurface = Color(0xFF17100B)
    ),
    SASUKE(
        label = "Sasuke Theme",
        subtitle = "Uchiha shrine indigo, Susanoo violet & Chidori steel",
        surfaceStyle = SurfaceStyle.NEUMORPHISM,
        defaultResName = "img_sasuke_uchiha",
        swatchPrimary = SusanooViolet,
        swatchSecondary = ChidoriCyan,
        swatchSurface = NeuDarkBase
    ),
    AKATSUKI(
        label = "Akatsuki",
        subtitle = "Crimson dawn clouds, abyssal slate & dramatic glass",
        surfaceStyle = SurfaceStyle.GLASSMORPHISM,
        defaultResName = "img_akatsuki_clouds",
        swatchPrimary = AkatsukiCrimson,
        swatchSecondary = AkatsukiCloudGray,
        swatchSurface = Color(0xFF120A0E)
    ),
    DARK_JAPANESE(
        label = "Dark Japanese",
        subtitle = "Sumi-e ink panels, torii vermilion & warm washi gold",
        surfaceStyle = SurfaceStyle.DARK_JAPANESE,
        defaultResName = "img_itachi_uchiha",
        swatchPrimary = VermilionTorii,
        swatchSecondary = WashiGold,
        swatchSurface = SumiInkSurface
    ),
    PREMIUM_GLASS(
        label = "Premium Glass",
        subtitle = "Crystal sapphire frost & Minato Yellow Flash silver",
        surfaceStyle = SurfaceStyle.GLASSMORPHISM,
        defaultResName = "img_minato_namikaze",
        swatchPrimary = ChakraCyan,
        swatchSecondary = GoldRankS,
        swatchSurface = HoloSurface
    ),
    CLAY(
        label = "Clay Theme",
        subtitle = "Soft 3D rounded dojo stone with tactile warm highlights",
        surfaceStyle = SurfaceStyle.CLAYMORPHISM,
        defaultResName = "img_kakashi_hatake",
        swatchPrimary = ChakraAmber,
        swatchSecondary = ChakraCyan,
        swatchSurface = ClayDarkElevated
    );

    companion object {
        fun fromStorage(raw: String?): ThemePreset {
            val key = raw.orEmpty().trim().uppercase()
            return entries.firstOrNull { it.name == key } ?: when (key) {
                "NARUTO", "CUSTOM" -> NARUTO_CHAKRA
                "CYBER_SHINOBI" -> PREMIUM_GLASS
                "ACADEMIC" -> DARK_JAPANESE
                "MINIMAL" -> SAGE_MODE
                "AMOLED" -> AKATSUKI
                else -> NARUTO_CHAKRA
            }
        }
    }
}

enum class AppearanceMode(val label: String) {
    DARK("Dark Dojo"),
    AMOLED("AMOLED Black"),
    LIGHT("Light Scroll"),
    SYSTEM("System")
}

@Immutable
data class ShinobiVisualConfig(
    val preset: ThemePreset = ThemePreset.NARUTO_CHAKRA,
    val appearanceMode: AppearanceMode = AppearanceMode.DARK,
    val defaultSurfaceStyle: SurfaceStyle = SurfaceStyle.GLASSMORPHISM,
    val isDark: Boolean = true,
    val primaryAccent: Color = NarutoOrange,
    val secondaryAccent: Color = ChakraCyan,
    val tertiaryAccent: Color = SusanooViolet,
    val glassmorphismEnabled: Boolean = true,
    val globalGlowEnabled: Boolean = true,
    val glowIntensity: Float = 0.8f,
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

fun resolveAccentColor(key: String, fallback: Color = NarutoOrange): Color = when (key.uppercase()) {
    "DEFAULT" -> fallback
    "CYAN" -> ChakraCyan
    "VIOLET" -> SusanooViolet
    "EMERALD" -> SageEmerald
    "GOLD" -> KuramaGold
    "CRIMSON" -> AkatsukiCrimson
    "ORANGE" -> NarutoOrange
    else -> fallback
}

@Composable
fun ShinobiPlannerTheme(
    preset: ThemePreset = ThemePreset.NARUTO_CHAKRA,
    appearanceMode: AppearanceMode = AppearanceMode.DARK,
    accentKey: String = "DEFAULT",
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
    val useAmoled = appearanceMode == AppearanceMode.AMOLED

    val basePrimary = when (preset) {
        ThemePreset.NARUTO_CHAKRA -> NarutoOrange
        ThemePreset.SAGE_MODE -> SageEmerald
        ThemePreset.KURAMA -> KuramaGold
        ThemePreset.SASUKE -> SusanooViolet
        ThemePreset.AKATSUKI -> AkatsukiCrimson
        ThemePreset.DARK_JAPANESE -> VermilionTorii
        ThemePreset.PREMIUM_GLASS -> ChakraCyan
        ThemePreset.CLAY -> ChakraAmber
    }
    val primaryAccent = if (accentKey.uppercase() == "DEFAULT") {
        basePrimary
    } else {
        resolveAccentColor(accentKey, basePrimary)
    }

    val secondaryAccent = when (preset) {
        ThemePreset.NARUTO_CHAKRA -> ChakraCyan
        ThemePreset.SAGE_MODE -> SageAmber
        ThemePreset.KURAMA -> ChakraOrangeDeep
        ThemePreset.SASUKE -> ChidoriCyan
        ThemePreset.AKATSUKI -> AkatsukiCloudGray
        ThemePreset.DARK_JAPANESE -> WashiGold
        ThemePreset.PREMIUM_GLASS -> GoldRankS
        ThemePreset.CLAY -> ChakraCyan
    }

    val tertiaryAccent = when (preset) {
        ThemePreset.SASUKE -> SasukeIndigo
        ThemePreset.DARK_JAPANESE -> WashiCream
        ThemePreset.SAGE_MODE -> ChakraCyan
        else -> SusanooViolet
    }

    val themeBackground = when {
        !useDark -> AcademicBgLight
        useAmoled -> PureBlackAmoled
        preset == ThemePreset.DARK_JAPANESE -> SumiInkDark
        preset == ThemePreset.CLAY -> ClayDarkBase
        preset == ThemePreset.SASUKE -> Color(0xFF0B0E1A)
        preset == ThemePreset.AKATSUKI -> Color(0xFF0C070A)
        preset == ThemePreset.SAGE_MODE -> Color(0xFF07110F)
        preset == ThemePreset.KURAMA -> Color(0xFF100B07)
        else -> MidnightNavy
    }

    val themeSurface = when {
        !useDark -> if (preset == ThemePreset.CLAY) ClayLightSurface else AcademicSurfaceLight
        useAmoled -> AmoledCard
        preset == ThemePreset.DARK_JAPANESE -> SumiInkSurface
        preset == ThemePreset.CLAY -> ClayDarkElevated
        preset == ThemePreset.SASUKE -> NeuDarkBase
        preset == ThemePreset.AKATSUKI -> Color(0xFF160D12)
        preset == ThemePreset.SAGE_MODE -> Color(0xFF0D1C19)
        preset == ThemePreset.KURAMA -> Color(0xFF19120B)
        else -> DeepObsidian
    }

    val themeSurfaceVariant = when {
        !useDark -> AcademicSurfaceVariant
        useAmoled -> Color(0xFF111520)
        preset == ThemePreset.DARK_JAPANESE -> SumiInkElevated
        preset == ThemePreset.CLAY -> ClayWarmHighlight
        else -> HoloSurface
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = MidnightNavy,
            primaryContainer = primaryAccent.copy(alpha = 0.20f),
            onPrimaryContainer = CyberTextPrimary,
            secondary = secondaryAccent,
            onSecondary = MidnightNavy,
            secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onSecondaryContainer = CyberTextPrimary,
            tertiary = tertiaryAccent,
            onTertiary = MidnightNavy,
            background = themeBackground,
            onBackground = CyberTextPrimary,
            surface = themeSurface,
            onSurface = CyberTextPrimary,
            surfaceVariant = themeSurfaceVariant,
            onSurfaceVariant = CyberTextSecondary,
            outline = primaryAccent.copy(alpha = 0.30f),
            error = CrimsonAlert,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = if (preset == ThemePreset.AKATSUKI || preset == ThemePreset.DARK_JAPANESE) VermilionTorii else AcademicPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEAD8),
            onPrimaryContainer = AcademicTextPrimary,
            secondary = AcademicSecondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0F2FE),
            onSecondaryContainer = AcademicTextPrimary,
            tertiary = Color(0xFF7C3AED),
            onTertiary = Color.White,
            background = themeBackground,
            onBackground = AcademicTextPrimary,
            surface = themeSurface,
            onSurface = AcademicTextPrimary,
            surfaceVariant = themeSurfaceVariant,
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
        defaultSurfaceStyle = preset.surfaceStyle,
        isDark = useDark,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        tertiaryAccent = tertiaryAccent,
        glassmorphismEnabled = glassmorphism,
        globalGlowEnabled = globalGlowEnabled && useDark && !batterySaver,
        glowIntensity = if (batterySaver || !useDark) 0.22f else glowIntensity,
        showParticles = effectiveMotion && useDark,
        animationsEnabled = effectiveMotion,
        reduceMotion = reduceMotion,
        batterySaverMode = batterySaver,
        cardCornerRadiusDp = if (preset.surfaceStyle == SurfaceStyle.CLAYMORPHISM) 24 else 20
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
