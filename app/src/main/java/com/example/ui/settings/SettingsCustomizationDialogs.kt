package com.example.ui.settings

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.core.common.NarutoArtwork
import com.example.core.common.NarutoVisualCatalog
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiSfx
import com.example.data.local.MusicTrackEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.WallpaperItemEntity
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.JadeEmerald
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy

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
                OutlinedTextField(value = university, onValueChange = { university = it }, label = { Text("Academy / University") }, singleLine = true)
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
    onSelectBuiltinWallpaper: (resName: String, screenTarget: String) -> Unit,
    onAddWallpaper: (String, String) -> Unit,
    onUpdateTuning: (WallpaperItemEntity, Float, Float, Float, Boolean) -> Unit,
    onDeleteWallpaper: (String) -> Unit,
    onResetWallpapers: () -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit
) {
    val context = LocalContext.current
    val visuals = LocalShinobiVisuals.current
    var targetScreen by remember { mutableStateOf("GLOBAL") }
    var previewArtwork by remember(targetScreen) {
        val currentBuiltIn = wallpapers.firstOrNull {
            it.screenTarget.equals(targetScreen, ignoreCase = true) && it.builtInResName.isNotBlank()
        }?.builtInResName
        mutableStateOf(
            NarutoVisualCatalog.artworks.firstOrNull { it.resName.equals(currentBuiltIn, ignoreCase = true) }
                ?: NarutoVisualCatalog.artworks.first()
        )
    }

    val active = wallpapers.firstOrNull { it.screenTarget.equals(targetScreen, ignoreCase = true) }
        ?: wallpapers.firstOrNull { it.screenTarget.equals("GLOBAL", ignoreCase = true) }

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
        title = { Text("Naruto Wallpaper Studio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Target Screen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("GLOBAL", "HOME", "SCHEDULE", "MISSIONS", "PROGRESS", "SETTINGS")) { scr ->
                        FilterChip(
                            selected = targetScreen == scr,
                            onClick = { targetScreen = scr },
                            label = { Text(scr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Live Preview Card of Selected Naruto Artwork
                LiveArtworkPreviewCard(
                    artwork = previewArtwork,
                    targetScreen = targetScreen,
                    accentColor = visuals.primaryAccent,
                    onApply = { onSelectBuiltinWallpaper(previewArtwork.resName, targetScreen) }
                )

                Text(
                    text = "2. Offline Naruto Gallery (${NarutoVisualCatalog.artworks.size} Artworks)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NarutoVisualCatalog.artworks, key = { it.resName }) { art ->
                        val isPreviewed = previewArtwork.resName == art.resName
                        val isApplied = active?.builtInResName.equals(art.resName, ignoreCase = true)
                        Column(
                            modifier = Modifier
                                .width(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isPreviewed) 2.dp else 1.dp,
                                    color = if (isPreviewed) visuals.primaryAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { previewArtwork = art }
                                .padding(4.dp)
                                .testTag("wallpaper_gallery_${art.resName}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = art.drawableRes),
                                    contentDescription = art.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isApplied) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Applied",
                                        tint = JadeEmerald,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = art.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isPreviewed) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Text("Or Import Custom Device Image", style = MaterialTheme.typography.labelMedium)
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
                    HorizontalDivider()
                    Text("Brightness: ${(brightness * 100).toInt()}%")
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.2f..1f)

                    Text("Blur Radius: ${blur.toInt()}dp")
                    Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..20f)

                    Text("Dark Scrim Opacity: ${(overlay * 100).toInt()}%")
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
                        Text("Apply Visual Tuning")
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
                    Text("Screen Overrides (${wallpapers.size})", style = MaterialTheme.typography.labelMedium)
                    wallpapers.forEach { w ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            val labelName = w.builtInResName.ifBlank { "Custom device image" }
                            Text("${w.screenTarget}: $labelName", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onDeleteWallpaper(w.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Wallpaper", tint = CrimsonAlert)
                            }
                        }
                    }
                }

                TextButton(onClick = onResetWallpapers) {
                    Text("Restore Default Naruto Artwork")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun LiveArtworkPreviewCard(
    artwork: NarutoArtwork,
    targetScreen: String,
    accentColor: Color,
    onApply: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .border(1.2.dp, accentColor.copy(alpha = 0.6f), shape)
    ) {
        Image(
            painter = painterResource(id = artwork.drawableRes),
            contentDescription = artwork.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MidnightNavy.copy(alpha = 0.88f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = artwork.clanTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(artwork.title, style = MaterialTheme.typography.titleSmall, color = GlassWhite, fontWeight = FontWeight.Bold)
                Text(artwork.subtitle, style = MaterialTheme.typography.labelSmall, color = GlassWhite.copy(alpha = 0.8f), maxLines = 1)
            }
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("apply_selected_wallpaper_button")
            ) {
                Text("Apply to $targetScreen", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
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
            val displayName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            val label = (displayName ?: uri.lastPathSegment?.substringAfterLast("/"))
                ?.substringBeforeLast(".")
                ?.takeIf { it.isNotBlank() }
                ?: "Custom Track"
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

                if (tracks.isEmpty()) {
                    Text(
                        text = "No songs added yet. Tap above to import your own MP3, M4A, or WAV tracks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    tracks.forEachIndexed { idx, track ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onPlayTrackAt(idx) }, modifier = Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1)
                            }
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
        title = { Text("Shinobi Planner Offline Edition") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. 100% Offline Privacy", fontWeight = FontWeight.Bold)
                Text(
                    "All schedules, study scrolls, XP progress, and focus sessions stay encrypted in your device Room database with zero internet connections or cloud telemetry.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("2. Bundled Naruto Artworks & Themes", fontWeight = FontWeight.Bold)
                Text(
                    "Includes 12 bundled offline artworks and 8 multi-style visual themes (Naruto Chakra, Sage Mode, Kurama, Sasuke, Akatsuki, Dark Japanese, Premium Glass, and Clay).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("3. Zero Broad Storage Permissions", fontWeight = FontWeight.Bold)
                Text(
                    "Wallpaper and music imports use Android Photo Picker / SAF so your device privacy stays protected. You can export JSON/CSV/PDF backups anytime.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
