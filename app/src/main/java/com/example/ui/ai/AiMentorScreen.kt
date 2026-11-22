package com.example.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.AiPersonality
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PlannerTaskEntity
import com.example.ui.components.GlassTier
import com.example.ui.components.HoloCard
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.ChakraGreen
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.HoloSurfaceElevated
import com.example.ui.theme.HolographicPurple
import com.example.ui.theme.LocalShinobiVisuals
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NarutoOrange
import com.example.ui.theme.ShinobiGlassModifier
import com.example.ui.theme.ThemeDesignSystem
import com.example.viewmodel.AiChatUiState
import com.example.viewmodel.PendingDestructiveAction

private val QUICK_COMMANDS = listOf(
    "Add mathematics study tomorrow at 4 PM",
    "Find free time tomorrow",
    "Create a two-hour physics revision session",
    "Summarize my upcoming schedule & streak strategy"
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
    onBack: () -> Unit,
    onSelectPersonality: (AiPersonality) -> Unit,
    onSelectOrCreateChat: (AiPersonality) -> Unit,
    onStartNewChat: (AiPersonality) -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onSendMessage: (String, AiPersonality, List<PlannerTaskEntity>, String) -> Unit,
    onConfirmPendingDelete: () -> Unit,
    onDismissPendingDelete: () -> Unit,
    onOpenScanner: () -> Unit
) {
    BackHandler { onBack() }

    val visuals = LocalShinobiVisuals.current
    val personality = remember(currentPersonalityCode) { AiPersonality.fromCode(currentPersonalityCode) }
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onSelectOrCreateChat(personality)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = HolographicPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("SHINOBI SENSEI AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(personality.label, style = MaterialTheme.typography.labelSmall, color = HolographicPurple)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_mentor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenScanner, modifier = Modifier.testTag("ai_open_scanner_button")) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = "Open AI Timetable Scanner", tint = ChakraCyan)
                    }
                    IconButton(onClick = { showHistorySheet = true }, modifier = Modifier.testTag("ai_history_button")) {
                        Icon(Icons.Filled.History, contentDescription = "Chat History")
                    }
                    IconButton(onClick = { onStartNewChat(personality) }, modifier = Modifier.testTag("ai_new_chat_button")) {
                        Icon(Icons.Filled.AddComment, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (visuals.isDark) 0.80f else 0.94f)
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
            // Personality selector strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

            // Messages Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && chatState is AiChatUiState.Idle) {
                    item {
                        HoloCard(
                            accentColor = HolographicPurple,
                            glassTier = GlassTier.HERO,
                            glowActive = true
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = HolographicPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shinobi Sensei Ready",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HolographicPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ask me to schedule missions, find free study slots, build revision routines, or plan your training!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(msg = msg)
                }

                if (chatState is AiChatUiState.Loading) {
                    item {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = HolographicPurple)
                            Text("Sensei is formulating your scroll...", style = MaterialTheme.typography.bodySmall, color = HolographicPurple)
                        }
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
                            )
                        )
                    }
                }

                if (chatState is AiChatUiState.Error) {
                    item {
                        HoloCard(accentColor = CrimsonAlert) {
                            Text(chatState.message, color = CrimsonAlert, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Quick Prompt Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_COMMANDS) { cmd ->
                    AssistChip(
                        onClick = {
                            onSendMessage(cmd, personality, tasks, timezoneId)
                        },
                        enabled = chatState !is AiChatUiState.Loading && chatState !is AiChatUiState.Streaming,
                        label = { Text(cmd, maxLines = 1) }
                    )
                }
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
                tonalElevation = composerStyle.tonalElevation,
                shadowElevation = composerStyle.shadowElevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask your Shinobi Sensei...") },
                        maxLines = 3,
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
                        enabled = chatState !is AiChatUiState.Loading && chatState !is AiChatUiState.Streaming,
                        modifier = Modifier.size(52.dp).testTag("ai_send_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = HolographicPurple,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message to AI Mentor")
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
                Text("Mentor Chat History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        tonalElevation = itemStyle.tonalElevation
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(session.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(session.lastMessagePreview, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            IconButton(onClick = { onDeleteChat(session.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Chat", tint = CrimsonAlert)
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
private fun ChatBubble(msg: ChatMessageEntity) {
    val isUser = msg.role == "user"
    val accent = if (isUser) NarutoOrange else HolographicPurple
    val bubbleStyle = ThemeDesignSystem.glassStyle(
        accentColor = accent,
        tier = GlassTier.STANDARD,
        glowActive = !isUser,
        cornerRadius = 18.dp,
        tintColor = if (isUser) NarutoOrange else null
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .ShinobiGlassModifier(style = bubbleStyle),
            shape = bubbleStyle.shape,
            color = Color.Transparent,
            tonalElevation = bubbleStyle.tonalElevation
        ) {
            Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            }
        }
    }
}
