package com.example.ui.theme

import android.os.Build
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

enum class GlassTier {
    HERO,
    STANDARD,
    SUBTLE,
    FLOATING_BAR
}

@Immutable
data class GlassStyle(
    val tier: GlassTier,
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
 * Centralized Compose design system defining reusable glassmorphism styling parameters
 * (blur radius, border stroke, translucent dark background, corner geometry, global glow,
 * and luminous highlights) applied consistently across all Shinobi Planner surfaces and cards.
 */
object ThemeDesignSystem {

    // Core Glassmorphism Tokens
    val HeroBlurRadius: Dp = 18.dp
    val DefaultBlurRadius: Dp = 12.dp
    val SubtleBlurRadius: Dp = 8.dp
    val FloatingBarBlurRadius: Dp = 20.dp

    val DefaultBorderWidth: Dp = 1.dp
    val ActiveBorderWidth: Dp = 1.35.dp

    val DefaultCornerRadius: Dp = 20.dp
    val CompactCornerRadius: Dp = 14.dp
    val PillCornerRadius: Dp = 16.dp
    val FloatingBarCornerRadius: Dp = 28.dp

    // Translucent Dark Background Palette Tokens
    val TranslucentDarkBackground: Color = DeepObsidian.copy(alpha = 0.76f)
    val TranslucentHeroBackground: Color = HoloSurfaceElevated.copy(alpha = 0.84f)
    val TranslucentSubtleBackground: Color = MidnightNavy.copy(alpha = 0.64f)
    val TranslucentAmoledBackground: Color = AmoledCard.copy(alpha = 0.84f)

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
        visuals: ShinobiVisualConfig
    ): Float {
        if (!visuals.isDark) return 0.95f
        if (!visuals.glassmorphismEnabled) return 0.98f
        return when (tier) {
            GlassTier.HERO -> 0.82f
            GlassTier.STANDARD -> 0.74f
            GlassTier.SUBTLE -> 0.64f
            GlassTier.FLOATING_BAR -> 0.88f
        }
    }

    fun glowHaloAlphaFor(
        tier: GlassTier,
        visuals: ShinobiVisualConfig,
        glowActive: Boolean
    ): Float {
        if (!visuals.isDark || visuals.batterySaverMode) return 0f
        val intensity = visuals.glowIntensity.coerceIn(0f, 1f)
        if (!visuals.globalGlowEnabled && !glowActive) return 0f
        val base = when {
            glowActive && (tier == GlassTier.HERO || tier == GlassTier.FLOATING_BAR) -> 0.25f
            glowActive -> 0.20f
            !visuals.globalGlowEnabled -> 0f
            tier == GlassTier.HERO || tier == GlassTier.FLOATING_BAR -> 0.16f
            tier == GlassTier.STANDARD -> 0.10f
            GlassTier.SUBTLE == tier -> 0.06f
            else -> 0.08f
        }
        return (base * intensity).coerceIn(0f, 0.30f)
    }

    fun borderStroke(
        accentColor: Color,
        visuals: ShinobiVisualConfig,
        tier: GlassTier = GlassTier.STANDARD,
        glowActive: Boolean = false,
        outlineFallback: Color = CyberBorderGlow
    ): BorderStroke {
        val width = if (glowActive) ActiveBorderWidth else DefaultBorderWidth
        val brush = borderBrush(
            accentColor = accentColor,
            visuals = visuals,
            tier = tier,
            glowActive = glowActive,
            outlineFallback = outlineFallback
        )
        return BorderStroke(width, brush)
    }

    fun borderBrush(
        accentColor: Color,
        visuals: ShinobiVisualConfig,
        tier: GlassTier = GlassTier.STANDARD,
        glowActive: Boolean = false,
        outlineFallback: Color = CyberBorderGlow
    ): Brush {
        return if (visuals.isDark) {
            val glowBoost = if (visuals.globalGlowEnabled) 0.08f * visuals.glowIntensity else 0f
            val edgeAlpha = when {
                glowActive -> (0.50f + 0.36f * visuals.glowIntensity).coerceAtMost(0.94f)
                tier == GlassTier.HERO -> (0.36f + 0.26f * visuals.glowIntensity + glowBoost).coerceAtMost(0.78f)
                tier == GlassTier.FLOATING_BAR -> (0.38f + glowBoost).coerceAtMost(0.70f)
                else -> (0.22f + 0.18f * visuals.glowIntensity + glowBoost).coerceAtMost(0.56f)
            }
            Brush.linearGradient(
                colors = listOf(
                    GlassWhite.copy(alpha = if (glowActive) 0.38f else if (visuals.globalGlowEnabled) 0.24f else 0.18f),
                    accentColor.copy(alpha = edgeAlpha),
                    accentColor.copy(alpha = edgeAlpha * 0.42f),
                    GlassWhite.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (glowActive) 0.55f else 0.26f),
                    outlineFallback.copy(alpha = 0.60f)
                )
            )
        }
    }

    fun backgroundBrush(
        visuals: ShinobiVisualConfig,
        surfaceColor: Color,
        tier: GlassTier = GlassTier.STANDARD,
        tintColor: Color? = null
    ): Brush {
        val baseAlpha = containerAlphaFor(tier, visuals)
        val topColor = if (visuals.isDark) {
            tintColor?.copy(alpha = 0.22f) ?: HoloSurfaceElevated.copy(alpha = (baseAlpha + 0.06f).coerceAtMost(0.98f))
        } else {
            tintColor?.copy(alpha = 0.14f) ?: GlassWhite
        }
        val bottomColor = if (visuals.isDark) {
            tintColor?.copy(alpha = 0.11f) ?: surfaceColor.copy(alpha = baseAlpha)
        } else {
            tintColor?.copy(alpha = 0.08f) ?: AcademicSurfaceVariant.copy(alpha = 0.45f)
        }
        return Brush.verticalGradient(colors = listOf(topColor, bottomColor))
    }

    @Composable
    fun glassStyle(
        accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
        tier: GlassTier = GlassTier.STANDARD,
        glowActive: Boolean = false,
        cornerRadius: Dp = LocalShinobiVisuals.current.cardCornerRadiusDp.dp,
        tintColor: Color? = null
    ): GlassStyle {
        val visuals = LocalShinobiVisuals.current
        val surfaceColor = MaterialTheme.colorScheme.surface
        val outlineColor = MaterialTheme.colorScheme.outline

        return remember(visuals, accentColor, tier, glowActive, cornerRadius, tintColor, surfaceColor, outlineColor) {
            val blur = blurRadiusFor(tier, visuals)
            val width = if (glowActive) ActiveBorderWidth else DefaultBorderWidth
            val bBrush = borderBrush(accentColor, visuals, tier, glowActive, outlineColor)
            val bgBrush = backgroundBrush(visuals, surfaceColor, tier, tintColor)
            val baseAlpha = containerAlphaFor(tier, visuals)
            val flatContainer = if (visuals.isDark) {
                tintColor?.copy(alpha = 0.16f) ?: surfaceColor.copy(alpha = baseAlpha)
            } else {
                tintColor?.copy(alpha = 0.11f) ?: GlassWhite.copy(alpha = 0.96f)
            }
            val specularBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    GlassWhite.copy(alpha = if (glowActive) 0.44f else if (visuals.globalGlowEnabled) 0.24f else 0.16f),
                    accentColor.copy(alpha = if (glowActive) 0.50f else if (visuals.globalGlowEnabled) 0.28f else 0.18f),
                    Color.Transparent
                )
            )
            GlassStyle(
                tier = tier,
                blurRadius = blur,
                borderWidth = width,
                borderBrush = bBrush,
                borderStroke = BorderStroke(width, bBrush),
                backgroundBrush = bgBrush,
                containerColor = flatContainer,
                specularHighlightBrush = specularBrush,
                cornerRadius = cornerRadius,
                shape = RoundedCornerShape(cornerRadius),
                tonalElevation = if (visuals.glassmorphismEnabled) 4.dp else 1.dp,
                shadowElevation = if ((glowActive || visuals.globalGlowEnabled) && visuals.isDark) 8.dp else if (tier == GlassTier.FLOATING_BAR && visuals.isDark) 12.dp else 2.dp,
                glowHaloColor = accentColor,
                glowHaloAlpha = glowHaloAlphaFor(tier, visuals, glowActive)
            )
        }
    }

    /**
     * Draws a two-stage ambient chakra outer halo when [GlassStyle.glowHaloAlpha] > 0.
     */
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

    /**
     * Reusable frosted dark glass layer positioned behind card content so foreground text
     * and icons remain sharp while honoring Android 12+ (API 31+) RenderEffect blur radius.
     */
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

/**
 * Jetpack Compose [Modifier] extension applying [ThemeDesignSystem] glassmorphism styling
 * (ambient global glow, rounded clip, translucent gradient background, crisp top specular
 * reflection, and holographic border stroke) directly to existing `Surface` and `Card` elements
 * while keeping child text and icons sharp.
 */
@Composable
fun Modifier.ShinobiGlassModifier(
    accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
    tier: GlassTier = GlassTier.STANDARD,
    glowActive: Boolean = false,
    cornerRadius: Dp = LocalShinobiVisuals.current.cardCornerRadiusDp.dp,
    tintColor: Color? = null,
    showSpecularEdge: Boolean = true
): Modifier {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = accentColor,
        tier = tier,
        glowActive = glowActive,
        cornerRadius = cornerRadius,
        tintColor = tintColor
    )
    return this.ShinobiGlassModifier(
        style = style,
        showSpecularEdge = showSpecularEdge
    )
}

/**
 * Overload applying a precomputed [GlassStyle] from [ThemeDesignSystem.glassStyle].
 */
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
                if (showSpecularEdge && visuals.isDark && visuals.glassmorphismEnabled) {
                    drawRect(
                        brush = style.specularHighlightBrush,
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1.dp.toPx())
                    )
                }
            }
            .border(border = style.borderStroke, shape = style.shape)
    }
}
