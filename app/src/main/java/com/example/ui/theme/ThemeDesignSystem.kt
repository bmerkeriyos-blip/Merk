package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.Build

enum class GlassTier {
    HERO,
    STANDARD,
    SUBTLE,
    FLOATING_BAR
}

@Immutable
data class GlassStyle(
    val tier: GlassTier,
    val surfaceStyle: SurfaceStyle,
    val blurRadius: Dp,
    val borderWidth: Dp,
    val borderBrush: Brush,
    val borderStroke: BorderStroke,
    val backgroundBrush: Brush,
    val containerColor: Color,
    val specularHighlightBrush: Brush,
    val cornerRadius: Dp,
    val shape: RoundedCornerShape,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val glowHaloColor: Color,
    val glowHaloAlpha: Float
)

/**
 * Multi-style Compose design system supporting Glassmorphism, Claymorphism,
 * Neumorphism, Anime Cyber UI, and Dark Japanese Washi aesthetics across
 * Shinobi Planner screens.
 */
object ThemeDesignSystem {

    val HeroBlurRadius: Dp = 18.dp
    val DefaultBlurRadius: Dp = 12.dp
    val SubtleBlurRadius: Dp = 8.dp
    val FloatingBarBlurRadius: Dp = 20.dp

    val DefaultBorderWidth: Dp = 1.dp
    val ActiveBorderWidth: Dp = 1.4.dp

    val DefaultCornerRadius: Dp = 20.dp
    val CompactCornerRadius: Dp = 14.dp
    val PillCornerRadius: Dp = 16.dp
    val FloatingBarCornerRadius: Dp = 28.dp

    fun blurRadiusFor(
        tier: GlassTier,
        visuals: ShinobiVisualConfig
    ): Dp {
        if (!visuals.glassmorphismEnabled || visuals.batterySaverMode) return 0.dp
        return when (tier) {
            GlassTier.HERO -> HeroBlurRadius
            GlassTier.STANDARD -> DefaultBlurRadius
            GlassTier.SUBTLE -> SubtleBlurRadius
            GlassTier.FLOATING_BAR -> FloatingBarBlurRadius
        }
    }

    fun backdropBlurRadius(
        wallpaperBlurDp: Float,
        visuals: ShinobiVisualConfig
    ): Dp {
        val minBackdropBlur = if (visuals.glassmorphismEnabled && visuals.isDark && !visuals.batterySaverMode) 2f else 0f
        return wallpaperBlurDp.coerceAtLeast(minBackdropBlur).coerceIn(0f, 24f).dp
    }

    fun containerAlphaFor(
        tier: GlassTier,
        visuals: ShinobiVisualConfig,
        surfaceStyle: SurfaceStyle
    ): Float {
        if (!visuals.isDark) {
            return when (surfaceStyle) {
                SurfaceStyle.CLAYMORPHISM, SurfaceStyle.NEUMORPHISM -> 0.98f
                else -> 0.94f
            }
        }
        if (!visuals.glassmorphismEnabled) return 0.97f
        return when (surfaceStyle) {
            SurfaceStyle.CLAYMORPHISM -> when (tier) {
                GlassTier.HERO -> 0.94f
                GlassTier.STANDARD -> 0.90f
                GlassTier.SUBTLE -> 0.86f
                GlassTier.FLOATING_BAR -> 0.92f
            }
            SurfaceStyle.NEUMORPHISM -> when (tier) {
                GlassTier.HERO -> 0.92f
                GlassTier.STANDARD -> 0.88f
                GlassTier.SUBTLE -> 0.82f
                GlassTier.FLOATING_BAR -> 0.92f
            }
            SurfaceStyle.DARK_JAPANESE -> when (tier) {
                GlassTier.HERO -> 0.88f
                GlassTier.STANDARD -> 0.82f
                GlassTier.SUBTLE -> 0.74f
                GlassTier.FLOATING_BAR -> 0.90f
            }
            SurfaceStyle.GLASSMORPHISM, SurfaceStyle.ANIME_CYBER -> when (tier) {
                GlassTier.HERO -> 0.82f
                GlassTier.STANDARD -> 0.74f
                GlassTier.SUBTLE -> 0.64f
                GlassTier.FLOATING_BAR -> 0.88f
            }
        }
    }

    fun glowHaloAlphaFor(
        tier: GlassTier,
        visuals: ShinobiVisualConfig,
        glowActive: Boolean,
        surfaceStyle: SurfaceStyle
    ): Float {
        if (!visuals.isDark || visuals.batterySaverMode) return 0f
        if (surfaceStyle == SurfaceStyle.NEUMORPHISM && !glowActive) return 0.04f
        val intensity = visuals.glowIntensity.coerceIn(0f, 1f)
        if (!visuals.globalGlowEnabled && !glowActive) return 0f
        val styleFactor = when (surfaceStyle) {
            SurfaceStyle.ANIME_CYBER -> 1.15f
            SurfaceStyle.GLASSMORPHISM -> 1.0f
            SurfaceStyle.DARK_JAPANESE -> 0.85f
            SurfaceStyle.CLAYMORPHISM -> 0.75f
            SurfaceStyle.NEUMORPHISM -> 0.65f
        }
        val base = when {
            glowActive && (tier == GlassTier.HERO || tier == GlassTier.FLOATING_BAR) -> 0.25f
            glowActive -> 0.20f
            !visuals.globalGlowEnabled -> 0f
            tier == GlassTier.HERO || tier == GlassTier.FLOATING_BAR -> 0.16f
            tier == GlassTier.STANDARD -> 0.10f
            else -> 0.06f
        }
        return (base * intensity * styleFactor).coerceIn(0f, 0.32f)
    }

    fun borderBrush(
        accentColor: Color,
        visuals: ShinobiVisualConfig,
        surfaceStyle: SurfaceStyle,
        tier: GlassTier = GlassTier.STANDARD,
        glowActive: Boolean = false,
        outlineFallback: Color = CyberBorderGlow
    ): Brush {
        if (!visuals.isDark) {
            return when (surfaceStyle) {
                SurfaceStyle.CLAYMORPHISM, SurfaceStyle.NEUMORPHISM -> Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.95f), outlineFallback.copy(alpha = 0.45f))
                )
                else -> Brush.linearGradient(
                    listOf(accentColor.copy(alpha = if (glowActive) 0.55f else 0.26f), outlineFallback.copy(alpha = 0.58f))
                )
            }
        }

        return when (surfaceStyle) {
            SurfaceStyle.CLAYMORPHISM -> Brush.linearGradient(
                colors = listOf(
                    GlassWhite.copy(alpha = if (glowActive) 0.36f else 0.22f),
                    accentColor.copy(alpha = if (glowActive) 0.48f else 0.22f),
                    Color.Black.copy(alpha = 0.42f)
                )
            )
            SurfaceStyle.NEUMORPHISM -> Brush.linearGradient(
                colors = listOf(
                    NeuLightEdge.copy(alpha = 0.88f),
                    accentColor.copy(alpha = if (glowActive) 0.52f else 0.18f),
                    NeuDarkShadow.copy(alpha = 0.90f)
                )
            )
            SurfaceStyle.DARK_JAPANESE -> Brush.linearGradient(
                colors = listOf(
                    WashiGold.copy(alpha = if (glowActive) 0.58f else 0.28f),
                    accentColor.copy(alpha = if (glowActive) 0.62f else 0.32f),
                    WashiGold.copy(alpha = 0.12f)
                )
            )
            SurfaceStyle.ANIME_CYBER, SurfaceStyle.GLASSMORPHISM -> {
                val glowBoost = if (visuals.globalGlowEnabled) 0.08f * visuals.glowIntensity else 0f
                val edgeAlpha = when {
                    glowActive -> (0.52f + 0.36f * visuals.glowIntensity).coerceAtMost(0.95f)
                    tier == GlassTier.HERO -> (0.38f + 0.26f * visuals.glowIntensity + glowBoost).coerceAtMost(0.80f)
                    tier == GlassTier.FLOATING_BAR -> (0.40f + glowBoost).coerceAtMost(0.72f)
                    else -> (0.24f + 0.18f * visuals.glowIntensity + glowBoost).coerceAtMost(0.58f)
                }
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = if (glowActive) 0.40f else if (visuals.globalGlowEnabled) 0.26f else 0.18f),
                        accentColor.copy(alpha = edgeAlpha),
                        visuals.secondaryAccent.copy(alpha = edgeAlpha * 0.48f),
                        GlassWhite.copy(alpha = 0.09f)
                    )
                )
            }
        }
    }

    fun backgroundBrush(
        visuals: ShinobiVisualConfig,
        surfaceStyle: SurfaceStyle,
        surfaceColor: Color,
        surfaceVariant: Color,
        tier: GlassTier = GlassTier.STANDARD,
        tintColor: Color? = null
    ): Brush {
        val baseAlpha = containerAlphaFor(tier, visuals, surfaceStyle)
        if (!visuals.isDark) {
            val top = tintColor?.copy(alpha = 0.14f) ?: GlassWhite
            val bottom = tintColor?.copy(alpha = 0.07f) ?: surfaceVariant.copy(alpha = 0.55f)
            return Brush.verticalGradient(colors = listOf(top, bottom))
        }

        return when (surfaceStyle) {
            SurfaceStyle.CLAYMORPHISM -> {
                val clayTop = tintColor?.copy(alpha = 0.28f)
                    ?: surfaceVariant.copy(alpha = baseAlpha)
                val clayBottom = tintColor?.copy(alpha = 0.16f)
                    ?: surfaceColor.copy(alpha = (baseAlpha + 0.04f).coerceAtMost(0.98f))
                Brush.linearGradient(colors = listOf(clayTop, clayBottom))
            }
            SurfaceStyle.NEUMORPHISM -> {
                val neuTop = tintColor?.copy(alpha = 0.20f) ?: surfaceVariant.copy(alpha = baseAlpha)
                val neuBottom = tintColor?.copy(alpha = 0.10f) ?: surfaceColor.copy(alpha = baseAlpha)
                Brush.linearGradient(colors = listOf(neuTop, neuBottom))
            }
            SurfaceStyle.DARK_JAPANESE -> {
                val washiTop = tintColor?.copy(alpha = 0.22f) ?: surfaceVariant.copy(alpha = baseAlpha)
                val washiBottom = tintColor?.copy(alpha = 0.10f) ?: surfaceColor.copy(alpha = baseAlpha)
                Brush.verticalGradient(colors = listOf(washiTop, washiBottom))
            }
            SurfaceStyle.GLASSMORPHISM, SurfaceStyle.ANIME_CYBER -> {
                val top = tintColor?.copy(alpha = 0.22f)
                    ?: surfaceVariant.copy(alpha = (baseAlpha + 0.06f).coerceAtMost(0.98f))
                val bottom = tintColor?.copy(alpha = 0.11f)
                    ?: surfaceColor.copy(alpha = baseAlpha)
                Brush.verticalGradient(colors = listOf(top, bottom))
            }
        }
    }

    @Composable
    fun glassStyle(
        accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
        tier: GlassTier = GlassTier.STANDARD,
        surfaceStyle: SurfaceStyle = LocalShinobiVisuals.current.defaultSurfaceStyle,
        glowActive: Boolean = false,
        cornerRadius: Dp = if (surfaceStyle == SurfaceStyle.CLAYMORPHISM) 24.dp else LocalShinobiVisuals.current.cardCornerRadiusDp.dp,
        tintColor: Color? = null
    ): GlassStyle {
        val visuals = LocalShinobiVisuals.current
        val surfaceColor = MaterialTheme.colorScheme.surface
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val outlineColor = MaterialTheme.colorScheme.outline

        return remember(
            visuals,
            accentColor,
            tier,
            surfaceStyle,
            glowActive,
            cornerRadius,
            tintColor,
            surfaceColor,
            surfaceVariant,
            outlineColor
        ) {
            val blur = blurRadiusFor(tier, visuals)
            val width = when {
                glowActive -> ActiveBorderWidth
                surfaceStyle == SurfaceStyle.CLAYMORPHISM || surfaceStyle == SurfaceStyle.NEUMORPHISM -> 1.25.dp
                else -> DefaultBorderWidth
            }
            val bBrush = borderBrush(accentColor, visuals, surfaceStyle, tier, glowActive, outlineColor)
            val bgBrush = backgroundBrush(visuals, surfaceStyle, surfaceColor, surfaceVariant, tier, tintColor)
            val baseAlpha = containerAlphaFor(tier, visuals, surfaceStyle)
            val flatContainer = if (visuals.isDark) {
                tintColor?.copy(alpha = 0.16f) ?: surfaceColor.copy(alpha = baseAlpha)
            } else {
                tintColor?.copy(alpha = 0.11f) ?: GlassWhite.copy(alpha = 0.96f)
            }
            val specularBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    GlassWhite.copy(alpha = if (glowActive) 0.46f else if (surfaceStyle == SurfaceStyle.CLAYMORPHISM) 0.34f else 0.22f),
                    accentColor.copy(alpha = if (glowActive) 0.50f else 0.26f),
                    Color.Transparent
                )
            )
            GlassStyle(
                tier = tier,
                surfaceStyle = surfaceStyle,
                blurRadius = blur,
                borderWidth = width,
                borderBrush = bBrush,
                borderStroke = BorderStroke(width, bBrush),
                backgroundBrush = bgBrush,
                containerColor = flatContainer,
                specularHighlightBrush = specularBrush,
                cornerRadius = cornerRadius,
                shape = RoundedCornerShape(cornerRadius),
                tonalElevation = when (surfaceStyle) {
                    SurfaceStyle.CLAYMORPHISM -> 8.dp
                    SurfaceStyle.NEUMORPHISM -> 6.dp
                    else -> if (visuals.glassmorphismEnabled) 4.dp else 1.dp
                },
                shadowElevation = when {
                    surfaceStyle == SurfaceStyle.CLAYMORPHISM -> 10.dp
                    surfaceStyle == SurfaceStyle.NEUMORPHISM -> 8.dp
                    (glowActive || visuals.globalGlowEnabled) && visuals.isDark -> 8.dp
                    tier == GlassTier.FLOATING_BAR && visuals.isDark -> 12.dp
                    else -> 2.dp
                },
                glowHaloColor = accentColor,
                glowHaloAlpha = glowHaloAlphaFor(tier, visuals, glowActive, surfaceStyle)
            )
        }
    }

    fun Modifier.glassAmbientGlow(style: GlassStyle): Modifier {
        if (style.glowHaloAlpha <= 0.01f) return this
        return this.drawBehind {
            val outerExpand = 8.dp.toPx()
            val innerExpand = 3.dp.toPx()
            val outerRadiusPx = style.cornerRadius.toPx() + outerExpand
            val innerRadiusPx = style.cornerRadius.toPx() + innerExpand

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(style.glowHaloColor.copy(alpha = style.glowHaloAlpha), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = (size.maxDimension * 0.92f).coerceAtLeast(1f)
                ),
                topLeft = Offset(-outerExpand, -outerExpand),
                size = Size(size.width + outerExpand * 2f, size.height + outerExpand * 2f),
                cornerRadius = CornerRadius(outerRadiusPx)
            )

            drawRoundRect(
                color = style.glowHaloColor.copy(alpha = (style.glowHaloAlpha * 0.45f).coerceAtMost(0.18f)),
                topLeft = Offset(-innerExpand, -innerExpand),
                size = Size(size.width + innerExpand * 2f, size.height + innerExpand * 2f),
                cornerRadius = CornerRadius(innerRadiusPx)
            )
        }
    }

    @Composable
    fun GlassLayerBackground(
        style: GlassStyle,
        modifier: Modifier = Modifier,
        showSpecularEdge: Boolean = true
    ) {
        val visuals = LocalShinobiVisuals.current
        val applyBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            style.blurRadius > 0.dp &&
            visuals.isDark &&
            visuals.glassmorphismEnabled

        Box(modifier = modifier.clip(style.shape)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (applyBlur) Modifier.blur(style.blurRadius) else Modifier)
                    .background(style.backgroundBrush)
            )
            if (showSpecularEdge && visuals.isDark && visuals.glassmorphismEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(style.specularHighlightBrush)
                )
            }
        }
    }
}

@Composable
fun Modifier.ShinobiGlassModifier(
    accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
    tier: GlassTier = GlassTier.STANDARD,
    surfaceStyle: SurfaceStyle = LocalShinobiVisuals.current.defaultSurfaceStyle,
    glowActive: Boolean = false,
    cornerRadius: Dp = LocalShinobiVisuals.current.cardCornerRadiusDp.dp,
    tintColor: Color? = null,
    showSpecularEdge: Boolean = true
): Modifier {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = accentColor,
        tier = tier,
        surfaceStyle = surfaceStyle,
        glowActive = glowActive,
        cornerRadius = cornerRadius,
        tintColor = tintColor
    )
    return this.ShinobiGlassModifier(
        style = style,
        showSpecularEdge = showSpecularEdge
    )
}

@Composable
fun Modifier.ShinobiGlassModifier(
    style: GlassStyle,
    showSpecularEdge: Boolean = true
): Modifier {
    val visuals = LocalShinobiVisuals.current
    return with(ThemeDesignSystem) {
        this@ShinobiGlassModifier
            .glassAmbientGlow(style)
            .clip(style.shape)
            .background(brush = style.backgroundBrush, shape = style.shape)
            .drawWithContent {
                drawContent()
                if (showSpecularEdge && visuals.isDark && (visuals.glassmorphismEnabled || style.surfaceStyle == SurfaceStyle.CLAYMORPHISM)) {
                    val edgeHeight = if (style.surfaceStyle == SurfaceStyle.CLAYMORPHISM) 1.5.dp.toPx() else 1.dp.toPx()
                    drawRect(
                        brush = style.specularHighlightBrush,
                        topLeft = Offset.Zero,
                        size = Size(size.width, edgeHeight)
                    )
                }
            }
            .border(border = style.borderStroke, shape = style.shape)
    }
}
