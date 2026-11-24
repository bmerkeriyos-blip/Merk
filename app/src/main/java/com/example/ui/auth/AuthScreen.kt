package com.example.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem

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

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoTried by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!autoTried) {
            autoTried = true
            attemptAutoSignIn(
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
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_ninja_village),
            contentDescription = "Shinobi Hidden Village Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.45f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightNavy.copy(alpha = 0.65f),
                            MidnightNavy.copy(alpha = 0.90f),
                            MidnightNavy
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NarutoOrange.copy(alpha = 0.36f * crestAlpha),
                                    ChakraCyan.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shinobi_avatar),
                        contentDescription = "Shinobi Planner Crest",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SHINOBI PLANNER",
                    style = MaterialTheme.typography.displaySmall,
                    color = CyberTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FUTURISTIC HIDDEN LEAF STUDY COMMAND",
                    style = MaterialTheme.typography.titleSmall,
                    color = NarutoOrange,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Master your classes, focus training, exams, and daily missions on your path to Kage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            HoloCard(
                accentColor = NarutoOrange,
                glassTier = GlassTier.HERO,
                glowActive = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureRow(Icons.Filled.Schedule, ChakraCyan, "Smart Class Scroll & Conflict Detection")
                    FeatureRow(Icons.Filled.AutoAwesome, HolographicPurple, "Gemini Shinobi Sensei & Timetable Scanner")
                    FeatureRow(Icons.Filled.School, NarutoOrange, "Genin to Kage Missions, XP & Focus Dojo")
                    FeatureRow(Icons.Filled.Lock, ChakraGreen, "Offline-First Room Storage & Optional Cloud Sync")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            .padding(bottom = 12.dp)
                            .ShinobiGlassModifier(style = errorStyle, showSpecularEdge = false),
                        shape = errorStyle.shape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
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
                        .height(56.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NarutoOrange,
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
                        Text("OPENING SHINOBI SCROLL...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = "SIGN IN WITH GOOGLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSkipForNow,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("skip_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ChakraCyan.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary)
                ) {
                    Text(
                        text = "SKIP FOR NOW",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Google Sign-In is optional. AI Shinobi Sensei, Timetable Scanner, Missions, and Focus Dojo work without signing in—sign in anytime for cloud backup.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = CyberTextPrimary
        )
    }
}
