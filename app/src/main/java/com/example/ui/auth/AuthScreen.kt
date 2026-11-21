package com.example.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipForNow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

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
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(ChakraOrange.copy(alpha = 0.34f * crestAlpha), GoldRankS.copy(alpha = 0.12f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shinobi_avatar),
                        contentDescription = "Shinobi Planner Crest",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SHINOBI PLANNER",
                    style = MaterialTheme.typography.displaySmall,
                    color = CyberTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "HIDDEN LEAF STUDY DOJO",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChakraOrange,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Master your classes, focus training, exams, and daily missions on your path to Kage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HoloSurface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureRow(Icons.Filled.Schedule, GoldRankS, "Smart Class Scroll & Conflict Detection")
                    FeatureRow(Icons.Filled.AutoAwesome, ChakraOrange, "Gemini Shinobi Sensei & Timetable Scanner")
                    FeatureRow(Icons.Filled.School, JadeEmerald, "Genin to Kage Missions, XP & Focus Dojo")
                    FeatureRow(Icons.Filled.Lock, ArcaneViolet, "Offline-First Room Storage & Optional Cloud Sync")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.16f)),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f))
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
                        containerColor = ChakraOrange,
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
                    border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.6f)),
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
