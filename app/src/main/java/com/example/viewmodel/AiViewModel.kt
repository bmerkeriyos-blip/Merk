package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.*
import com.example.core.common.ShinobiTimeUtil
import com.example.core.common.MissionRank
import com.example.core.common.TaskCategory
import com.example.data.local.*
import com.example.data.repository.ShinobiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Loading : AiChatUiState
    data class Streaming(val partialReply: String) : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Scanning : ScannerUiState
    data class Preview(val events: List<ScannedTimetableEvent>) : ScannerUiState
    data class Saved(val count: Int) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

data class PendingDestructiveAction(
    val description: String,
    val matchingTask: PlannerTaskEntity
)

class AiViewModel(
    private val repository: ShinobiRepository,
    private val aiService: AiMentorService,
    private val currentUserId: String
) : ViewModel() {

    val chats: StateFlow<List<ChatSessionEntity>> = repository.observeChats(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val scansHistory: StateFlow<List<TimetableScanEntity>> = repository.observeScans(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _chatState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    private val _studyStance = MutableStateFlow(StudyStance.DOJO_TUTOR)
    val studyStance: StateFlow<StudyStance> = _studyStance.asStateFlow()

    private val _focusSubject = MutableStateFlow("")
    val focusSubject: StateFlow<String> = _focusSubject.asStateFlow()

    private val _scannerState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    private val _dailySuggestion = MutableStateFlow<String?>(null)
    val dailySuggestion: StateFlow<String?> = _dailySuggestion.asStateFlow()

    private val _pendingDeleteConfirmation = MutableStateFlow<PendingDestructiveAction?>(null)
    val pendingDeleteConfirmation: StateFlow<PendingDestructiveAction?> = _pendingDeleteConfirmation.asStateFlow()

    private var lastSubmittedPrompt: String? = null
    private var lastSuggestionSignature: String? = null
    private var lastSuggestionFetchMs: Long = 0L
    private var suggestionInFlight: Boolean = false

    fun setStudyStance(stance: StudyStance) {
        _studyStance.value = stance
    }

    fun setFocusSubject(subject: String) {
        _focusSubject.value = subject
    }

    fun dismissChatError() {
        if (_chatState.value is AiChatUiState.Error) {
            _chatState.value = AiChatUiState.Idle
        }
    }

    fun selectOrCreateChat(personality: AiPersonality = AiPersonality.MENTOR) {
        viewModelScope.launch {
            val currentChats = chats.value
            if (_activeChatId.value == null) {
                if (currentChats.isNotEmpty()) {
                    _activeChatId.value = currentChats.first().id
                } else {
                    startNewChat(personality)
                }
            }
        }
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun startNewChat(personality: AiPersonality = AiPersonality.MENTOR) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val session = ChatSessionEntity(
                id = id,
                userId = currentUserId,
                title = "Dojo Study Scroll",
                personality = personality.code,
                lastMessagePreview = "Sensei is ready. Ask any study question!"
            )
            repository.createOrUpdateChat(session)
            _activeChatId.value = id
            _chatState.value = AiChatUiState.Idle
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                _activeChatId.value = chats.value.firstOrNull { it.id != chatId }?.id
            }
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.deleteAllChats(currentUserId)
            _activeChatId.value = null
        }
    }

    fun retryLastMessage(
        personality: AiPersonality,
        tasks: List<PlannerTaskEntity>,
        timezoneId: String
    ) {
        val retryText = lastSubmittedPrompt ?: return
        sendMessage(
            text = retryText,
            personality = personality,
            tasks = tasks,
            timezoneId = timezoneId,
            saveUserTurn = false
        )
    }

    fun sendMessage(
        text: String,
        personality: AiPersonality,
        tasks: List<PlannerTaskEntity>,
        timezoneId: String,
        stance: StudyStance = _studyStance.value,
        subject: String = _focusSubject.value,
        saveUserTurn: Boolean = true
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (_chatState.value is AiChatUiState.Loading || _chatState.value is AiChatUiState.Streaming) return
        lastSubmittedPrompt = trimmed

        viewModelScope.launch {
            var chatId = _activeChatId.value
            if (chatId == null) {
                chatId = UUID.randomUUID().toString()
                repository.createOrUpdateChat(
                    ChatSessionEntity(
                        id = chatId,
                        userId = currentUserId,
                        title = trimmed.take(36),
                        personality = personality.code,
                        lastMessagePreview = trimmed
                    )
                )
                _activeChatId.value = chatId
            }

            val priorHistory = messages.value.filter { it.chatId == chatId }
            if (saveUserTurn) {
                val userMsg = ChatMessageEntity(
                    chatId = chatId,
                    userId = currentUserId,
                    role = "user",
                    content = trimmed
                )
                repository.saveChatMessage(userMsg)
            }
            _chatState.value = AiChatUiState.Loading

            try {
                val todayIso = ShinobiTimeUtil.nowZoned(timezoneId)
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)

                val shouldClassify = stance == StudyStance.MISSION_PLANNER ||
                    AiMentorService.looksLikeScheduleCommand(trimmed)
                val actionSummary = if (shouldClassify) {
                    val command = aiService.classifyCommand(trimmed, todayIso)
                    executePlannerCommandIfApplicable(command, tasks, todayIso)
                } else {
                    ""
                }

                val scheduleSummary = buildScheduleContext(tasks, todayIso, actionSummary)
                var latestReply = ""
                aiService.streamMentorReply(
                    personality = personality,
                    history = priorHistory,
                    userMessage = trimmed,
                    scheduleContext = scheduleSummary,
                    studyStance = stance,
                    focusSubject = subject
                ).collect { accumulated ->
                    latestReply = accumulated
                    _chatState.value = AiChatUiState.Streaming(accumulated)
                }

                val modelMsg = ChatMessageEntity(
                    chatId = chatId,
                    userId = currentUserId,
                    role = "model",
                    content = latestReply,
                    actionSummary = actionSummary
                )
                repository.saveChatMessage(modelMsg)
                repository.createOrUpdateChat(
                    ChatSessionEntity(
                        id = chatId,
                        userId = currentUserId,
                        title = trimmed.take(36),
                        personality = personality.code,
                        lastMessagePreview = latestReply.take(80)
                    )
                )
                _chatState.value = AiChatUiState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e("AiViewModel", "AI mentor timed out", e)
                _chatState.value = AiChatUiState.Error("AI Mentor timed out after 60s. Check connection and retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiViewModel", "AI mentor error", e)
                _chatState.value = AiChatUiState.Error(formatAiError(e, "AI Mentor request failed."))
            }
        }
    }

    private suspend fun executePlannerCommandIfApplicable(
        command: ParsedAiCommand,
        tasks: List<PlannerTaskEntity>,
        todayIso: String
    ): String {
        return when (command) {
            is ParsedAiCommand.CreateTask -> {
                val rank = MissionRank.fromCode(command.missionRank)
                val cat = TaskCategory.fromCode(command.category)
                val newTask = PlannerTaskEntity(
                    userId = currentUserId,
                    title = command.title,
                    subjectName = command.subjectName.ifBlank { "General" },
                    category = cat.code,
                    dateIso = command.dateIso,
                    startTimeMinutes = command.startTimeMinutes,
                    endTimeMinutes = command.endTimeMinutes,
                    priority = command.priority,
                    missionRank = rank.code,
                    xpReward = rank.baseXp,
                    colorHex = cat.defaultColorHex,
                    location = command.location
                )
                repository.saveTask(newTask)
                "Created mission '${newTask.title}' on ${newTask.dateIso} at ${ShinobiTimeUtil.formatTime(newTask.startTimeMinutes, false)}"
            }
            is ParsedAiCommand.RescheduleTask -> {
                val match = tasks.firstOrNull {
                    command.searchKeyword.isNotBlank() &&
                        it.title.contains(command.searchKeyword, ignoreCase = true)
                }
                if (match != null) {
                    val dur = (match.endTimeMinutes - match.startTimeMinutes).coerceAtLeast(30)
                    val newStart = command.newStartMinutes ?: match.startTimeMinutes
                    val updated = match.copy(
                        dateIso = command.newDateIso,
                        startTimeMinutes = newStart,
                        endTimeMinutes = (newStart + dur).coerceAtMost(1439)
                    )
                    repository.saveTask(updated)
                    "Rescheduled '${match.title}' to ${updated.dateIso}"
                } else {
                    "No existing task matched '${command.searchKeyword}'"
                }
            }
            is ParsedAiCommand.DeleteTaskRequest -> {
                val match = tasks.firstOrNull {
                    command.searchKeyword.isNotBlank() &&
                        it.title.contains(command.searchKeyword, ignoreCase = true)
                }
                if (match != null) {
                    _pendingDeleteConfirmation.value = PendingDestructiveAction(
                        description = "Delete mission '${match.title}' on ${match.dateIso}?",
                        matchingTask = match
                    )
                    "Awaiting user confirmation before deleting '${match.title}'"
                } else ""
            }
            is ParsedAiCommand.FindFreeTime -> {
                val dayTasks = tasks.filter { it.dateIso == command.dateIso }
                val slots = repository.findFreeTimeSlots(dayTasks)
                if (slots.isEmpty()) "No free blocks >=30m found on ${command.dateIso}"
                else "Free slots on ${command.dateIso}: " + slots.take(4).joinToString {
                    "${ShinobiTimeUtil.formatTime(it.startMinutes, false)}-${ShinobiTimeUtil.formatTime(it.endMinutes, false)}"
                }
            }
            ParsedAiCommand.SummarizeWeek, ParsedAiCommand.GenerateStudyPlan, ParsedAiCommand.ChatOnly -> ""
        }
    }

    fun confirmPendingDelete() {
        val pending = _pendingDeleteConfirmation.value ?: return
        _pendingDeleteConfirmation.value = null
        viewModelScope.launch {
            repository.deleteTask(pending.matchingTask)
        }
    }

    fun dismissPendingDelete() {
        _pendingDeleteConfirmation.value = null
    }

    private fun buildScheduleContext(
        tasks: List<PlannerTaskEntity>,
        todayIso: String,
        actionSummary: String
    ): String {
        val upcoming = tasks.filter { it.dateIso >= todayIso && !it.completed }.take(12)
        val lines = upcoming.joinToString("\n") {
            "- ${it.dateIso} ${ShinobiTimeUtil.formatTime(it.startTimeMinutes, true)}-${ShinobiTimeUtil.formatTime(it.endTimeMinutes, true)}: ${it.title} [${it.subjectName}, ${it.category}, Rank ${it.missionRank}]"
        }.ifBlank { "No upcoming open missions." }
        return buildString {
            appendLine("Today: $todayIso")
            if (actionSummary.isNotBlank()) appendLine("System action executed: $actionSummary")
            appendLine("Upcoming schedule:")
            append(lines)
        }
    }

    fun refreshDailySuggestion(
        tasks: List<PlannerTaskEntity>,
        timezoneId: String,
        force: Boolean = false
    ) {
        val todayIso = ShinobiTimeUtil.nowZoned(timezoneId)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val signature = "$todayIso|${tasks.size}|${tasks.count { it.completed }}|${tasks.firstOrNull { !it.completed }?.id.orEmpty()}"
        val nowMs = System.currentTimeMillis()
        if (!force) {
            if (suggestionInFlight) return
            if (signature == lastSuggestionSignature && _dailySuggestion.value != null) return
            if (_dailySuggestion.value != null && nowMs - lastSuggestionFetchMs < 60_000L) return
        }

        suggestionInFlight = true
        viewModelScope.launch {
            try {
                _dailySuggestion.value = aiService.generateQuickSuggestion(tasks, todayIso)
                lastSuggestionSignature = signature
                lastSuggestionFetchMs = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AiViewModel", "Daily suggestion refresh failed: ${e.message}")
            } finally {
                suggestionInFlight = false
            }
        }
    }

    fun scanFromImageUri(
        context: Context,
        uri: Uri,
        sourceType: String,
        accurateMode: Boolean,
        autoSave: Boolean,
        timezoneId: String
    ) {
        viewModelScope.launch {
            _scannerState.value = ScannerUiState.Scanning
            try {
                val bitmaps = withContext(Dispatchers.IO) {
                    if (sourceType == "PDF") renderPdfPages(context, uri)
                    else listOfNotNull(loadScaledBitmap(context, uri))
                }
                if (bitmaps.isEmpty()) {
                    _scannerState.value = ScannerUiState.Error("Could not decode image or PDF file.")
                    return@launch
                }
                val todayIso = ShinobiTimeUtil.nowZoned(timezoneId)
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                val extracted = aiService.scanTimetableImages(bitmaps, todayIso, accurateMode)
                if (extracted.isEmpty()) {
                    _scannerState.value = ScannerUiState.Error("No timetable events detected. Try Accurate Mode or a clearer crop.")
                } else if (autoSave) {
                    saveScannedEvents(extracted, sourceType, accurateMode)
                } else {
                    _scannerState.value = ScannerUiState.Preview(extracted)
                }
            } catch (e: TimeoutCancellationException) {
                _scannerState.value = ScannerUiState.Error("Timetable scan timed out. Try Quick Mode or fewer pages.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiViewModel", "Timetable scan error", e)
                _scannerState.value = ScannerUiState.Error(formatAiError(e, "Timetable scan failed."))
            }
        }
    }

    fun updateScannedEvent(index: Int, updated: ScannedTimetableEvent) {
        val current = (_scannerState.value as? ScannerUiState.Preview)?.events ?: return
        if (index !in current.indices) return
        val mutable = current.toMutableList()
        mutable[index] = updated
        _scannerState.value = ScannerUiState.Preview(mutable)
    }

    fun removeScannedEvent(index: Int) {
        val current = (_scannerState.value as? ScannerUiState.Preview)?.events ?: return
        if (index !in current.indices) return
        val mutable = current.toMutableList()
        mutable.removeAt(index)
        _scannerState.value = ScannerUiState.Preview(mutable)
    }

    fun confirmAndSaveScannedEvents(sourceType: String, accurateMode: Boolean) {
        val events = (_scannerState.value as? ScannerUiState.Preview)?.events ?: return
        viewModelScope.launch {
            saveScannedEvents(events, sourceType, accurateMode)
        }
    }

    private suspend fun saveScannedEvents(
        events: List<ScannedTimetableEvent>,
        sourceType: String,
        accurateMode: Boolean
    ) {
        events.forEach { ev ->
            val rank = MissionRank.fromCode(ev.missionRank)
            val cat = TaskCategory.fromCode(ev.category)
            val validDate = runCatching { LocalDate.parse(ev.dateIso) }.getOrNull()
                ?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val notes = if (ev.teacher.isNotBlank()) "Instructor: ${ev.teacher}" else ""
            repository.saveTask(
                PlannerTaskEntity(
                    userId = currentUserId,
                    title = ev.title.ifBlank { ev.subject },
                    notes = notes,
                    subjectName = ev.subject,
                    category = cat.code,
                    dateIso = validDate,
                    startTimeMinutes = ev.startTimeMinutes,
                    endTimeMinutes = ev.endTimeMinutes,
                    missionRank = rank.code,
                    xpReward = rank.baseXp,
                    colorHex = cat.defaultColorHex,
                    recurrence = ev.recurrence,
                    location = ev.location
                )
            )
        }
        repository.recordTimetableScan(
            TimetableScanEntity(
                userId = currentUserId,
                sourceType = sourceType,
                mode = if (accurateMode) "ACCURATE" else "QUICK",
                extractedCount = events.size,
                summary = events.take(3).joinToString { it.subject }
            )
        )
        _scannerState.value = ScannerUiState.Saved(events.size)
    }

    fun resetScanner() {
        _scannerState.value = ScannerUiState.Idle
    }

    private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val raw = BitmapFactory.decodeStream(stream) ?: return null
            val maxDim = 1440
            if (raw.width <= maxDim && raw.height <= maxDim) return raw
            val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }

    private fun renderPdfPages(context: Context, uri: Uri): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return result
        pfd.use { fd ->
            PdfRenderer(fd).use { renderer ->
                val count = minOf(renderer.pageCount, 3)
                for (i in 0 until count) {
                    renderer.openPage(i).use { page ->
                        val bmp = Bitmap.createBitmap(
                            (page.width * 2).coerceAtMost(1440),
                            (page.height * 2).coerceAtMost(1920),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        result.add(bmp)
                    }
                }
            }
        }
        return result
    }

    private fun formatAiError(e: Exception, fallback: String): String {
        val raw = e.localizedMessage?.takeIf { it.isNotBlank() } ?: return fallback
        return if (raw.contains("Default FirebaseApp is not initialized", ignoreCase = true)) {
            "Firebase is not initialized in this build (missing google-services.json): $raw"
        } else {
            raw
        }
    }
}
