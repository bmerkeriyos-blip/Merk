package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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

    LaunchedEffect(targetWallpapers, settings.wallpaperSlideshowEnabled, settings.wallpaperSlideshowIntervalSec, settings.wallpaperRandomOrder) {
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
    val blurDp = (active?.blurRadius ?: 2f).coerceIn(0f, 24f).dp
    val brightness = (active?.brightness ?: 0.72f).coerceIn(0.15f, 1f)
    val overlayOpacity = (active?.overlayOpacity ?: 0.65f).coerceIn(0.15f, 0.92f)
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
                .alpha(brightness * 0.55f)
                .then(if (blurDp > 0.dp) Modifier.blur(blurDp) else Modifier)

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

            // Readability overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = overlayOpacity * 0.85f),
                                MaterialTheme.colorScheme.background.copy(alpha = overlayOpacity),
                                MaterialTheme.colorScheme.background.copy(alpha = (overlayOpacity + 0.18f).coerceAtMost(0.97f))
                            )
                        )
                    )
            )
        }

        if (visuals.showParticles) {
            ChakraParticleLayer(accentColor = visuals.primaryAccent, secondaryColor = visuals.secondaryAccent)
        }
    }
}

@Composable
private fun ChakraParticleLayer(accentColor: Color, secondaryColor: Color) {
    val infinite = rememberInfiniteTransition(label = "chakra_particles")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )
    val seeds = remember {
        List(14) { i ->
            Triple(
                (i * 0.173f) % 1f,
                (i * 0.291f) % 1f,
                1.8f + (i % 3)
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        seeds.forEachIndexed { idx, (sx, sy, radiusDp) ->
            val yFrac = (sy - phase + 1f) % 1f
            val drift = sin((phase * 2f * PI + idx).toDouble()).toFloat() * 18f
            val x = (sx * size.width + drift).coerceIn(0f, size.width)
            val y = yFrac * size.height
            val col = if (idx % 3 == 0) secondaryColor else accentColor
            val r = radiusDp.dp.toPx()
            drawCircle(
                color = col.copy(alpha = 0.09f),
                radius = r * 2.2f,
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
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val shape = RoundedCornerShape(visuals.cardCornerRadiusDp.dp)
    val container = if (visuals.glassmorphismEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = accentColor.copy(alpha = 0.18f + 0.30f * visuals.glowIntensity)

    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (visuals.glassmorphismEnabled) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
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
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (visuals.animationsEnabled) tween(700, easing = FastOutSlowInEasing) else snap(),
        label = "ring_progress"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = accentColor.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
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
        animationSpec = if (visuals.animationsEnabled) spring(dampingRatio = 0.6f) else snap(),
        label = "shuriken_spin"
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        val fill = if (checked) JadeEmerald else accentColor.copy(alpha = 0.25f)
        val stroke = if (checked) JadeEmerald else accentColor
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
        MissionRank.B -> ArcaneViolet
        MissionRank.C -> NeonCyan
        MissionRank.D -> JadeEmerald
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f))
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

@Composable
fun FloatingAiOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalShinobiVisuals.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val pulse = rememberInfiniteTransition(label = "orb_pulse")
    val scale by pulse.animateFloat(
        initialValue = if (visuals.animationsEnabled) 0.94f else 1f,
        targetValue = if (visuals.animationsEnabled) 1.07f else 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "orb_scale"
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
            .size((58 * scale).dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        visuals.primaryAccent,
                        visuals.tertiaryAccent
                    )
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("floating_ai_orb"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "Open Shinobi AI Quick Assistant",
            tint = MidnightNavy,
            modifier = Modifier.size(28.dp)
        )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("celebration_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HoloSurfaceElevated),
                border = BorderStroke(1.5.dp, GoldRankS)
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
