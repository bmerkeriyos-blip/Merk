package com.example.ui.ai

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.ai.ScannedTimetableEvent
import com.example.core.common.ShinobiTimeUtil
import com.example.data.local.TimetableScanEntity
import com.example.ui.components.HoloCard
import com.example.ui.components.RankBadgeChip
import com.example.ui.theme.*
import com.example.viewmodel.ScannerUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScannerScreen(
    scannerState: ScannerUiState,
    scansHistory: List<TimetableScanEntity>,
    autoSaveScans: Boolean,
    timezoneId: String,
    use24HourClock: Boolean,
    onBack: () -> Unit,
    onScanUri: (Uri, String, Boolean, Boolean) -> Unit,
    onUpdateEvent: (Int, ScannedTimetableEvent) -> Unit,
    onRemoveEvent: (Int) -> Unit,
    onConfirmSave: (String, Boolean) -> Unit,
    onResetScanner: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val visuals = LocalShinobiVisuals.current
    var accurateMode by remember { mutableStateOf(true) }
    var lastSourceType by remember { mutableStateOf("GALLERY") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraErrorMsg by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            lastSourceType = "CAMERA"
            onScanUri(uri, "CAMERA", accurateMode, autoSaveScans)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "timetable_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraErrorMsg = "Camera permission denied. You can still upload from Gallery or PDF."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            lastSourceType = "GALLERY"
            onScanUri(uri, "GALLERY", accurateMode, autoSaveScans)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lastSourceType = "PDF"
            onScanUri(uri, "PDF", accurateMode, autoSaveScans)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("AI TIMETABLE SCANNER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scanner_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("timetable_scanner_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HoloCard(accentColor = visuals.primaryAccent) {
                    Text(
                        text = "Capture or upload your university class timetable, exam calendar, or PDF schedule. Gemini AI extracts subjects, days, times, and rooms.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (accurateMode) "Accurate Mode (Gemini Pro Vision)" else "Quick Mode (Fast Lite)", fontWeight = FontWeight.SemiBold)
                            Text("Accurate mode handles dense multi-column university PDFs best.", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = accurateMode, onCheckedChange = { accurateMode = it })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                cameraErrorMsg = null
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("scan_camera_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = visuals.primaryAccent, contentColor = MidnightNavy)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }
                        Button(
                            onClick = {
                                cameraErrorMsg = null
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("scan_gallery_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ChakraOrange, contentColor = MidnightNavy)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        Button(
                            onClick = {
                                cameraErrorMsg = null
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("scan_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ArcaneViolet, contentColor = CyberTextPrimary)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF")
                        }
                    }
                    cameraErrorMsg?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = CrimsonAlert, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when (scannerState) {
                ScannerUiState.Idle -> {}
                ScannerUiState.Scanning -> {
                    item {
                        HoloCard(accentColor = visuals.primaryAccent) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                Text("Optical Jutsu: analyzing timetable layout & extracting classes...")
                            }
                        }
                    }
                }
                is ScannerUiState.Error -> {
                    item {
                        HoloCard(accentColor = CrimsonAlert) {
                            Text(scannerState.message, color = CrimsonAlert)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onResetScanner) { Text("Dismiss") }
                        }
                    }
                }
                is ScannerUiState.Saved -> {
                    item {
                        HoloCard(accentColor = JadeEmerald) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = JadeEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Imported ${scannerState.count} missions into your Schedule!", fontWeight = FontWeight.Bold, color = JadeEmerald)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onResetScanner) { Text("Scan Another Document") }
                        }
                    }
                }
                is ScannerUiState.Preview -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Editable Preview (${scannerState.events.size} events)", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = { onConfirmSave(lastSourceType, accurateMode) },
                                modifier = Modifier.testTag("confirm_save_scanned_events"),
                                colors = ButtonDefaults.buttonColors(containerColor = JadeEmerald, contentColor = MidnightNavy)
                            ) {
                                Text("Save All to Schedule", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    itemsIndexed(scannerState.events) { idx, ev ->
                        HoloCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = ev.title,
                                    onValueChange = { onUpdateEvent(idx, ev.copy(title = it)) },
                                    label = { Text("Title") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RankBadgeChip(ev.missionRank)
                                IconButton(onClick = { onRemoveEvent(idx) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove Scanned Event", tint = CrimsonAlert)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ev.subject,
                                    onValueChange = { onUpdateEvent(idx, ev.copy(subject = it)) },
                                    label = { Text("Subject") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ev.dateIso,
                                    onValueChange = { onUpdateEvent(idx, ev.copy(dateIso = it)) },
                                    label = { Text("Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${ev.dayOfWeek} • ${
                                    ShinobiTimeUtil.formatTime(ev.startTimeMinutes, use24HourClock)
                                } - ${
                                    ShinobiTimeUtil.formatTime(ev.endTimeMinutes, use24HourClock)
                                } • Room: ${ev.location.ifBlank { "TBA" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (scansHistory.isNotEmpty()) {
                item {
                    Text("Scan History", style = MaterialTheme.typography.titleSmall)
                }
                itemsIndexed(scansHistory.take(5)) { _, scan ->
                    HoloCard(contentPadding = PaddingValues(12.dp)) {
                        Text("${scan.sourceType} (${scan.mode}) — ${scan.extractedCount} events", fontWeight = FontWeight.SemiBold)
                        if (scan.summary.isNotBlank()) {
                            Text(scan.summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
