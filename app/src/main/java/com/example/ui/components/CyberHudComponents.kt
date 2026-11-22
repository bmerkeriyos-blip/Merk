package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.MissionRank
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import com.example.ui.theme.AcademicSurfaceVariant
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceElevated
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.JadeEmerald
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

typealias GlassTier = com.example.ui.theme.GlassTier

@Composable
fun ShinobiWallpaperBackdrop(
    wallpapers: List<WallpaperItemEntity>,
    settings: UserSettingsEntity,
    screenTarget: String,
    scrollParallaxPx: Float = 0f,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current

    val targetWallpapers = remember(wallpapers, screenTarget) {
        val specific = wallpapers.filter { it.screenTarget.equals(screenTarget, ignoreCase = true) }
        if (specific.isNotEmpty()) specific
        else wallpapers.filter { it.screenTarget.equals("GLOBAL", ignoreCase = true) }
    }

    var slideshowIndex by remember(targetWallpapers, settings.wallpaperSlideshowEnabled) { mutableIntStateOf(0) }

    LaunchedEffect(
        targetWallpapers,
        settings.wallpaperSlideshowEnabled,
        settings.wallpaperSlideshowIntervalSec,
        settings.wallpaperRandomOrder
    ) {
        if (settings.wallpaperSlideshowEnabled && targetWallpapers.size > 1) {
            while (true) {
                delay(settings.wallpaperSlideshowIntervalSec.coerceAtLeast(5) * 1000L)
                slideshowIndex = if (settings.wallpaperRandomOrder) {
                    Random.nextInt(targetWallpapers.size)
                } else {
                    (slideshowIndex + 1) % targetWallpapers.size
                }
            }
        }
    }

    val active = targetWallpapers.getOrNull(slideshowIndex % targetWallpapers.size.coerceAtLeast(1))
    val blurDp = ThemeDesignSystem.backdropBlurRadius(active?.blurRadius ?: 3f, visuals)
    val brightness = (active?.brightness ?: 0.70f).coerceIn(0.15f, 1f)
    val overlayOpacity = (active?.overlayOpacity ?: 0.68f).coerceIn(0.15f, 0.94f)
    val parallaxOffsetY = if (active?.parallaxEnabled != false && visuals.animationsEnabled) {
        (scrollParallaxPx * 0.15f).coerceIn(-60f, 60f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (visuals.preset != ThemePreset.AMOLED && visuals.preset != ThemePreset.MINIMAL) {
            val isBuiltIn = active == null ||
                active.builtInResName.equals("bg_ninja_village", ignoreCase = true) ||
                active.uriString.startsWith("builtin://")

            val imageModifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, parallaxOffsetY.roundToInt()) }
                .alpha(if (visuals.isDark) brightness * 0.50f else brightness * 0.16f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurDp > 0.dp) {
                        Modifier.blur(blurDp)
                    } else {
                        Modifier
                    }
                )

            if (isBuiltIn) {
                Image(
                    painter = painterResource(id = R.drawable.bg_ninja_village),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
            } else {
                AsyncImage(
                    model = active.uriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
            }

            // Readability overlay gradient protecting UI typography
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = overlayOpacity * 0.88f),
                                MaterialTheme.colorScheme.background.copy(alpha = overlayOpacity),
                                MaterialTheme.colorScheme.background.copy(alpha = (overlayOpacity + 0.22f).coerceAtMost(0.98f))
                            )
                        )
                    )
            )
        }

        // Futuristic global ambient radial aura & subtle screen-edge glow behind glass cards
        if (visuals.isDark && visuals.preset != ThemePreset.MINIMAL && !visuals.batterySaverMode) {
            val primaryGlow = visuals.primaryAccent
            val secondaryGlow = visuals.secondaryAccent
            val tertiaryGlow = visuals.tertiaryAccent
            val globalBoost = if (visuals.globalGlowEnabled) 1.35f else 0.45f
            val strength = (visuals.glowIntensity.coerceIn(0.1f, 1f) * globalBoost).coerceIn(0.1f, 1.35f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow.copy(alpha = 0.16f * strength), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.10f),
                        radius = size.maxDimension * 0.45f
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.88f, size.height * 0.10f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow.copy(alpha = 0.12f * strength), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.45f),
                        radius = size.maxDimension * 0.40f
                    ),
                    radius = size.maxDimension * 0.40f,
                    center = Offset(size.width * 0.12f, size.height * 0.45f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiaryGlow.copy(alpha = 0.10f * strength), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.84f),
                        radius = size.maxDimension * 0.38f
                    ),
                    radius = size.maxDimension * 0.38f,
                    center = Offset(size.width * 0.78f, size.height * 0.84f)
                )
                if (visuals.globalGlowEnabled) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryGlow.copy(alpha = (0.07f * visuals.glowIntensity).coerceIn(0f, 0.10f)),
                                Color.Transparent,
                                secondaryGlow.copy(alpha = (0.06f * visuals.glowIntensity).coerceIn(0f, 0.09f))
                            )
                        )
                    )
                }
            }
        }

        if (visuals.showParticles) {
            ChakraParticleLayer(
                accentColor = visuals.primaryAccent,
                secondaryColor = visuals.secondaryAccent,
                tertiaryColor = visuals.tertiaryAccent
            )
        }
    }
}

@Composable
private fun ChakraParticleLayer(
    accentColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    val infinite = rememberInfiniteTransition(label = "chakra_particles")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )
    val seeds = remember {
        List(18) { i ->
            Triple(
                (i * 0.167f + 0.05f) % 1f,
                (i * 0.283f + 0.09f) % 1f,
                1.4f + (i % 3) * 0.9f
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        seeds.forEachIndexed { idx, (sx, sy, radiusDp) ->
            val yFrac = (sy - phase + 1f) % 1f
            val drift = sin((phase * 2.0 * PI + idx.toDouble())).toFloat() * 20f
            val x = (sx * size.width + drift).coerceIn(0f, size.width)
            val y = yFrac * size.height
            val col = when (idx % 4) {
                0 -> secondaryColor
                1 -> tertiaryColor
                else -> accentColor
            }
            val r = radiusDp.dp.toPx()
            drawCircle(
                color = col.copy(alpha = 0.08f),
                radius = r * 2.8f,
                center = Offset(x, y)
            )
            drawCircle(
                color = col.copy(alpha = 0.24f),
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun HoloCard(
    modifier: Modifier = Modifier,
    accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
    glassTier: GlassTier = GlassTier.STANDARD,
    glowActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val style = ThemeDesignSystem.glassStyle(
        accentColor = accentColor,
        tier = glassTier,
        glowActive = glowActive
    )

    Surface(
        modifier = modifier
            .ShinobiGlassModifier(style = style)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = style.shape,
        color = Color.Transparent,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun XpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    startColor: Color = NarutoOrange,
    endColor: Color = GoldRankS,
    height: Dp = 9.dp
) {
    val visuals = LocalShinobiVisuals.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (visuals.animationsEnabled) tween(750, easing = FastOutSlowInEasing) else snap(),
        label = "xp_bar_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(
                if (visuals.isDark) GlassWhite.copy(alpha = 0.08f) else startColor.copy(alpha = 0.14f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceAtLeast(0.03f))
                .height(height)
                .clip(RoundedCornerShape(height))
                .background(Brush.horizontalGradient(colors = listOf(startColor, endColor)))
        )
    }
}

@Composable
fun ChakraProgressRing(
    progress: Float,
    centerTitle: String,
    centerSubtitle: String,
    size: Dp = 112.dp,
    strokeWidth: Dp = 10.dp,
    accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
    isAnimating: Boolean = false,
    showCompletionPulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (visuals.animationsEnabled) tween(700, easing = FastOutSlowInEasing) else snap(),
        label = "ring_progress"
    )

    val infinite = rememberInfiniteTransition(label = "chakra_ring_aura")
    val auraRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotation"
    )
    val pulseRadiusFraction by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 6.dp.toPx()
            val arcSize = Size(
                (this.size.width - inset * 2f).coerceAtLeast(10f),
                (this.size.height - inset * 2f).coerceAtLeast(10f)
            )
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            if (isAnimating && visuals.animationsEnabled) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f * pulseRadiusFraction),
                            Color.Transparent
                        ),
                        center = center,
                        radius = (this.size.minDimension / 2f) * pulseRadiusFraction
                    ),
                    radius = (this.size.minDimension / 2f) * pulseRadiusFraction,
                    center = center
                )
            }

            if (showCompletionPulse && visuals.animationsEnabled) {
                drawCircle(
                    color = ChakraGreen.copy(alpha = 0.35f),
                    radius = (this.size.minDimension / 2f) * pulseRadiusFraction,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Track ring
            drawArc(
                color = accentColor.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Subtle rotating chakra energy shimmer when active
            if (isAnimating && visuals.animationsEnabled) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.45f),
                            GlassWhite.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    startAngle = auraRotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx * 0.45f, cap = StrokeCap.Round)
                )
            }

            // Main progress arc glow underlay
            drawArc(
                color = accentColor.copy(alpha = 0.22f * visuals.glowIntensity),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx * 1.45f, cap = StrokeCap.Round)
            )

            // Main crisp progress sweep
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(accentColor, visuals.secondaryAccent, accentColor)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = centerSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShurikenCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    accentColor: Color = LocalShinobiVisuals.current.primaryAccent,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    val rotation by animateFloatAsState(
        targetValue = if (checked) 180f else 0f,
        animationSpec = if (visuals.animationsEnabled) spring(dampingRatio = 0.58f) else snap(),
        label = "shuriken_spin"
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        val fill = if (checked) ChakraGreen else accentColor.copy(alpha = 0.20f)
        val stroke = if (checked) ChakraGreen else accentColor
        Canvas(
            modifier = Modifier
                .size(26.dp)
                .rotate(rotation)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.minDimension / 2f
            val inner = outer * 0.34f
            val path = Path().apply {
                for (i in 0 until 4) {
                    val angle = (i * 90.0) * PI / 180.0
                    val nextAngle = ((i * 90.0) + 45.0) * PI / 180.0
                    val ox = cx + (outer * cos(angle)).toFloat()
                    val oy = cy + (outer * sin(angle)).toFloat()
                    val ix = cx + (inner * cos(nextAngle)).toFloat()
                    val iy = cy + (inner * sin(nextAngle)).toFloat()
                    if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                    lineTo(ix, iy)
                }
                close()
            }
            drawPath(path, color = fill)
            drawPath(path, color = stroke, style = Stroke(width = 2.dp.toPx()))
            drawCircle(
                color = if (checked) MidnightNavy else stroke,
                radius = outer * 0.16f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun RankBadgeChip(rankCode: String, modifier: Modifier = Modifier) {
    val rank = MissionRank.fromCode(rankCode)
    val badgeColor = when (rank) {
        MissionRank.S -> GoldRankS
        MissionRank.A -> CrimsonAlert
        MissionRank.B -> HolographicPurple
        MissionRank.C -> ChakraCyan
        MissionRank.D -> ChakraGreen
    }
    val style = ThemeDesignSystem.glassStyle(
        accentColor = badgeColor,
        tier = GlassTier.SUBTLE,
        glowActive = true,
        cornerRadius = 8.dp,
        tintColor = badgeColor
    )
    Surface(
        modifier = modifier.ShinobiGlassModifier(style = style, showSpecularEdge = false),
        shape = style.shape,
        color = Color.Transparent
    ) {
        Text(
            text = rank.label,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingAiOrb(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val infinite = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infinite.animateFloat(
        initialValue = if (visuals.animationsEnabled) 0.95f else 1f,
        targetValue = if (visuals.animationsEnabled) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb_scale"
    )
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_ring_rotation"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(-650f, 100f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(-900f, 120f)
                }
            }
            .size(66.dp),
        contentAlignment = Alignment.Center
    ) {
        // Holographic orbital chakra ring + orbiting particles
        if (visuals.animationsEnabled) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
            ) {
                val r = size.minDimension / 2f - 3.dp.toPx()
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HolographicPurple.copy(alpha = 0.30f),
                            NarutoOrange.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = c,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f,
                    center = c
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ChakraCyan.copy(alpha = 0.85f), Color.Transparent, HolographicPurple.copy(alpha = 0.85f), Color.Transparent)
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // Orbiting mini chakra nodes
                drawCircle(
                    color = NarutoOrange,
                    radius = 2.6.dp.toPx(),
                    center = Offset(c.x + r, c.y)
                )
                drawCircle(
                    color = ChakraCyan,
                    radius = 2.2.dp.toPx(),
                    center = Offset(c.x - r, c.y)
                )
            }
        }

        Box(
            modifier = Modifier
                .size((54 * scale).dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NarutoOrange,
                            HolographicPurple
                        )
                    )
                )
                .border(1.5.dp, GlassWhite.copy(alpha = 0.72f), CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("floating_ai_orb"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Open Shinobi AI Mentor (Hold for Quick Launcher)",
                tint = GlassWhite,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun RankUpCelebrationOverlay(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(3200L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        val style = ThemeDesignSystem.glassStyle(
            accentColor = GoldRankS,
            tier = GlassTier.HERO,
            glowActive = true
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .ShinobiGlassModifier(style = style)
                    .testTag("celebration_banner"),
                shape = style.shape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GoldRankS,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldRankS,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss celebration",
                            tint = CyberTextSecondary
                        )
                    }
                }
            }
        }
    }
}
