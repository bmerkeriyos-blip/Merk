package com.example.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiSfx
import com.example.data.local.MusicTrackEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import com.example.ui.theme.*

@Composable
fun ProfileEditDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var university by remember { mutableStateOf(profile.university) }
    var department by remember { mutableStateOf(profile.department) }
    var academicYear by remember { mutableStateOf(profile.academicYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Shinobi Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Shinobi Handle") }, singleLine = true)
                OutlinedTextField(value = university, onValueChange = { university = it }, label = { Text("University") }, singleLine = true)
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, singleLine = true)
                OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("Academic Year") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            displayName = displayName.trim().ifBlank { profile.displayName },
                            username = username.trim().ifBlank { profile.username },
                            university = university.trim(),
                            department = department.trim(),
                            academicYear = academicYear.trim()
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WallpaperStudioDialog(
    wallpapers: List<WallpaperItemEntity>,
    settings: UserSettingsEntity,
    onDismiss: () -> Unit,
    onAddWallpaper: (String, String) -> Unit,
    onUpdateTuning: (WallpaperItemEntity, Float, Float, Float, Boolean) -> Unit,
    onDeleteWallpaper: (String) -> Unit,
    onResetWallpapers: () -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit
) {
    val context = LocalContext.current
    var targetScreen by remember { mutableStateOf("GLOBAL") }
    val active = wallpapers.firstOrNull { it.screenTarget == targetScreen }
        ?: wallpapers.firstOrNull { it.screenTarget == "GLOBAL" }

    var brightness by remember(active?.id) { mutableFloatStateOf(active?.brightness ?: 0.72f) }
    var blur by remember(active?.id) { mutableFloatStateOf(active?.blurRadius ?: 2f) }
    var overlay by remember(active?.id) { mutableFloatStateOf(active?.overlayOpacity ?: 0.65f) }
    var parallax by remember(active?.id) { mutableStateOf(active?.parallaxEnabled ?: true) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAddWallpaper(uri.toString(), targetScreen)
        }
    }

    val safImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onAddWallpaper(uri.toString(), targetScreen)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallpapers & Visual Studio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Target Screen", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GLOBAL", "HOME", "SCHEDULE", "MISSIONS").forEach { scr ->
                        FilterChip(
                            selected = targetScreen == scr,
                            onClick = { targetScreen = scr },
                            label = { Text(scr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f).testTag("pick_wallpaper_photo")
                    ) {
                        Text("Photo Picker")
                    }
                    OutlinedButton(
                        onClick = { safImagePicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Files (SAF)")
                    }
                }

                if (active != null) {
                    Text("Brightness: ${(brightness * 100).toInt()}%")
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.2f..1f)

                    Text("Blur Radius: ${blur.toInt()}dp")
                    Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..20f)

                    Text("Overlay Opacity: ${(overlay * 100).toInt()}%")
                    Slider(value = overlay, onValueChange = { overlay = it }, valueRange = 0.2f..0.9f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Parallax Depth Scroll")
                        Switch(checked = parallax, onCheckedChange = { parallax = it })
                    }

                    Button(
                        onClick = { onUpdateTuning(active, brightness, blur, overlay, parallax) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Tuning")
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Slideshow (${settings.wallpaperSlideshowIntervalSec}s)")
                    Switch(
                        checked = settings.wallpaperSlideshowEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(wallpaperSlideshowEnabled = enabled) }
                        }
                    )
                }

                if (wallpapers.size > 1) {
                    Text("Saved Wallpapers (${wallpapers.size})", style = MaterialTheme.typography.labelMedium)
                    wallpapers.filter { !it.builtInResName.equals("bg_ninja_village", true) }.forEach { w ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("${w.screenTarget} custom image", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onDeleteWallpaper(w.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Wallpaper", tint = CrimsonAlert)
                            }
                        }
                    }
                }

                TextButton(onClick = onResetWallpapers) {
                    Text("Reset to Default Ninja Village")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun MusicStudioDialog(
    tracks: List<MusicTrackEntity>,
    audioState: AudioPlaybackState,
    settings: UserSettingsEntity,
    onDismiss: () -> Unit,
    onAddTrack: (String, String, Boolean) -> Unit,
    onDeleteTrack: (String) -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onSpeakMotivation: (String) -> Unit,
    onPlaySfx: (ShinobiSfx) -> Unit = {}
) {
    val context = LocalContext.current
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val label = uri.lastPathSegment?.substringAfterLast("/") ?: "Custom Track"
            onAddTrack(label, uri.toString(), false)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Music & Audio Studio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${audioState.currentTitle} • ${audioState.currentArtist}",
                    fontWeight = FontWeight.Bold,
                    color = LocalShinobiVisuals.current.primaryAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Filled.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (audioState.shuffleEnabled) JadeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Track")
                    }
                    FilledIconButton(onClick = onTogglePlayPause) {
                        Icon(if (audioState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play or Pause")
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next Track")
                    }
                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = "Cycle Repeat Mode",
                            tint = if (audioState.repeatMode != Player.REPEAT_MODE_OFF) JadeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text("Music Volume (${(settings.musicVolume * 100).toInt()}%)")
                Slider(
                    value = settings.musicVolume,
                    onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } }
                )

                Text("Ninja SFX Volume (${(settings.sfxVolume * 100).toInt()}%)")
                Slider(
                    value = settings.sfxVolume,
                    onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPlaySfx(ShinobiSfx.SHURIKEN) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shuriken SFX")
                    }
                    OutlinedButton(
                        onClick = { onPlaySfx(ShinobiSfx.JUTSU) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Jutsu SFX")
                    }
                }

                Text("Motivational Voice Volume (${(settings.motivationalVolume * 100).toInt()}%)")
                Slider(
                    value = settings.motivationalVolume,
                    onValueChange = { v -> onUpdateSettings { it.copy(motivationalVolume = v) } }
                )

                OutlinedButton(
                    onClick = { onSpeakMotivation("Focus your chakra. Your next academic mission awaits.") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Motivational Briefing Voice")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-play on App Open")
                    Switch(checked = settings.musicOnOpen, onCheckedChange = { b -> onUpdateSettings { it.copy(musicOnOpen = b) } })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-play During Focus Dojo")
                    Switch(checked = settings.musicDuringFocus, onCheckedChange = { b -> onUpdateSettings { it.copy(musicDuringFocus = b) } })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Speak Celebration on Rank Up")
                    Switch(checked = settings.musicOnComplete, onCheckedChange = { b -> onUpdateSettings { it.copy(musicOnComplete = b) } })
                }

                Button(
                    onClick = { audioPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth().testTag("import_audio_button")
                ) {
                    Text("Import MP3 / M4A / WAV (SAF)")
                }

                tracks.forEachIndexed { idx, track ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onPlayTrackAt(idx) }, modifier = Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1)
                        }
                        if (!track.builtInSynth) {
                            IconButton(onClick = { onDeleteTrack(track.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Track", tint = CrimsonAlert)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PinSetupDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf(currentPin) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit App Lock PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pin = it },
                label = { Text("4-Digit PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pin_setup_input")
            )
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4,
                onClick = {
                    onSavePin(pin)
                    onDismiss()
                }
            ) {
                Text("Enable Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AboutAndApkGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("APK Build & Play Compliance") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. GitHub Actions APK Download", fontWeight = FontWeight.Bold)
                Text(
                    "Push this repository to GitHub. Open the repository Actions tab -> 'Android Build & Test' -> run/select latest workflow -> download 'shinobi-planner-debug-apk' artifact containing app-debug.apk.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("2. Local Debug vs Play Store Release Signing", fontWeight = FontWeight.Bold)
                Text(
                    "For Google Play release builds, configure KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD in CI secrets or AI Studio Secrets without modifying debug.keystore.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("3. Privacy & Data Protection", fontWeight = FontWeight.Bold)
                Text(
                    "All media imports use Android Photo Picker / SAF (zero broad storage permissions). Cloud sync is protected by Firebase Auth + App Check + strict owner-only Firestore rules. You can export or delete your entire account data anytime from Settings.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
