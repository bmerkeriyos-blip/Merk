package com.example.ui.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.RankUpCelebrationOverlay
import com.example.ui.components.ShinobiWallpaperBackdrop
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.missions.MissionsScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.ui.theme.ThemeDesignSystem
import com.example.ui.theme.ThemePreset
import com.example.viewmodel.FocusTimerViewModel
import com.example.viewmodel.PlannerUiState
import com.example.viewmodel.PlannerViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ScheduleRoute
@Serializable data object MissionsRoute
@Serializable data object ProgressRoute
@Serializable data object SettingsRoute

private data class TopLevelTab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val tag: String
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab("Home", Icons.Filled.Home, HomeRoute, "nav_home"),
    TopLevelTab("Schedule", Icons.Filled.CalendarMonth, ScheduleRoute, "nav_schedule"),
    TopLevelTab("Missions", Icons.Filled.TrackChanges, MissionsRoute, "nav_missions"),
    TopLevelTab("Progress", Icons.Filled.Insights, ProgressRoute, "nav_progress"),
    TopLevelTab("Settings", Icons.Filled.Settings, SettingsRoute, "nav_settings")
)

@Composable
fun ShinobiAppShell(
    plannerViewModel: PlannerViewModel,
    focusViewModel: FocusTimerViewModel
) {
    val uiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()

    val settings = uiState.settings
    val profile = uiState.profile

    val preset = remember(settings.themePreset) {
        ThemePreset.fromStorage(settings.themePreset)
    }
    val appearance = remember(settings.appearanceMode) {
        runCatching { AppearanceMode.valueOf(settings.appearanceMode) }.getOrDefault(AppearanceMode.DARK)
    }

    var unlockedThisSession by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            plannerViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(focusState.lastCompletedMessage) {
        val msg = focusState.lastCompletedMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            focusViewModel.clearLastMessage()
        }
    }

    ShinobiPlannerTheme(
        preset = preset,
        appearanceMode = appearance,
        accentKey = settings.accentColorKey,
        glassmorphism = settings.glassmorphism,
        globalGlowEnabled = settings.globalGlowEnabled,
        glowIntensity = settings.glowIntensity,
        animationsEnabled = settings.animationsEnabled,
        reduceMotion = settings.reduceMotion,
        batterySaver = settings.batterySaverMode,
        fontScale = settings.fontScale
    ) {
        when {
            settings.appLockEnabled && settings.appLockPin.length == 4 && !unlockedThisSession -> {
                AppLockGateScreen(
                    expectedPin = settings.appLockPin,
                    biometricsEnabled = settings.biometricsEnabled,
                    onUnlocked = { unlockedThisSession = true }
                )
            }

            profile != null && !profile.onboardingCompleted -> {
                OnboardingScreen(
                    profile = profile,
                    initialThemePreset = preset.name,
                    onComplete = { name, univ, dept, year, tz, use24h, showEthTime, pName, initialWallRes ->
                        plannerViewModel.completeOnboarding(
                            displayName = name,
                            university = univ,
                            department = dept,
                            academicYear = year,
                            timezoneId = tz,
                            use24HourClock = use24h,
                            showEthiopianTime = showEthTime,
                            themePreset = pName,
                            initialWallpaperResName = initialWallRes
                        )
                    }
                )
            }

            else -> {
                MainDojoScaffold(
                    uiState = uiState,
                    plannerViewModel = plannerViewModel,
                    focusViewModel = focusViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainDojoScaffold(
    uiState: PlannerUiState,
    plannerViewModel: PlannerViewModel,
    focusViewModel: FocusTimerViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val visuals = LocalShinobiVisuals.current

    val audioState by plannerViewModel.audioState.collectAsStateWithLifecycle()
    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()

    var openScheduleCreateFlag by remember { mutableStateOf(false) }
    var missionsInitialTab by remember { mutableIntStateOf(0) }

    val screenTarget = when {
        currentDest?.hasRoute<HomeRoute>() == true -> "HOME"
        currentDest?.hasRoute<ScheduleRoute>() == true -> "SCHEDULE"
        currentDest?.hasRoute<MissionsRoute>() == true -> "MISSIONS"
        currentDest?.hasRoute<ProgressRoute>() == true -> "PROGRESS"
        currentDest?.hasRoute<SettingsRoute>() == true -> "SETTINGS"
        else -> "HOME"
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        ShinobiWallpaperBackdrop(
            wallpapers = uiState.wallpapers,
            settings = uiState.settings,
            screenTarget = screenTarget
        )

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                val railStyle = ThemeDesignSystem.glassStyle(
                    accentColor = visuals.primaryAccent,
                    tier = GlassTier.FLOATING_BAR,
                    cornerRadius = 24.dp
                )
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                        .ShinobiGlassModifier(style = railStyle, showSpecularEdge = false),
                    shape = railStyle.shape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = railStyle.tonalElevation,
                    shadowElevation = railStyle.shadowElevation
                ) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        contentColor = visuals.primaryAccent
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = when (tab.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is ScheduleRoute -> currentDest?.hasRoute<ScheduleRoute>() == true
                                is MissionsRoute -> currentDest?.hasRoute<MissionsRoute>() == true
                                is ProgressRoute -> currentDest?.hasRoute<ProgressRoute>() == true
                                is SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        FloatingGlassBottomNavBar(
                            tabs = TOP_LEVEL_TABS,
                            isSelected = { tab ->
                                when (tab.route) {
                                    is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                    is ScheduleRoute -> currentDest?.hasRoute<ScheduleRoute>() == true
                                    is MissionsRoute -> currentDest?.hasRoute<MissionsRoute>() == true
                                    is ProgressRoute -> currentDest?.hasRoute<ProgressRoute>() == true
                                    is SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                            },
                            onSelect = { tab -> navigateTopLevel(tab.route) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        enterTransition = {
                            if (!visuals.animationsEnabled) EnterTransition.None
                            else fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(220))
                        },
                        exitTransition = {
                            if (!visuals.animationsEnabled) ExitTransition.None
                            else fadeOut(tween(180))
                        }
                    ) {
                        composable<HomeRoute>(
                            enterTransition = {
                                if (!visuals.animationsEnabled) EnterTransition.None
                                else fadeIn(tween(240)) + slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    tween(240, easing = FastOutSlowInEasing)
                                )
                            }
                        ) {
                            DashboardScreen(
                                uiState = uiState,
                                focusState = focusState,
                                onToggleTask = { plannerViewModel.toggleTaskCompleted(it) },
                                onQuickFocusToggle = {
                                    focusViewModel.toggleStartPause(uiState.settings.musicDuringFocus)
                                },
                                onOpenScheduleAdd = {
                                    openScheduleCreateFlag = true
                                    navigateTopLevel(ScheduleRoute)
                                },
                                onOpenScheduleView = {
                                    navigateTopLevel(ScheduleRoute)
                                },
                                onOpenFocusDojo = {
                                    missionsInitialTab = 0
                                    navigateTopLevel(MissionsRoute)
                                },
                                onOpenProgress = {
                                    navigateTopLevel(ProgressRoute)
                                },
                                onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                onSaveWidgetsOrder = { csv ->
                                    plannerViewModel.updateSettings { it.copy(dashboardWidgetsCsv = csv) }
                                }
                            )
                        }

                        composable<ScheduleRoute>(
                            // Home <-> Schedule: Chakra-inspired surge
                            enterTransition = {
                                if (!visuals.animationsEnabled) EnterTransition.None
                                else fadeIn(tween(240)) +
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Start,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                            },
                            exitTransition = {
                                if (!visuals.animationsEnabled) ExitTransition.None
                                else fadeOut(tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180))
                            }
                        ) {
                            ScheduleScreen(
                                uiState = uiState,
                                openCreateInitially = openScheduleCreateFlag,
                                onConsumedCreateFlag = { openScheduleCreateFlag = false },
                                detectConflicts = { plannerViewModel.detectConflicts(it) },
                                findFreeTimeSlots = { plannerViewModel.findFreeTimeSlots(it) },
                                onSaveTask = { task, subs -> plannerViewModel.saveTask(task, subs) },
                                onToggleTask = { plannerViewModel.toggleTaskCompleted(it) },
                                onToggleSubtask = { plannerViewModel.toggleSubtask(it) },
                                onDeleteTask = { plannerViewModel.deleteTask(it) },
                                onDuplicateTask = { plannerViewModel.duplicateTask(it) },
                                onMoveTaskUp = { t, list -> plannerViewModel.moveTaskUp(t, list) },
                                onSaveSubject = { plannerViewModel.saveSubject(it) },
                                onDeleteSubject = { plannerViewModel.deleteSubject(it) }
                            )
                        }

                        composable<MissionsRoute>(
                            // Schedule -> Missions: Smooth anime vertical reveal
                            enterTransition = {
                                if (!visuals.animationsEnabled) EnterTransition.None
                                else fadeIn(tween(240)) + slideInVertically(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 12 }
                                )
                            },
                            exitTransition = {
                                if (!visuals.animationsEnabled) ExitTransition.None
                                else fadeOut(tween(180)) + slideOutVertically(
                                    animationSpec = tween(180),
                                    targetOffsetY = { -it / 16 }
                                )
                            }
                        ) {
                            MissionsScreen(
                                uiState = uiState,
                                focusState = focusState,
                                audioState = audioState,
                                initialTabIndex = missionsInitialTab,
                                onSelectFocusMode = { focusViewModel.selectMode(it) },
                                onSetCustomMinutes = { focusViewModel.setCustomMinutes(it) },
                                onSelectFocusSubject = { focusViewModel.selectSubject(it) },
                                onToggleFocusStartPause = {
                                    focusViewModel.toggleStartPause(uiState.settings.musicDuringFocus)
                                },
                                onStopAndSaveFocus = { focusViewModel.stopAndSaveEarly() },
                                onResetFocus = { focusViewModel.resetTimer() },
                                onToggleDistractionFree = { focusViewModel.toggleDistractionFree() },
                                onToggleKeepAwake = { focusViewModel.toggleKeepScreenAwake() },
                                onToggleMusic = { plannerViewModel.togglePlayPauseMusic() },
                                onNextTrack = { plannerViewModel.nextTrack() },
                                onToggleTask = { plannerViewModel.toggleTaskCompleted(it) },
                                onSaveNote = { plannerViewModel.saveNote(it) },
                                onDeleteNote = { plannerViewModel.deleteNote(it) }
                            )
                        }

                        composable<ProgressRoute>(
                            // Missions -> Progress: Subtle cinematic zoom/particle dissolve
                            enterTransition = {
                                if (!visuals.animationsEnabled) EnterTransition.None
                                else fadeIn(tween(250)) + scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                )
                            },
                            exitTransition = {
                                if (!visuals.animationsEnabled) ExitTransition.None
                                else fadeOut(tween(180)) + scaleOut(targetScale = 1.03f, animationSpec = tween(180))
                            }
                        ) {
                            ProgressScreen(
                                uiState = uiState,
                                onSaveTask = { task, subs -> plannerViewModel.saveTask(task, subs) }
                            )
                        }

                        composable<SettingsRoute>(
                            // Any -> Settings: Naruto-inspired leaf/chakra cross-slide
                            enterTransition = {
                                if (!visuals.animationsEnabled) EnterTransition.None
                                else fadeIn(tween(240)) +
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                            },
                            exitTransition = {
                                if (!visuals.animationsEnabled) ExitTransition.None
                                else fadeOut(tween(180)) +
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        tween(180)
                                    )
                            }
                        ) {
                            SettingsScreen(
                                uiState = uiState,
                                audioState = audioState,
                                onUpdateProfile = { plannerViewModel.updateProfile(it) },
                                onUpdateSettings = { plannerViewModel.updateSettings(it) },
                                onSelectBuiltinWallpaper = { resName, scr ->
                                    plannerViewModel.selectBuiltinWallpaper(resName, scr)
                                },
                                onAddWallpaper = { uri, scr -> plannerViewModel.addImportedWallpaper(uri, scr) },
                                onUpdateWallpaperTuning = { w, br, bl, ov, pr ->
                                    plannerViewModel.updateWallpaperTuning(w, br, bl, ov, pr)
                                },
                                onDeleteWallpaper = { plannerViewModel.deleteWallpaper(it) },
                                onResetWallpapers = { plannerViewModel.resetWallpapers() },
                                onAddTrack = { title, uri, mot -> plannerViewModel.addImportedTrack(title, uri, mot) },
                                onDeleteTrack = { plannerViewModel.deleteTrack(it) },
                                onPlayTrackAt = { plannerViewModel.playTrackAt(it) },
                                onTogglePlayPauseMusic = { plannerViewModel.togglePlayPauseMusic() },
                                onNextTrack = { plannerViewModel.nextTrack() },
                                onPreviousTrack = { plannerViewModel.previousTrack() },
                                onToggleShuffle = { plannerViewModel.toggleShuffle() },
                                onCycleRepeat = { plannerViewModel.cycleRepeatMode() },
                                onSpeakMotivation = { plannerViewModel.speakMotivation(it) },
                                onPlaySfx = { plannerViewModel.playSfx(it) },
                                onExportJson = { plannerViewModel.exportJson() },
                                onExportCsv = { plannerViewModel.exportCsv() },
                                onWritePdf = { uri -> plannerViewModel.writePdfReport(context, uri) },
                                onResetLocalData = { plannerViewModel.resetLocalData() }
                            )
                        }
                    }

                    // Rank-up / Mission Complete Celebration Banner
                    RankUpCelebrationOverlay(
                        message = uiState.celebrationMessage,
                        onDismiss = { plannerViewModel.clearCelebration() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassBottomNavBar(
    tabs: List<TopLevelTab>,
    isSelected: (TopLevelTab) -> Boolean,
    onSelect: (TopLevelTab) -> Unit
) {
    val visuals = LocalShinobiVisuals.current
    val barStyle = ThemeDesignSystem.glassStyle(
        accentColor = visuals.primaryAccent,
        tier = GlassTier.FLOATING_BAR,
        glowActive = true,
        cornerRadius = ThemeDesignSystem.FloatingBarCornerRadius
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .ShinobiGlassModifier(style = barStyle),
            shape = barStyle.shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = barStyle.tonalElevation,
            shadowElevation = barStyle.shadowElevation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = isSelected(tab)
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = if (visuals.animationsEnabled) spring(dampingRatio = 0.65f) else snap(),
                        label = "nav_icon_scale_${tab.tag}"
                    )
                    val tintColor by animateColorAsState(
                        targetValue = if (selected) {
                            visuals.primaryAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = if (visuals.animationsEnabled) tween(200) else snap(),
                        label = "nav_tint_${tab.tag}"
                    )

                    val itemShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(itemShape)
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            listOf(
                                                visuals.primaryAccent.copy(alpha = 0.22f),
                                                visuals.secondaryAccent.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(tab) }
                            )
                            .testTag(tab.tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tintColor,
                                modifier = Modifier
                                    .size(21.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tintColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .clip(CircleShape)
                                        .background(visuals.primaryAccent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockGateScreen(
    expectedPin: String,
    biometricsEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun launchBiometrics() {
        val activity = context.findFragmentActivity() ?: return
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorText = errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Shinobi Planner")
            .setSubtitle("Verify your shinobi seal to unlock")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(biometricsEnabled) {
        if (biometricsEnabled) {
            launchBiometrics()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        HoloCard(
            glassTier = GlassTier.HERO,
            glowActive = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = ChakraOrange, modifier = Modifier.size(40.dp))
                Text("SHINOBI SEAL LOCKED", style = MaterialTheme.typography.titleLarge, color = CyberTextPrimary)
                OutlinedTextField(
                    value = entered,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            entered = it
                            errorText = null
                            if (it == expectedPin) onUnlocked()
                            else if (it.length == 4) {
                                errorText = "Incorrect 4-digit PIN"
                                entered = ""
                            }
                        }
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
                )
                if (errorText != null) {
                    Text(errorText.orEmpty(), color = CrimsonAlert, style = MaterialTheme.typography.bodySmall)
                }
                if (biometricsEnabled) {
                    OutlinedButton(onClick = { launchBiometrics() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Unlock with Fingerprint / Biometrics")
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
