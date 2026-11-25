package com.example.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.NarutoVisualCatalog
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.UserProfileEntity
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.components.NarutoHeroArtBanner
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.JadeEmerald
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.ui.theme.ThemePreset

@Composable
fun OnboardingScreen(
    profile: UserProfileEntity?,
    initialThemePreset: String = ThemePreset.NARUTO_CHAKRA.name,
    onComplete: (
        displayName: String,
        university: String,
        department: String,
        academicYear: String,
        timezoneId: String,
        use24HourClock: Boolean,
        showEthiopianTime: Boolean,
        themePreset: String,
        initialWallpaperResName: String
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var displayName by remember(profile?.uid) { mutableStateOf(profile?.displayName.orEmpty()) }
    var university by remember { mutableStateOf(profile?.university.orEmpty()) }
    var department by remember { mutableStateOf(profile?.department.orEmpty()) }
    var academicYear by remember { mutableStateOf(profile?.academicYear?.ifBlank { "Year 2" } ?: "Year 2") }
    var timezoneId by remember { mutableStateOf(ShinobiTimeUtil.DEFAULT_TIMEZONE_ID) }
    var use24HourClock by remember { mutableStateOf(false) }
    var showEthiopianTime by remember { mutableStateOf(true) }
    var selectedPreset by remember(initialThemePreset) {
        mutableStateOf(ThemePreset.fromStorage(initialThemePreset).name)
    }
    var notifPermissionResult by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionResult = if (granted) "Mission alerts enabled" else "Alerts can be enabled later in Settings"
    }

    val totalSteps = 3
    val resolvedPreset = ThemePreset.fromStorage(selectedPreset)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .align(Alignment.Center)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHINOBI DOJO SETUP  ${step + 1}/$totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChakraOrange,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        onComplete(
                            displayName.ifBlank { profile?.displayName ?: "Shinobi Scholar" },
                            university,
                            department,
                            academicYear,
                            timezoneId,
                            use24HourClock,
                            showEthiopianTime,
                            resolvedPreset.name,
                            resolvedPreset.defaultResName
                        )
                    },
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip to Dojo", color = CyberTextSecondary)
                }
            }

            XpProgressBar(
                progress = (step + 1) / totalSteps.toFloat(),
                startColor = NarutoOrange,
                endColor = ChakraCyan,
                height = 7.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_shinobi_avatar),
                                contentDescription = "Shinobi Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, ChakraOrange, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Welcome to Shinobi Planner",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = CyberTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "100% offline Hidden Leaf study scroll & mission dojo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        NarutoHeroArtBanner(
                            drawableRes = R.drawable.img_naruto_uzumaki,
                            badgeLabel = "OFFLINE NINJA SCROLL",
                            title = "Forge Your Academic Ninja Way",
                            subtitle = "Schedules, Focus Dojo, missions, and themes stored securely on your device.",
                            accentColor = ChakraOrange,
                            height = 128.dp
                        )

                        HoloCard(
                            accentColor = ChakraOrange,
                            glassTier = GlassTier.HERO,
                            glowActive = true
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.School, contentDescription = null, tint = ChakraOrange)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Scholar Profile", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Ninja Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = university,
                                onValueChange = { university = it },
                                label = { Text("University / Academy") },
                                placeholder = { Text("e.g. Hidden Leaf Academy") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_university_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                label = { Text("Department / Major") },
                                placeholder = { Text("e.g. Software Engineering") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_department_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = academicYear,
                                onValueChange = { academicYear = it },
                                label = { Text("Academic Year") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    1 -> {
                        HoloCard(accentColor = ChakraCyan) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Public, contentDescription = null, tint = ChakraCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Time & Timezone", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select your preferred timezone for class reminders, quiet hours, and local clock display.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val tzOptions = listOf(
                                "Africa/Addis_Ababa" to "East Africa Time (EAT, UTC+3)",
                                "UTC" to "UTC",
                                "Africa/Nairobi" to "Nairobi (EAT)",
                                "Europe/London" to "London (GMT/BST)",
                                "America/New_York" to "New York (ET)"
                            )
                            tzOptions.forEach { (tzId, tzLabel) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = timezoneId == tzId,
                                        onClick = { timezoneId = tzId }
                                    )
                                    Text(tzLabel, color = CyberTextPrimary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CyberBorderGlow)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("12-Hour Time (AM / PM)", color = CyberTextPrimary, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (!use24HourClock) "Shows times like 2:30 PM (turn off for 14:30)" else "24-hour clock active (14:30)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberTextSecondary
                                    )
                                }
                                Switch(
                                    checked = !use24HourClock,
                                    onCheckedChange = { useAmPm -> use24HourClock = !useAmPm },
                                    modifier = Modifier.testTag("onboarding_am_pm_switch")
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CyberBorderGlow)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Show Ethiopian Time (12h Clock)", color = CyberTextPrimary, fontWeight = FontWeight.Medium)
                                    Text("Displays shifted Ethiopian time alongside standard local time", style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                                }
                                Switch(
                                    checked = showEthiopianTime,
                                    onCheckedChange = { showEthiopianTime = it },
                                    modifier = Modifier.testTag("onboarding_ethiopian_time_switch")
                                )
                            }
                        }
                    }

                    2 -> {
                        HoloCard(accentColor = resolvedPreset.swatchPrimary) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Palette, contentDescription = null, tint = resolvedPreset.swatchPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Choose Naruto Visual Theme", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            ThemePreset.entries.forEach { preset ->
                                val chosen = resolvedPreset == preset
                                val presetStyle = ThemeDesignSystem.glassStyle(
                                    accentColor = preset.swatchPrimary,
                                    tier = GlassTier.SUBTLE,
                                    glowActive = chosen,
                                    cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                                    tintColor = if (chosen) preset.swatchPrimary else null
                                )
                                Surface(
                                    onClick = { selectedPreset = preset.name },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .ShinobiGlassModifier(style = presetStyle, showSpecularEdge = false),
                                    shape = presetStyle.shape,
                                    color = Color.Transparent,
                                    contentColor = CyberTextPrimary,
                                    tonalElevation = presetStyle.tonalElevation
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(
                                                id = NarutoVisualCatalog.resolveDrawable(preset.defaultResName)
                                            ),
                                            contentDescription = preset.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.label, color = CyberTextPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                                        }
                                        if (chosen) {
                                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = preset.swatchPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        HoloCard(accentColor = GoldRankS) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = GoldRankS)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Offline Mission Reminders", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = notifPermissionResult ?: "Enable local device alerts for upcoming classes, exam countdowns, and daily briefings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        notifPermissionResult = "Notifications enabled on this Android version"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Grant Notification Permission")
                            }
                        }

                        HoloCard(accentColor = JadeEmerald, glassTier = GlassTier.SUBTLE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = JadeEmerald)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Offline Focus Audio & Wallpapers Ready", style = MaterialTheme.typography.titleSmall, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Includes 12 bundled Naruto artworks, shinobi sound effects, motivational voice cues, and local SAF MP3/M4A player.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(52.dp).testTag("onboarding_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step++
                        } else {
                            onComplete(
                                displayName.ifBlank { profile?.displayName ?: "Shinobi Scholar" },
                                university,
                                department,
                                academicYear,
                                timezoneId,
                                use24HourClock,
                                showEthiopianTime,
                                resolvedPreset.name,
                                resolvedPreset.defaultResName
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp).testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ChakraOrange, contentColor = MidnightNavy)
                ) {
                    Text(
                        text = if (step < totalSteps - 1) "Continue" else "Enter Shinobi Dojo",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (step < totalSteps - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
