package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.AiPersonality
import com.example.core.media.AudioPlaybackState
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import com.example.ui.components.HoloCard
import com.example.ui.theme.*
import com.example.viewmodel.PlannerUiState

@Composable
fun SettingsScreen(
    uiState: PlannerUiState,
    audioState: AudioPlaybackState,
    isSignedInWithGoogle: Boolean,
    onSignInWithGoogle: () -> Unit,
    onUpdateProfile: (UserProfileEntity) -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onAddWallpaper: (String, String) -> Unit,
    onUpdateWallpaperTuning: (WallpaperItemEntity, Float, Float, Float, Boolean) -> Unit,
    onDeleteWallpaper: (String) -> Unit,
    onResetWallpapers: () -> Unit,
    onAddTrack: (String, String, Boolean) -> Unit,
    onDeleteTrack: (String) -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onTogglePlayPauseMusic: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSpeakMotivation: (String) -> Unit,
    onTriggerSync: () -> Unit,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onWritePdf: (Uri) -> Boolean,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit
) {
    val context = LocalContext.current
    val visuals = LocalShinobiVisuals.current
    val settings = uiState.settings
    val profile = uiState.profile

    var showProfileDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showMusicDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showApkGuideDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(onExportJson().toByteArray()) }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(onExportCsv().toByteArray()) }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) onWritePdf(uri)
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "SHINOBI COMMAND SETTINGS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. Account & Export
        item {
            HoloCard(accentColor = visuals.primaryAccent) {
                SectionTitle(Icons.Filled.AccountCircle, "Account & Cloud Sync", visuals.primaryAccent)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSignedInWithGoogle) {
                        "${profile?.displayName ?: "Scholar"} (${profile?.email ?: ""})"
                    } else {
                        "${profile?.displayName ?: "Shinobi Scholar"} • Local Dojo Mode"
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(profile?.university?.ifBlank { "University not set" } ?: "University not set")
                        append(" • ")
                        append(profile?.ninjaTitle ?: "")
                        if (!isSignedInWithGoogle) append(" • AI enabled without sign-in")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { if (profile != null) showProfileDialog = true },
                        modifier = Modifier.weight(1f).testTag("edit_profile_button")
                    ) {
                        Text("Edit Profile")
                    }
                    if (isSignedInWithGoogle) {
                        Button(
                            onClick = onTriggerSync,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.weight(1f).testTag("sync_now_button")
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isSyncing) "Syncing..." else "Sync Now")
                        }
                    } else {
                        Button(
                            onClick = onSignInWithGoogle,
                            modifier = Modifier.weight(1f).testTag("settings_google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChakraOrange,
                                contentColor = MidnightNavy
                            )
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign in Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Export Study Archive", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    AssistChip(
                        onClick = { jsonExportLauncher.launch("shinobi_planner_backup.json") },
                        label = { Text("JSON") },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { csvExportLauncher.launch("shinobi_missions.csv") },
                        label = { Text("CSV") },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { pdfExportLauncher.launch("shinobi_training_report.pdf") },
                        label = { Text("PDF Report") },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (isSignedInWithGoogle) {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f).testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f).testTag("delete_account_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSignedInWithGoogle) "Erase Data" else "Reset Local Data")
                    }
                }
            }
        }

        // 2. Appearance & Theme Presets
        item {
            HoloCard(accentColor = ChakraOrange) {
                SectionTitle(Icons.Filled.Palette, "Appearance & Theme Presets", ChakraOrange)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemePreset.entries) { preset ->
                        FilterChip(
                            selected = settings.themePreset == preset.name,
                            onClick = { onUpdateSettings { it.copy(themePreset = preset.name) } },
                            label = { Text(preset.label) },
                            modifier = Modifier.testTag("theme_preset_${preset.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mode", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppearanceMode.entries) { mode ->
                        FilterChip(
                            selected = settings.appearanceMode == mode.name,
                            onClick = { onUpdateSettings { it.copy(appearanceMode = mode.name) } },
                            label = { Text(mode.label) }
                        )
                    }
                }
                if (settings.themePreset == ThemePreset.CUSTOM.name) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Custom Accent Color", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("CYAN", "ORANGE", "VIOLET", "EMERALD", "GOLD", "CRIMSON")) { key ->
                            FilterChip(
                                selected = settings.accentColorKey == key,
                                onClick = { onUpdateSettings { it.copy(accentColorKey = key) } },
                                label = { Text(key) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggle("Glassmorphic Cards", settings.glassmorphism) { b ->
                    onUpdateSettings { it.copy(glassmorphism = b) }
                }
                Text("Font Scale: ${String.format("%.2fx", settings.fontScale)}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.fontScale,
                    onValueChange = { v -> onUpdateSettings { it.copy(fontScale = v) } },
                    valueRange = 0.85f..1.30f
                )
            }
        }

        // 3. Wallpapers & Audio Quick Studios
        item {
            HoloCard(accentColor = ArcaneViolet) {
                SectionTitle(Icons.Filled.Wallpaper, "Wallpapers & Audio Studios", ArcaneViolet)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showWallpaperDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("open_wallpaper_studio")
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wallpapers")
                    }
                    Button(
                        onClick = { showMusicDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("open_music_studio")
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Music & SFX")
                    }
                }
            }
        }

        // 4. AI Mentor & Floating Assistant
        item {
            HoloCard(accentColor = JadeEmerald) {
                SectionTitle(Icons.Filled.AutoAwesome, "AI Mentor & Floating Orb", JadeEmerald)
                SettingToggle("Enable AI Shinobi Sensei", settings.aiEnabled) { b ->
                    onUpdateSettings { it.copy(aiEnabled = b) }
                }
                SettingToggle("Show Floating AI Orb", settings.floatingAiEnabled) { b ->
                    onUpdateSettings { it.copy(floatingAiEnabled = b) }
                }
                SettingToggle("Hide Orb During Focus Dojo", settings.floatingAiDisableInFocus) { b ->
                    onUpdateSettings { it.copy(floatingAiDisableInFocus = b) }
                }
                SettingToggle("Hide Orb While Keyboard Open", settings.floatingAiHideWhileTyping) { b ->
                    onUpdateSettings { it.copy(floatingAiHideWhileTyping = b) }
                }
                SettingToggle("Auto-Save Scanned Timetables", settings.autoSaveScans) { b ->
                    onUpdateSettings { it.copy(autoSaveScans = b) }
                }
                Text("Default Personality", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AiPersonality.entries) { p ->
                        FilterChip(
                            selected = settings.aiPersonality == p.code,
                            onClick = { onUpdateSettings { it.copy(aiPersonality = p.code) } },
                            label = { Text(p.label) }
                        )
                    }
                }
            }
        }

        // 5. Time & Region
        item {
            HoloCard(accentColor = GoldRankS) {
                SectionTitle(Icons.Filled.Public, "Time & Region", GoldRankS)
                SettingToggle("Use 24-Hour Clock", settings.use24HourClock) { b ->
                    onUpdateSettings { it.copy(use24HourClock = b) }
                }
                SettingToggle("Show Ethiopian Time (12h Clock)", settings.showEthiopianTime) { b ->
                    onUpdateSettings { it.copy(showEthiopianTime = b) }
                }
                Text("Timezone (${settings.timezoneId})", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Africa/Addis_Ababa", "UTC", "Europe/London", "America/New_York")) { tz ->
                        FilterChip(
                            selected = settings.timezoneId == tz,
                            onClick = { onUpdateSettings { it.copy(timezoneId = tz) } },
                            label = { Text(tz) }
                        )
                    }
                }
            }
        }

        // 6. Notifications & Quiet Hours
        item {
            HoloCard(accentColor = visuals.primaryAccent) {
                SectionTitle(Icons.Filled.Notifications, "Alerts & Quiet Hours", visuals.primaryAccent)
                SettingToggle("Quiet Hours (${settings.quietStartHour}:00 - ${settings.quietEndHour}:00)", settings.quietHoursEnabled) { b ->
                    onUpdateSettings { it.copy(quietHoursEnabled = b) }
                }
                SettingToggle("Sound Alerts", settings.soundEnabled) { b ->
                    onUpdateSettings { it.copy(soundEnabled = b) }
                }
                SettingToggle("Vibration Alerts", settings.vibrationEnabled) { b ->
                    onUpdateSettings { it.copy(vibrationEnabled = b) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Notif Permission")
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exact Alarms")
                        }
                    }
                }
            }
        }

        // 7. Security, Accessibility & Animations
        item {
            HoloCard(accentColor = CrimsonAlert) {
                SectionTitle(Icons.Filled.Lock, "Security & Offline Privacy", CrimsonAlert)
                SettingToggle("Offline-Only Mode (Pause Cloud Sync)", settings.offlineModeOnly) { b ->
                    onUpdateSettings { it.copy(offlineModeOnly = b) }
                }
                SettingToggle("4-Digit PIN App Lock", settings.appLockEnabled) { b ->
                    if (b && settings.appLockPin.length != 4) {
                        showPinDialog = true
                    } else {
                        onUpdateSettings { it.copy(appLockEnabled = b) }
                    }
                }
                if (settings.appLockEnabled) {
                    TextButton(onClick = { showPinDialog = true }) { Text("Change 4-Digit PIN") }
                    SettingToggle("Allow Biometric Unlock", settings.biometricsEnabled) { b ->
                        onUpdateSettings { it.copy(biometricsEnabled = b) }
                    }
                }
            }
        }

        item {
            HoloCard(accentColor = JadeEmerald) {
                SectionTitle(Icons.Filled.Speed, "Animations & Accessibility", JadeEmerald)
                SettingToggle("Enable Animations", settings.animationsEnabled) { b ->
                    onUpdateSettings { it.copy(animationsEnabled = b) }
                }
                SettingToggle("Reduce Motion", settings.reduceMotion) { b ->
                    onUpdateSettings { it.copy(reduceMotion = b) }
                }
                SettingToggle("Battery Saver Mode (Static Low-Glow)", settings.batterySaverMode) { b ->
                    onUpdateSettings { it.copy(batterySaverMode = b) }
                }
                SettingToggle("Tactile Haptic Feedback", settings.hapticsEnabled) { b ->
                    onUpdateSettings { it.copy(hapticsEnabled = b) }
                }
                OutlinedButton(
                    onClick = { showApkGuideDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("open_apk_guide_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("APK Download, Signing & Play Privacy Guide")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showProfileDialog && profile != null) {
        ProfileEditDialog(
            profile = profile,
            onDismiss = { showProfileDialog = false },
            onSave = onUpdateProfile
        )
    }

    if (showWallpaperDialog) {
        WallpaperStudioDialog(
            wallpapers = uiState.wallpapers,
            settings = settings,
            onDismiss = { showWallpaperDialog = false },
            onAddWallpaper = onAddWallpaper,
            onUpdateTuning = onUpdateWallpaperTuning,
            onDeleteWallpaper = onDeleteWallpaper,
            onResetWallpapers = onResetWallpapers,
            onUpdateSettings = onUpdateSettings
        )
    }

    if (showMusicDialog) {
        MusicStudioDialog(
            tracks = uiState.tracks,
            audioState = audioState,
            settings = settings,
            onDismiss = { showMusicDialog = false },
            onAddTrack = onAddTrack,
            onDeleteTrack = onDeleteTrack,
            onPlayTrackAt = onPlayTrackAt,
            onTogglePlayPause = onTogglePlayPauseMusic,
            onNext = onNextTrack,
            onPrevious = onPreviousTrack,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onUpdateSettings = onUpdateSettings,
            onSpeakMotivation = onSpeakMotivation
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            currentPin = settings.appLockPin,
            onDismiss = { showPinDialog = false },
            onSavePin = { pin ->
                onUpdateSettings { it.copy(appLockPin = pin, appLockEnabled = true) }
            }
        )
    }

    if (showApkGuideDialog) {
        AboutAndApkGuideDialog(onDismiss = { showApkGuideDialog = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account & Study Data?") },
            text = { Text("This permanently erases your local Room cache and Firestore cloud missions, sessions, XP, and chat history.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
