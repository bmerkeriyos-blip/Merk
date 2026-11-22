package com.example.ui.navigation

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.core.ai.AiPersonality
import com.example.ui.ai.AiMentorScreen
import com.example.ui.ai.TimetableScannerScreen
import com.example.ui.components.FloatingAiOrb
import com.example.ui.components.HoloCard
import com.example.ui.components.RankUpCelebrationOverlay
import com.example.ui.components.ShinobiWallpaperBackdrop
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.missions.MissionsScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.*
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.FocusTimerViewModel
import com.example.viewmodel.PlannerUiState
import com.example.viewmodel.PlannerViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ScheduleRoute
@Serializable data object MissionsRoute
@Serializable data object ProgressRoute
@Serializable data object SettingsRoute
@Serializable data object AiMentorRoute
@Serializable data object ScannerRoute

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShinobiAppShell(
    plannerViewModel: PlannerViewModel,
    focusViewModel: FocusTimerViewModel,
    aiViewModel: AiViewModel,
    isSignedInWithGoogle: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val uiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
    val audioState by plannerViewModel.audioState.collectAsStateWithLifecycle()
    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()
    val chats by aiViewModel.chats.collectAsStateWithLifecycle()
    val activeChatId by aiViewModel.activeChatId.collectAsStateWithLifecycle()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val chatState by aiViewModel.chatState.collectAsStateWithLifecycle()
    val scannerState by aiViewModel.scannerState.collectAsStateWithLifecycle()
    val scansHistory by aiViewModel.scansHistory.collectAsStateWithLifecycle()
    val dailySuggestion by aiViewModel.dailySuggestion.collectAsStateWithLifecycle()
    val pendingDelete by aiViewModel.pendingDeleteConfirmation.collectAsStateWithLifecycle()

    val settings = uiState.settings
    val profile = uiState.profile

    val preset = remember(settings.themePreset) {
        runCatching { ThemePreset.valueOf(settings.themePreset) }.getOrDefault(ThemePreset.NARUTO)
    }
    val appearance = remember(settings.appearanceMode) {
        runCatching { AppearanceMode.valueOf(settings.appearanceMode) }.getOrDefault(AppearanceMode.DARK)
    }

    var unlockedThisSession by remember { mutableStateOf(false) }

    // Surface Snackbars for user status updates
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            plannerViewModel.clearStatusMessage()
        }
    }

    // Celebrate focus timer completions via Planner HUD
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
                    onComplete = { name, univ, dept, year, tz, use24h, showEthTime, pName, aiPers ->
                        plannerViewModel.completeOnboarding(
                            displayName = name,
                            university = univ,
                            department = dept,
                            academicYear = year,
                            timezoneId = tz,
                            use24HourClock = use24h,
                            showEthiopianTime = showEthTime,
                            themePreset = pName,
                            aiPersonality = aiPers
                        )
                    }
                )
            }

            else -> {
                MainAuthenticatedScaffold(
                    uiState = uiState,
                    plannerViewModel = plannerViewModel,
                    focusViewModel = focusViewModel,
                    aiViewModel = aiViewModel,
                    snackbarHostState = snackbarHostState,
                    isSignedInWithGoogle = isSignedInWithGoogle,
                    onSignInWithGoogle = onSignInWithGoogle,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainAuthenticatedScaffold(
    uiState: PlannerUiState,
    plannerViewModel: PlannerViewModel,
    focusViewModel: FocusTimerViewModel,
    aiViewModel: AiViewModel,
    snackbarHostState: SnackbarHostState,
    isSignedInWithGoogle: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val visuals = LocalShinobiVisuals.current

    val audioState by plannerViewModel.audioState.collectAsStateWithLifecycle()
    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()
    val chats by aiViewModel.chats.collectAsStateWithLifecycle()
    val activeChatId by aiViewModel.activeChatId.collectAsStateWithLifecycle()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val chatState by aiViewModel.chatState.collectAsStateWithLifecycle()
    val scannerState by aiViewModel.scannerState.collectAsStateWithLifecycle()
    val scansHistory by aiViewModel.scansHistory.collectAsStateWithLifecycle()
    val dailySuggestion by aiViewModel.dailySuggestion.collectAsStateWithLifecycle()
    val pendingDelete by aiViewModel.pendingDeleteConfirmation.collectAsStateWithLifecycle()

    var openScheduleCreateFlag by remember { mutableStateOf(false) }
    var missionsInitialTab by remember { mutableIntStateOf(0) }
    var showQuickAiSheet by remember { mutableStateOf(false) }

    val screenTarget = when {
        currentDest?.hasRoute<HomeRoute>() == true -> "HOME"
        currentDest?.hasRoute<ScheduleRoute>() == true -> "SCHEDULE"
        currentDest?.hasRoute<MissionsRoute>() == true -> "MISSIONS"
        currentDest?.hasRoute<AiMentorRoute>() == true -> "AI"
        currentDest?.hasRoute<ProgressRoute>() == true -> "PROFILE"
        else -> "GLOBAL"
    }

    val isTopLevel = currentDest?.hasRoute<AiMentorRoute>() != true &&
        currentDest?.hasRoute<ScannerRoute>() != true

    val imeVisible = WindowInsets.isImeVisible
    val showFloatingOrb = uiState.settings.aiEnabled &&
        uiState.settings.floatingAiEnabled &&
        isTopLevel &&
        !(uiState.settings.floatingAiHideWhileTyping && imeVisible) &&
        !(uiState.settings.floatingAiDisableInFocus && currentDest?.hasRoute<MissionsRoute>() == true && focusState.isRunning)

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
            if (useRail && isTopLevel) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    contentColor = visuals.primaryAccent
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
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

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail && isTopLevel) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            tonalElevation = 8.dp
                        ) {
                            TOP_LEVEL_TABS.forEach { tab ->
                                val selected = when (tab.route) {
                                    is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                    is ScheduleRoute -> currentDest?.hasRoute<ScheduleRoute>() == true
                                    is MissionsRoute -> currentDest?.hasRoute<MissionsRoute>() == true
                                    is ProgressRoute -> currentDest?.hasRoute<ProgressRoute>() == true
                                    is SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
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
                            if (visuals.animationsEnabled) {
                                fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220))
                            } else EnterTransition.None
                        },
                        exitTransition = {
                            if (visuals.animationsEnabled) fadeOut(tween(180)) else ExitTransition.None
                        },
                        popEnterTransition = {
                            if (visuals.animationsEnabled) fadeIn(tween(200)) else EnterTransition.None
                        },
                        popExitTransition = {
                            if (visuals.animationsEnabled) fadeOut(tween(180)) else ExitTransition.None
                        }
                    ) {
                        composable<HomeRoute> {
                            DashboardScreen(
                                uiState = uiState,
                                focusState = focusState,
                                dailySuggestion = dailySuggestion,
                                onRefreshSuggestion = {
                                    aiViewModel.refreshDailySuggestion(uiState.tasks, uiState.settings.timezoneId)
                                },
                                onToggleTask = { plannerViewModel.toggleTaskCompleted(it) },
                                onQuickFocusToggle = {
                                    focusViewModel.toggleStartPause(uiState.settings.musicDuringFocus)
                                },
                                onOpenScheduleAdd = {
                                    openScheduleCreateFlag = true
                                    navigateTopLevel(ScheduleRoute)
                                },
                                onOpenFocusDojo = {
                                    missionsInitialTab = 0
                                    navigateTopLevel(MissionsRoute)
                                },
                                onOpenAiMentor = { navController.navigate(AiMentorRoute) },
                                onOpenScanner = { navController.navigate(ScannerRoute) },
                                onSaveWidgetsOrder = { csv ->
                                    plannerViewModel.updateSettings { it.copy(dashboardWidgetsCsv = csv) }
                                }
                            )
                        }

                        composable<ScheduleRoute> {
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
                                onDeleteSubject = { plannerViewModel.deleteSubject(it) },
                                onOpenScanner = { navController.navigate(ScannerRoute) }
                            )
                        }

                        composable<MissionsRoute> {
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

                        composable<ProgressRoute> {
                            ProgressScreen(
                                uiState = uiState,
                                onSaveTask = { task, subs -> plannerViewModel.saveTask(task, subs) }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                uiState = uiState,
                                audioState = audioState,
                                isSignedInWithGoogle = isSignedInWithGoogle,
                                onSignInWithGoogle = onSignInWithGoogle,
                                onUpdateProfile = { plannerViewModel.updateProfile(it) },
                                onUpdateSettings = { plannerViewModel.updateSettings(it) },
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
                                onTriggerSync = { plannerViewModel.triggerSyncNow(context) },
                                onExportJson = { plannerViewModel.exportJson() },
                                onExportCsv = { plannerViewModel.exportCsv() },
                                onWritePdf = { uri -> plannerViewModel.writePdfReport(context, uri) },
                                onSignOut = onSignOut,
                                onDeleteAccountData = {
                                    plannerViewModel.deleteAccountData(onDeleteAccount)
                                }
                            )
                        }

                        composable<AiMentorRoute> {
                            AiMentorScreen(
                                chats = chats,
                                activeChatId = activeChatId,
                                messages = messages,
                                chatState = chatState,
                                currentPersonalityCode = uiState.settings.aiPersonality,
                                tasks = uiState.tasks,
                                timezoneId = uiState.settings.timezoneId,
                                pendingDelete = pendingDelete,
                                onBack = { navController.popBackStack() },
                                onSelectPersonality = { p ->
                                    plannerViewModel.updateSettings { it.copy(aiPersonality = p.code) }
                                },
                                onSelectOrCreateChat = { p -> aiViewModel.selectOrCreateChat(p) },
                                onStartNewChat = { p -> aiViewModel.startNewChat(p) },
                                onSelectChat = { id -> aiViewModel.selectChat(id) },
                                onDeleteChat = { id -> aiViewModel.deleteChat(id) },
                                onSendMessage = { text, p, tasks, tz ->
                                    aiViewModel.sendMessage(text, p, tasks, tz)
                                },
                                onConfirmPendingDelete = { aiViewModel.confirmPendingDelete() },
                                onDismissPendingDelete = { aiViewModel.dismissPendingDelete() },
                                onOpenScanner = { navController.navigate(ScannerRoute) }
                            )
                        }

                        composable<ScannerRoute> {
                            TimetableScannerScreen(
                                scannerState = scannerState,
                                scansHistory = scansHistory,
                                autoSaveScans = uiState.settings.autoSaveScans,
                                timezoneId = uiState.settings.timezoneId,
                                use24HourClock = uiState.settings.use24HourClock,
                                onBack = { navController.popBackStack() },
                                onScanUri = { uri, src, acc, auto ->
                                    aiViewModel.scanFromImageUri(
                                        context = context,
                                        uri = uri,
                                        sourceType = src,
                                        accurateMode = acc,
                                        autoSave = auto,
                                        timezoneId = uiState.settings.timezoneId
                                    )
                                },
                                onUpdateEvent = { i, ev -> aiViewModel.updateScannedEvent(i, ev) },
                                onRemoveEvent = { i -> aiViewModel.removeScannedEvent(i) },
                                onConfirmSave = { src, acc -> aiViewModel.confirmAndSaveScannedEvents(src, acc) },
                                onResetScanner = { aiViewModel.resetScanner() }
                            )
                        }
                    }

                    // Rank-up / Mission Complete Celebration Banner
                    RankUpCelebrationOverlay(
                        message = uiState.celebrationMessage,
                        onDismiss = { plannerViewModel.clearCelebration() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Draggable Floating AI Orb
                    if (showFloatingOrb) {
                        FloatingAiOrb(
                            onClick = { showQuickAiSheet = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showQuickAiSheet) {
        var quickPrompt by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showQuickAiSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = visuals.primaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHINOBI QUICK AI LAUNCHER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = quickPrompt,
                    onValueChange = { quickPrompt = it },
                    label = { Text("Quick mission command (e.g. Study Algorithms at 5 PM)") },
                    modifier = Modifier.fillMaxWidth().testTag("quick_ai_sheet_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (quickPrompt.isNotBlank()) {
                                val p = AiPersonality.fromCode(uiState.settings.aiPersonality)
                                aiViewModel.sendMessage(quickPrompt, p, uiState.tasks, uiState.settings.timezoneId)
                            }
                            showQuickAiSheet = false
                            navController.navigate(AiMentorRoute)
                        },
                        modifier = Modifier.weight(1f).testTag("quick_ai_go_mentor")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (quickPrompt.isBlank()) "Open Mentor Chat" else "Send & Open Chat")
                    }
                    OutlinedButton(
                        onClick = {
                            showQuickAiSheet = false
                            navController.navigate(ScannerRoute)
                        },
                        modifier = Modifier.weight(1f).testTag("quick_ai_go_scanner")
                    ) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Timetable")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        val activity = context as? FragmentActivity ?: return
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
                            else if (it.length == 4) errorText = "Incorrect PIN"
                        }
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
                )
                errorText?.let {
                    Text(it, color = CrimsonAlert, style = MaterialTheme.typography.bodySmall)
                }
                if (biometricsEnabled) {
                    OutlinedButton(onClick = { launchBiometrics() }) {
                        Text("Use Biometrics")
                    }
                }
            }
        }
    }
}
