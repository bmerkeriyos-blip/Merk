package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.ai.AiPersonality
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlin.math.cos
import kotlin.math.sin

private val AUTH_THEME_OPTIONS = listOf(
    ThemePreset.NARUTO,
    ThemePreset.CYBER_SHINOBI,
    ThemePreset.ACADEMIC,
    ThemePreset.MINIMAL
)

private val AUTH_TRACK_OPTIONS = listOf(
    "Genin • Year 1",
    "Chunin • Year 2",
    "Jonin • Year 3",
    "ANBU • Exam Prep"
)

private fun accentForPreset(preset: ThemePreset): Pair<Color, Color> = when (preset) {
    ThemePreset.NARUTO -> NarutoOrange to ChakraCyan
    ThemePreset.CYBER_SHINOBI -> ChakraCyan to HolographicPurple
    ThemePreset.ACADEMIC -> GoldRankS to ChakraCyan
    ThemePreset.MINIMAL -> ChakraGreen to ChakraCyan
    ThemePreset.AMOLED -> NarutoOrange to GoldRankS
    ThemePreset.CUSTOM -> NarutoOrange to ChakraCyan
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipForNow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }

    val savedDefaults = remember(context) { ShinobiLoginPersonalizationStore.loadLastOrDefault(context) }
    var callsign by remember { mutableStateOf(savedDefaults.callsign) }
    var selectedPreset by remember { mutableStateOf(savedDefaults.themePreset) }
    var selectedPersonality by remember { mutableStateOf(savedDefaults.aiPersonality) }
    var selectedTrack by remember { mutableStateOf(savedDefaults.academicTrack) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoTried by remember { mutableStateOf(false) }
    var autoSignInJob by remember { mutableStateOf<Job?>(null) }
    var activeSignInJob by remember { mutableStateOf<Job?>(null) }

    val (targetPrimary, targetSecondary) = accentForPreset(selectedPreset)
    val primaryAccent by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = spring(),
        label = "auth_primary_accent"
    )
    val secondaryAccent by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = spring(),
        label = "auth_secondary_accent"
    )

    fun persistPersonalization() {
        ShinobiLoginPersonalizationStore.savePending(
            context = context,
            personalization = ShinobiLoginPersonalization(
                callsign = callsign.trim(),
                themePreset = selectedPreset,
                aiPersonality = selectedPersonality,
                academicTrack = selectedTrack
            )
        )
    }

    LaunchedEffect(Unit) {
        if (!autoTried) {
            autoTried = true
            autoSignInJob = attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "crest_pulse")
    val crestAlpha by pulseTransition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .testTag("auth_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_ninja_village),
            contentDescription = "Shinobi Hidden Village Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.42f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightNavy.copy(alpha = 0.58f),
                            MidnightNavy.copy(alpha = 0.88f),
                            MidnightNavy
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Status Pill + Shinobi Crest Hero
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = primaryAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "FIREBASE AUTH • SHINOBI DOJO GATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .drawBehind {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = size.minDimension * 0.48f
                                drawCircle(
                                    color = primaryAccent.copy(alpha = 0.35f * crestAlpha),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = secondaryAccent.copy(alpha = 0.25f * crestAlpha),
                                    radius = outerRadius * 0.84f,
                                    center = center,
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                                for (i in 0 until 8) {
                                    val angle = Math.toRadians(i * 45.0)
                                    val dotX = center.x + (outerRadius * cos(angle)).toFloat()
                                    val dotY = center.y + (outerRadius * sin(angle)).toFloat()
                                    drawCircle(
                                        color = if (i % 2 == 0) primaryAccent else secondaryAccent,
                                        radius = 2.5.dp.toPx(),
                                        center = Offset(dotX, dotY)
                                    )
                                }
                            }
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        primaryAccent.copy(alpha = 0.34f * crestAlpha),
                                        secondaryAccent.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shinobi_avatar),
                            contentDescription = "Shinobi Planner Crest",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(id = R.string.auth_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = CyberTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.auth_subtitle),
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryAccent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.auth_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Personalization Scroll Card
                HoloCard(
                    accentColor = primaryAccent,
                    glassTier = GlassTier.HERO,
                    glowActive = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.auth_personalize_header),
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = callsign,
                            onValueChange = { callsign = it.take(40) },
                            label = { Text(stringResource(id = R.string.auth_callsign_label)) },
                            placeholder = { Text(stringResource(id = R.string.auth_callsign_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = secondaryAccent
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_callsign_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryAccent,
                                unfocusedBorderColor = secondaryAccent.copy(alpha = 0.45f),
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary,
                                focusedLabelColor = primaryAccent,
                                unfocusedLabelColor = CyberTextSecondary
                            )
                        )

                        // Academy Track Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel(
                                icon = Icons.Filled.School,
                                tint = secondaryAccent,
                                text = stringResource(id = R.string.auth_track_label)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AUTH_TRACK_OPTIONS.forEach { track ->
                                    val selected = selectedTrack == track
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedTrack = track },
                                        label = {
                                            Text(
                                                text = track,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag("auth_track_${track.substringBefore(" ").lowercase()}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = secondaryAccent.copy(alpha = 0.22f),
                                            selectedLabelColor = CyberTextPrimary,
                                            labelColor = CyberTextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = CyberTextSecondary.copy(alpha = 0.35f),
                                            selectedBorderColor = secondaryAccent
                                        )
                                    )
                                }
                            }
                        }

                        // Village Chakra Theme Selector (Live previews on screen!)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel(
                                icon = Icons.Filled.Palette,
                                tint = primaryAccent,
                                text = stringResource(id = R.string.auth_theme_label)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AUTH_THEME_OPTIONS.forEach { preset ->
                                    val selected = selectedPreset == preset
                                    val (swatchPrimary, _) = accentForPreset(preset)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) swatchPrimary.copy(alpha = 0.20f) else DeepObsidian.copy(alpha = 0.65f),
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) swatchPrimary else CyberTextSecondary.copy(alpha = 0.28f)
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedPreset = preset }
                                            .testTag("auth_theme_${preset.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchPrimary)
                                            )
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) CyberTextPrimary else CyberTextSecondary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Selected theme",
                                                    tint = swatchPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AI Sensei Mentor Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel(
                                icon = Icons.Filled.Psychology,
                                tint = HolographicPurple,
                                text = stringResource(id = R.string.auth_mentor_label)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AiPersonality.entries.forEach { persona ->
                                    val selected = selectedPersonality == persona
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedPersonality = persona },
                                        label = {
                                            Text(
                                                text = persona.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag("auth_mentor_${persona.code.lowercase()}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = HolographicPurple.copy(alpha = 0.24f),
                                            selectedLabelColor = CyberTextPrimary,
                                            labelColor = CyberTextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = CyberTextSecondary.copy(alpha = 0.35f),
                                            selectedBorderColor = HolographicPurple
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DeepObsidian.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, HolographicPurple.copy(alpha = 0.32f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_shinobi_avatar),
                                        contentDescription = selectedPersonality.label,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selectedPersonality.label} • ${selectedPersonality.ninjaRank}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = primaryAccent,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "\"${selectedPersonality.motto}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CyberTextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cloud Benefits Strip
                HoloCard(
                    accentColor = secondaryAccent,
                    glassTier = GlassTier.STANDARD,
                    glowActive = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PerkBadge(
                            icon = Icons.Filled.CloudSync,
                            tint = secondaryAccent,
                            title = "Firestore Cloud Sync",
                            subtitle = "Cross-device backup",
                            modifier = Modifier.weight(1f)
                        )
                        PerkBadge(
                            icon = Icons.Filled.Lock,
                            tint = ChakraGreen,
                            title = "Offline-First Dojo",
                            subtitle = "Room local storage",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Anchored Bottom Auth Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { msg ->
                        val errorStyle = ThemeDesignSystem.glassStyle(
                            accentColor = CrimsonAlert,
                            tier = GlassTier.SUBTLE,
                            glowActive = true,
                            cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                            tintColor = CrimsonAlert
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .ShinobiGlassModifier(style = errorStyle, showSpecularEdge = false)
                                .testTag("auth_error_banner"),
                            shape = errorStyle.shape,
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent,
                                contentColor = CyberTextPrimary
                            ),
                            border = null
                        ) {
                            Text(
                                text = msg,
                                color = CyberTextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        errorMessage = null
                        persistPersonalization()
                        autoSignInJob?.cancel()
                        autoSignInJob = null
                        activeSignInJob?.cancel()
                        isLoading = true
                        activeSignInJob = onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent,
                        contentColor = MidnightNavy
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MidnightNavy,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.auth_opening_scroll),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.auth_sign_in_cta),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        persistPersonalization()
                        onSkipForNow()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, secondaryAccent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary)
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_continue_offline),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.auth_optional_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = CyberTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PerkBadge(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = CyberTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = CyberTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
