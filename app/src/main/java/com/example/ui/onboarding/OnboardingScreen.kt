package com.example.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.core.ai.AiPersonality
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.UserProfileEntity
import com.example.ui.components.HoloCard
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    profile: UserProfileEntity?,
    initialThemePreset: String = ThemePreset.NARUTO.name,
    initialAiPersonality: String = AiPersonality.MENTOR.code,
    onComplete: (
        displayName: String,
        university: String,
        department: String,
        academicYear: String,
        timezoneId: String,
        use24HourClock: Boolean,
        showEthiopianTime: Boolean,
        themePreset: String,
        aiPersonality: String
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
    var selectedPreset by remember(initialThemePreset) { mutableStateOf(initialThemePreset) }
    var selectedPersonality by remember(initialAiPersonality) { mutableStateOf(initialAiPersonality) }
    var notifPermissionResult by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionResult = if (granted) "Mission alerts enabled" else "Alerts can be enabled later in Settings"
    }

    val totalSteps = 4

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
                    text = "SHINOBI SETUP  ${step + 1}/$totalSteps",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChakraOrange
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
                            selectedPreset,
                            selectedPersonality
                        )
                    },
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip to Dojo", color = CyberTextSecondary)
                }
            }

            com.example.ui.components.XpProgressBar(
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
                                    .clip(CircleShape),
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
                                    text = "Configure your Hidden Leaf study scroll & training dojo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextSecondary
                                )
                            }
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_scroll_banner),
                            contentDescription = "Hidden Leaf Study Scroll",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        HoloCard(
                            accentColor = ChakraOrange,
                            glassTier = com.example.ui.components.GlassTier.HERO,
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
                                placeholder = { Text("e.g. Metro Tech University") },
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
                        HoloCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Public, contentDescription = null, tint = ChakraOrange)
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
                        HoloCard(accentColor = ChakraOrange) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Palette, contentDescription = null, tint = ChakraOrange)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Dojo Theme Preset", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            ThemePreset.entries.forEach { preset ->
                                val chosen = selectedPreset == preset.name
                                val presetStyle = ThemeDesignSystem.glassStyle(
                                    accentColor = ChakraOrange,
                                    tier = GlassTier.SUBTLE,
                                    glowActive = chosen,
                                    cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                                    tintColor = if (chosen) ChakraOrange else null
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
                                    border = null,
                                    tonalElevation = presetStyle.tonalElevation
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.label, color = CyberTextPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                                        }
                                        if (chosen) {
                                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = ChakraOrange)
                                        }
                                    }
                                }
                            }
                        }

                        HoloCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = GoldRankS)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Mission Notifications", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = notifPermissionResult ?: "Enable alerts for upcoming classes, exam countdowns, and daily briefings.",
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
                    }

                    3 -> {
                        HoloCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ArcaneViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI Ninja Mentor Personality", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            AiPersonality.entries.forEach { p ->
                                val chosen = selectedPersonality == p.code
                                val persStyle = ThemeDesignSystem.glassStyle(
                                    accentColor = ArcaneViolet,
                                    tier = GlassTier.SUBTLE,
                                    glowActive = chosen,
                                    cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                                    tintColor = if (chosen) ArcaneViolet else null
                                )
                                Surface(
                                    onClick = { selectedPersonality = p.code },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .ShinobiGlassModifier(style = persStyle, showSpecularEdge = false),
                                    shape = persStyle.shape,
                                    color = Color.Transparent,
                                    contentColor = CyberTextPrimary,
                                    border = null,
                                    tonalElevation = persStyle.tonalElevation
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(p.label, color = CyberTextPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(p.systemTone, style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary, maxLines = 2)
                                        }
                                        if (chosen) {
                                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = ArcaneViolet)
                                        }
                                    }
                                }
                            }
                        }

                        HoloCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = JadeEmerald)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Custom Music & Audio Dojo Ready", style = MaterialTheme.typography.titleMedium, color = CyberTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Includes shinobi sound effects, voice briefings, and SAF import for your own MP3/M4A songs and custom wallpapers.",
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
                                selectedPreset,
                                selectedPersonality
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
