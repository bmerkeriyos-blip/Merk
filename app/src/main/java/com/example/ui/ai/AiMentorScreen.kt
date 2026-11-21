package com.example.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.AiPersonality
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PlannerTaskEntity
import com.example.ui.components.HoloCard
import com.example.ui.theme.*
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SHINOBI SENSEI AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(personality.label, style = MaterialTheme.typography.labelSmall, color = visuals.primaryAccent)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_mentor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenScanner, modifier = Modifier.testTag("ai_open_scanner_button")) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = "Open AI Timetable Scanner", tint = JadeEmerald)
                    }
                    IconButton(onClick = { showHistorySheet = true }, modifier = Modifier.testTag("ai_history_button")) {
                        Icon(Icons.Filled.History, contentDescription = "Chat History")
                    }
                    IconButton(onClick = { onStartNewChat(personality) }, modifier = Modifier.testTag("ai_new_chat_button")) {
                        Icon(Icons.Filled.AddComment, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
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
                        HoloCard(accentColor = ArcaneViolet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ArcaneViolet)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Shinobi Sensei Ready", style = MaterialTheme.typography.titleSmall, color = ArcaneViolet)
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
                    ChatBubble(msg = msg, primaryAccent = visuals.primaryAccent)
                }

                if (chatState is AiChatUiState.Loading) {
                    item {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Sensei is writing on your scroll...", style = MaterialTheme.typography.bodySmall)
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
                            ),
                            primaryAccent = visuals.primaryAccent
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
                        label = { Text(cmd, maxLines = 1) }
                    )
                }
            }

            // Input Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .imePadding(),
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
                        containerColor = visuals.primaryAccent,
                        contentColor = MidnightNavy
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message to AI Mentor")
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Mentor Chat History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                chats.forEach { session ->
                    Surface(
                        onClick = {
                            onSelectChat(session.id)
                            showHistorySheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (session.id == activeChatId) visuals.primaryAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
private fun ChatBubble(msg: ChatMessageEntity, primaryAccent: androidx.compose.ui.graphics.Color) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) primaryAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, if (isUser) primaryAccent.copy(alpha = 0.6f) else ArcaneViolet.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (msg.actionSummary.isNotBlank()) {
                    Text(
                        text = "✓ ${msg.actionSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JadeEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
