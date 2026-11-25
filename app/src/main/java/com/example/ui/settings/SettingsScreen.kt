package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.AiPersonality
import com.example.core.common.ShinobiTimeUtil
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiSfx
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ThemePreset
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
    onPlaySfx: (ShinobiSfx) -> Unit = {},
    onTriggerSync: () -> Unit,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onWritePdf: (Uri) -> Boolean,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit,
    isSigningIn: Boolean = false,
    authErrorMessage: String? = null
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
            Column {
                Text(
                    text = "SHINOBI COMMAND SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize glass HUD visuals, audio cues, clock format & cloud sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Account & Export
        item {
            HoloCard(accentColor = visuals.primaryAccent, glassTier = GlassTier.HERO) {
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
                if (!authErrorMessage.isNullOrBlank() && !isSignedInWithGoogle) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth().testTag("settings_auth_error")
                    ) {
                        Text(
                            text = authErrorMessage,
                            color = CrimsonAlert,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
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
                            enabled = !isSigningIn,
                            modifier = Modifier.weight(1f).testTag("settings_google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NarutoOrange,
                                contentColor = MidnightNavy
                            )
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MidnightNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Signing in...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign in Google", fontWeight = FontWeight.Bold)
                            }
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

        // 2. Appearance, Theme Presets, Accent Color & Glow Intensity
        item {
            HoloCard(accentColor = NarutoOrange) {
                SectionTitle(Icons.Filled.Palette, "Appearance & Holographic Presets", NarutoOrange)
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
                Spacer(modifier = Modifier.height(8.dp))
                Text("Accent Color (Custom Chakra)", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ORANGE", "CYAN", "VIOLET", "EMERALD", "GOLD", "CRIMSON")) { key ->
                        FilterChip(
                            selected = settings.accentColorKey == key && settings.themePreset == ThemePreset.CUSTOM.name,
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        themePreset = ThemePreset.CUSTOM.name,
                                        accentColorKey = key
                                    )
                                }
                            },
                            label = { Text(key) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggle("Glassmorphic Cards", settings.glassmorphism) { b ->
                    onUpdateSettings { it.copy(glassmorphism = b) }
                }
                SettingToggle(
                    label = "Global Holographic Glow",
                    checked = settings.globalGlowEnabled,
                    switchTag = "global_glow_switch"
                ) { b ->
                    onUpdateSettings { it.copy(globalGlowEnabled = b) }
                }
                Text("Holographic Glow Intensity: ${(settings.glowIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.glowIntensity,
                    onValueChange = { g -> onUpdateSettings { it.copy(glowIntensity = g) } },
                    valueRange = 0.1f..1.0f
                )
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
            HoloCard(accentColor = HolographicPurple) {
                SectionTitle(Icons.Filled.Wallpaper, "Wallpapers & Audio Studios", HolographicPurple)
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
            HoloCard(accentColor = HolographicPurple) {
                SectionTitle(Icons.Filled.AutoAwesome, "AI Mentor & Floating Orb", HolographicPurple)
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
            HoloCard(accentColor = ChakraCyan) {
                SectionTitle(Icons.Filled.Public, "Time & Region", ChakraCyan)
                val amPmEnabled = !settings.use24HourClock
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("12-Hour Time (AM / PM)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (amPmEnabled) {
                                "Format preview: ${ShinobiTimeUtil.formatTime(14 * 60 + 30, use24Hour = false)} (AM / PM)"
                            } else {
                                "Format preview: ${ShinobiTimeUtil.formatTime(14 * 60 + 30, use24Hour = true)} (24-Hour)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = amPmEnabled,
                        onCheckedChange = { useAmPm ->
                            onUpdateSettings { it.copy(use24HourClock = !useAmPm) }
                        },
                        modifier = Modifier.testTag("am_pm_time_switch")
                    )
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
            HoloCard(accentColor = GoldRankS) {
                SectionTitle(Icons.Filled.Notifications, "Alerts & Quiet Hours", GoldRankS)
                val quietStartLabel = ShinobiTimeUtil.formatTime(settings.quietStartHour * 60, settings.use24HourClock)
                val quietEndLabel = ShinobiTimeUtil.formatTime(settings.quietEndHour * 60, settings.use24HourClock)
                SettingToggle("Quiet Hours ($quietStartLabel - $quietEndLabel)", settings.quietHoursEnabled) { b ->
                    onUpdateSettings { it.copy(quietHoursEnabled = b) }
                }
                if (settings.quietHoursEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start: $quietStartLabel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(quietStartHour = (it.quietStartHour + 23) % 24) }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("quiet_start_minus"),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-1h") }
                                OutlinedButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(quietStartHour = (it.quietStartHour + 1) % 24) }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("quiet_start_plus"),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+1h") }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End: $quietEndLabel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(quietEndHour = (it.quietEndHour + 23) % 24) }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("quiet_end_minus"),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-1h") }
                                OutlinedButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(quietEndHour = (it.quietEndHour + 1) % 24) }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("quiet_end_plus"),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+1h") }
                            }
                        }
                    }
                }
                SettingToggle("Sound Effects & Alerts (Shuriken / Jutsu)", settings.soundEnabled) { b ->
                    onUpdateSettings { it.copy(soundEnabled = b) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPlaySfx(ShinobiSfx.SHURIKEN) },
                        enabled = settings.soundEnabled,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("test_shuriken_sfx_button")
                    ) {
                        Text("Test Shuriken")
                    }
                    OutlinedButton(
                        onClick = { onPlaySfx(ShinobiSfx.JUTSU) },
                        enabled = settings.soundEnabled,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("test_jutsu_sfx_button")
                    ) {
                        Text("Test Jutsu")
                    }
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
            HoloCard(accentColor = ChakraGreen) {
                SectionTitle(Icons.Filled.Speed, "Animations & Accessibility", ChakraGreen)
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
            onSpeakMotivation = onSpeakMotivation,
            onPlaySfx = onPlaySfx
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
    icon: ImageVector,
    title: String,
    tint: Color
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
    switchTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier
        )
    }
}
