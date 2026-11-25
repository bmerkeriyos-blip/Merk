package com.example.ui.ai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.ai.AiPersonality
import com.example.core.ai.AiResponseTier
import com.example.core.ai.StudyStance
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GoldRankS
import com.example.ui.theme.HoloSurfaceElevated
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.viewmodel.AiChatUiState
import com.example.viewmodel.PendingDestructiveAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STUDY_STARTERS = listOf(
    "Explain recursion like a Shadow Clone jutsu",
    "Quiz me with 3 progressive questions",
    "Break down integration by parts step-by-step",
    "Build a cheat-sheet scroll for key formulas",
    "Add mathematics study tomorrow at 4 PM"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMentorScreen(
    chats: List<ChatSessionEntity>,
    activeChatId: String?,
    messages: List<ChatMessageEntity>,
    chatState: AiChatUiState,
    currentPersonalityCode: String,
    tasks: List<PlannerTaskEntity>,
    timezoneId: String,
    pendingDelete: PendingDestructiveAction?,
    subjects: List<SubjectEntity> = emptyList(),
    studyStance: StudyStance = StudyStance.DOJO_TUTOR,
    focusSubject: String = "",
    responseTier: AiResponseTier = AiResponseTier.BALANCED,
    onBack: () -> Unit,
    onSelectPersonality: (AiPersonality) -> Unit,
    onSelectStudyStance: (StudyStance) -> Unit = {},
    onSelectFocusSubject: (String) -> Unit = {},
    onSelectResponseTier: (AiResponseTier) -> Unit = {},
    onSelectOrCreateChat: (AiPersonality) -> Unit,
    onStartNewChat: (AiPersonality) -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSendMessage: (String, AiPersonality, List<PlannerTaskEntity>, String) -> Unit,
    onRetryLast: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onSpeakReply: (String) -> Unit = {},
    onConfirmPendingDelete: () -> Unit,
    onDismissPendingDelete: () -> Unit,
    onOpenScanner: () -> Unit
) {
    BackHandler { onBack() }

    val visuals = LocalShinobiVisuals.current
    val clipboard = LocalClipboardManager.current
    val personality = remember(currentPersonalityCode) { AiPersonality.fromCode(currentPersonalityCode) }
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var copiedMessageId by remember { mutableStateOf<String?>(null) }
    var voiceNotice by remember { mutableStateOf<String?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "${inputText.trim()} $spoken"
                voiceNotice = null
            }
        }
    }

    val listState = rememberLazyListState()
    val isBusy = chatState is AiChatUiState.Loading || chatState is AiChatUiState.Streaming

    val quickPrompts = remember(focusSubject) {
        if (focusSubject.isNotBlank()) {
            listOf(
                "Explain the core principles of $focusSubject",
                "Quiz me on $focusSubject with 3 questions",
                "Step-by-step practice problem in $focusSubject",
                "Build a cheat-sheet scroll for $focusSubject",
                "Add $focusSubject study tomorrow at 4 PM"
            )
        } else {
            STUDY_STARTERS
        }
    }

    LaunchedEffect(Unit) {
        onSelectOrCreateChat(personality)
    }

    LaunchedEffect(messages.size, chatState) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SenseiAvatarSeal(sizeDp = 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI SENSEI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${personality.label} • ${personality.ninjaRank}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HolographicPurple,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("ai_mentor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenScanner, modifier = Modifier.size(48.dp).testTag("ai_open_scanner_button")) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = "Open AI Timetable Scanner", tint = ChakraCyan)
                    }
                    IconButton(onClick = { showHistorySheet = true }, modifier = Modifier.size(48.dp).testTag("ai_history_button")) {
                        Icon(Icons.Filled.History, contentDescription = "Chat History")
                    }
                    IconButton(onClick = { onStartNewChat(personality) }, modifier = Modifier.size(48.dp).testTag("ai_new_chat_button")) {
                        Icon(Icons.Filled.AddComment, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (visuals.isDark) 0.84f else 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("ai_mentor_screen")
        ) {
            // Character Persona Selector
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AiPersonality.entries) { p ->
                    FilterChip(
                        selected = p == personality,
                        onClick = { onSelectPersonality(p) },
                        label = { Text(p.label) },
                        modifier = Modifier.testTag("ai_personality_${p.code.lowercase()}")
                    )
                }
            }

            // Study Jutsu Stance + Subject Focus Strip
            LazyRow(
                modifier = Modifier.testTag("ai_stance_subject_row"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StudyStance.entries) { stance ->
                    FilterChip(
                        selected = studyStance == stance,
                        onClick = { onSelectStudyStance(stance) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stance.label) },
                        modifier = Modifier.testTag("ai_stance_${stance.code.lowercase()}")
                    )
                }
                items(AiResponseTier.entries) { tier ->
                    FilterChip(
                        selected = responseTier == tier,
                        onClick = { onSelectResponseTier(tier) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(tier.label) },
                        modifier = Modifier.testTag("ai_tier_${tier.code.lowercase()}")
                    )
                }
                if (subjects.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = focusSubject.isBlank(),
                            onClick = { onSelectFocusSubject("") },
                            leadingIcon = {
                                Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("All Subjects") },
                            modifier = Modifier.testTag("ai_subject_all")
                        )
                    }
                    items(subjects, key = { it.id }) { subj ->
                        FilterChip(
                            selected = focusSubject.equals(subj.name, ignoreCase = true),
                            onClick = { onSelectFocusSubject(subj.name) },
                            label = { Text(subj.name) },
                            modifier = Modifier.testTag("ai_subject_${subj.id}")
                        )
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("ai_messages_list"),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && chatState is AiChatUiState.Idle) {
                    item {
                        SenseiWelcomeScroll(
                            personality = personality,
                            studyStance = studyStance,
                            focusSubject = focusSubject,
                            responseTier = responseTier,
                            starters = quickPrompts,
                            onSelectStarter = { prompt ->
                                onSendMessage(prompt, personality, tasks, timezoneId)
                            }
                        )
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        msg = msg,
                        personality = personality,
                        isCopied = copiedMessageId == msg.id,
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.content))
                            copiedMessageId = msg.id
                        },
                        onSpeak = { onSpeakReply(msg.content) }
                    )
                }

                if (chatState is AiChatUiState.Loading) {
                    item {
                        SenseiThinkingIndicator(personality = personality)
                    }
                }

                if (chatState is AiChatUiState.Streaming) {
                    item {
                        ChatBubble(
                            msg = ChatMessageEntity(
                                chatId = activeChatId.orEmpty(),
                                userId = "",
                                role = "model",
                                content = chatState.partialReply
                            ),
                            personality = personality,
                            isStreaming = true,
                            isCopied = false,
                            onCopy = {},
                            onSpeak = {}
                        )
                    }
                }

                if (chatState is AiChatUiState.Error) {
                    item {
                        HoloCard(
                            accentColor = CrimsonAlert,
                            modifier = Modifier.testTag("ai_error_card")
                        ) {
                            Text(
                                text = chatState.message,
                                color = CrimsonAlert,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onRetryLast,
                                    modifier = Modifier.height(48.dp).testTag("ai_retry_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Scroll")
                                }
                                TextButton(
                                    onClick = onDismissError,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Study Prompts Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { cmd ->
                    AssistChip(
                        onClick = { onSendMessage(cmd, personality, tasks, timezoneId) },
                        enabled = !isBusy,
                        label = { Text(cmd, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            // Voice Status Banner if recognizer is missing
            AnimatedVisibility(visible = voiceNotice != null) {
                Text(
                    text = voiceNotice.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NarutoOrange,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            // Frosted Glass Input Composer
            val composerStyle = ThemeDesignSystem.glassStyle(
                accentColor = HolographicPurple,
                tier = GlassTier.FLOATING_BAR,
                glowActive = true,
                cornerRadius = 22.dp
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .imePadding()
                    .ShinobiGlassModifier(style = composerStyle),
                shape = composerStyle.shape,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = composerStyle.tonalElevation,
                shadowElevation = composerStyle.shadowElevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask ${personality.label} a study question...")
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                voiceNotice = "Voice recognition is not available on this device."
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.size(48.dp).testTag("ai_voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Ask with voice",
                            tint = ChakraCyan
                        )
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            if (voiceNotice != null) voiceNotice = null
                        },
                        placeholder = {
                            Text(
                                text = if (focusSubject.isNotBlank()) {
                                    "Ask ${personality.label} about $focusSubject..."
                                } else {
                                    "Ask AI Sensei a study question..."
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        modifier = Modifier.weight(1f).testTag("ai_chat_input")
                    )
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val toSend = inputText
                                inputText = ""
                                onSendMessage(toSend, personality, tasks, timezoneId)
                            }
                        },
                        enabled = !isBusy && inputText.isNotBlank(),
                        modifier = Modifier.size(52.dp).testTag("ai_send_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = HolographicPurple,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send study question to AI Sensei")
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = if (visuals.isDark) HoloSurfaceElevated else MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Sensei Dojo Scroll History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (chats.isEmpty()) {
                    Text(
                        text = "No saved scrolls yet. Ask AI Sensei your first study question!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    chats.forEach { session ->
                        val isActive = session.id == activeChatId
                        val itemStyle = ThemeDesignSystem.glassStyle(
                            accentColor = HolographicPurple,
                            tier = GlassTier.SUBTLE,
                            glowActive = isActive,
                            cornerRadius = ThemeDesignSystem.CompactCornerRadius,
                            tintColor = if (isActive) HolographicPurple else null
                        )
                        Surface(
                            onClick = {
                                onSelectChat(session.id)
                                showHistorySheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .ShinobiGlassModifier(style = itemStyle, showSpecularEdge = false),
                            shape = itemStyle.shape,
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = itemStyle.tonalElevation
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(session.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        session.lastMessagePreview,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onDeleteChat(session.id) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Chat", tint = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissPendingDelete,
            title = { Text("Confirm Mission Deletion") },
            text = { Text(pendingDelete.description) },
            confirmButton = {
                Button(
                    onClick = onConfirmPendingDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPendingDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SenseiAvatarSeal(sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(NarutoOrange, ChakraCyan, HolographicPurple, NarutoOrange)))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_shinobi_avatar),
            contentDescription = "AI Sensei Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun SenseiWelcomeScroll(
    personality: AiPersonality,
    studyStance: StudyStance,
    focusSubject: String,
    responseTier: AiResponseTier,
    starters: List<String>,
    onSelectStarter: (String) -> Unit
) {
    HoloCard(
        accentColor = HolographicPurple,
        glassTier = GlassTier.HERO,
        glowActive = true,
        modifier = Modifier.testTag("sensei_welcome_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SenseiAvatarSeal(sizeDp = 56)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${personality.label.uppercase()} READY",
                    style = MaterialTheme.typography.titleSmall,
                    color = HolographicPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = personality.ninjaRank,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldRankS,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "\"${personality.motto}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = buildString {
                append("Stance: ${studyStance.label} • Mode: ${responseTier.label}")
                if (focusSubject.isNotBlank()) append(" • Subject: $focusSubject")
                append(". Ask or speak any homework, concept, formula, or exam-prep question below.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TAP A TRAINING SCROLL TO BEGIN",
            style = MaterialTheme.typography.labelSmall,
            color = ChakraCyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            starters.take(3).forEachIndexed { idx, prompt ->
                OutlinedButton(
                    onClick = { onSelectStarter(prompt) },
                    modifier = Modifier.fillMaxWidth().testTag("sensei_starter_$idx"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = HolographicPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SenseiThinkingIndicator(personality: AiPersonality) {
    val infinite = rememberInfiniteTransition(label = "sensei_dots")
    val pulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dots_alpha"
    )
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SenseiAvatarSeal(sizeDp = 28)
        Text(
            text = "${personality.label} is weaving your study scroll...",
            style = MaterialTheme.typography.bodySmall,
            color = HolographicPurple,
            modifier = Modifier.alpha(pulse)
        )
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMessageEntity,
    personality: AiPersonality,
    isStreaming: Boolean = false,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = msg.role == "user"
    val accent = if (isUser) NarutoOrange else HolographicPurple
    val bubbleStyle = ThemeDesignSystem.glassStyle(
        accentColor = accent,
        tier = GlassTier.STANDARD,
        glowActive = !isUser,
        cornerRadius = 18.dp,
        tintColor = if (isUser) NarutoOrange else null
    )
    val timeText = remember(msg.timestampEpochMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestampEpochMs))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(modifier = Modifier.padding(top = 4.dp, end = 8.dp)) {
                SenseiAvatarSeal(sizeDp = 30)
            }
        }
        Surface(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .ShinobiGlassModifier(style = bubbleStyle),
            shape = bubbleStyle.shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = bubbleStyle.tonalElevation
        ) {
            Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "SCHOLAR • $timeText" else "${personality.label.uppercase()} • $timeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) NarutoOrange else ChakraCyan,
                        fontWeight = FontWeight.Bold
                    )
                    if (isStreaming) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = HolographicPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (msg.actionSummary.isNotBlank()) {
                    Text(
                        text = "✓ ${msg.actionSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChakraGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                AnimatedVisibility(visible = !isUser && !isStreaming && msg.content.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = if (isCopied) "Copied scroll" else "Copy answer",
                                tint = if (isCopied) ChakraGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read answer aloud",
                                tint = ChakraCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
