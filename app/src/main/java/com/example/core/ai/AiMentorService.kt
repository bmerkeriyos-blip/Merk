package com.example.core.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PlannerTaskEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiPersonality(
    val code: String,
    val label: String,
    val ninjaRank: String,
    val motto: String,
    val systemTone: String
) {
    MENTOR(
        code = "MENTOR",
        label = "Shinobi Sensei",
        ninjaRank = "Hokage Study Master",
        motto = "Master the fundamentals step by step, young shinobi, and every S-rank exam yields.",
        systemTone = "You are Shinobi Sensei, a wise, warm, and encouraging anime ninja academy master mentoring a student. Use subtle, vivid shinobi imagery (training scrolls, chakra focus, Genin-to-Kage mastery) while keeping academic explanations crystal clear, accurate, and practical."
    ),
    STRICT(
        code = "STRICT",
        label = "Jonin Captain",
        ninjaRank = "ANBU Exam Commander",
        motto = "Precision and steady training turn the hardest syllabus into second nature.",
        systemTone = "You are a disciplined Jonin squad captain tutoring a scholar. Keep explanations razor-sharp, rigorous, structured, and accountable—like preparing a squad for the Chunin Exams."
    ),
    FRIENDLY(
        code = "FRIENDLY",
        label = "Squad Teammate",
        ninjaRank = "Hidden Leaf Study Ally",
        motto = "We are cracking this subject together—focus your chakra and let's solve it!",
        systemTone = "You are a loyal, energetic Hidden Leaf squadmate studying side by side with the user. Explain tricky concepts in approachable, relatable ways with upbeat anime camaraderie."
    ),
    PROFESSIONAL(
        code = "PROFESSIONAL",
        label = "Kage Advisor",
        ninjaRank = "Konoha Grand Strategist",
        motto = "Calm strategy, structured mental models, and deliberate recall win the semester.",
        systemTone = "You are a composed Hokage-tower academic strategist. Deliver structured outlines, formal definitions, clean examples, and calm tactical exam guidance."
    ),
    COMPETITIVE(
        code = "COMPETITIVE",
        label = "Eternal Rival",
        ninjaRank = "S-Rank Training Rival",
        motto = "Show me your best answer—let's surpass yesterday's mastery score!",
        systemTone = "You are a spirited friendly shinobi rival pushing the student to sharpen their thinking, challenge assumptions, and aim for S-rank mastery on every question."
    );

    companion object {
        fun fromCode(raw: String): AiPersonality =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: MENTOR
    }
}

enum class StudyStance(
    val code: String,
    val label: String,
    val promptDirective: String
) {
    DOJO_TUTOR(
        code = "DOJO_TUTOR",
        label = "Sensei Tutor",
        promptDirective = "Act as an expert ninja study tutor: explain the concept clearly with an intuitive example (and a light shinobi analogy if helpful), highlight common pitfalls, and end with 1 quick check-for-understanding question."
    ),
    STEP_BY_STEP(
        code = "STEP_BY_STEP",
        label = "Step-by-Step",
        promptDirective = "Break down the question into numbered tactical scroll steps (Step 1, Step 2, ...). Show intermediate reasoning, equations, or code clearly so the student learns the exact solving technique."
    ),
    CHUNIN_QUIZ(
        code = "CHUNIN_QUIZ",
        label = "Chunin Quiz",
        promptDirective = "Quiz mode: if the student asks to be quizzed on a topic, pose 3 progressive practice questions labeled [Genin], [Chunin], and [Jonin] (wait for their attempt before revealing full answers, or give brief collapsible-style hints). If they submitted an answer, grade it fairly and explain why."
    ),
    MEMORY_SCROLL(
        code = "MEMORY_SCROLL",
        label = "Memory Scroll",
        promptDirective = "Create a high-yield study cheat-sheet scroll: Core Idea, Key Formulas/Rules, Common Exam Trap, and a memorable Mnemonic Jutsu to lock it in long-term memory."
    ),
    MISSION_PLANNER(
        code = "MISSION_PLANNER",
        label = "Planner & Tutor",
        promptDirective = "Support both academic study tutoring and tactical mission/schedule planning based on the student's calendar."
    );

    companion object {
        fun fromCode(raw: String): StudyStance =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: DOJO_TUTOR
    }
}

enum class AiResponseTier(
    val code: String,
    val label: String,
    val description: String
) {
    FAST(
        code = "FAST",
        label = "Quick Flash",
        description = "Ultra-fast answers & quick definitions"
    ),
    BALANCED(
        code = "BALANCED",
        label = "Standard Chat",
        description = "Balanced daily study & tutoring"
    ),
    DEEP_REASONING(
        code = "DEEP_REASONING",
        label = "Deep Reasoning",
        description = "Multi-step STEM, proofs & complex coding"
    );

    companion object {
        fun fromCode(raw: String): AiResponseTier =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: BALANCED
    }
}

data class ScannedTimetableEvent(
    val subject: String,
    val title: String,
    val dayOfWeek: String,
    val dateIso: String,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val location: String,
    val teacher: String,
    val category: String,
    val missionRank: String,
    val recurrence: String
)

sealed interface ParsedAiCommand {
    data class CreateTask(
        val title: String,
        val subjectName: String,
        val category: String,
        val dateIso: String,
        val startTimeMinutes: Int,
        val endTimeMinutes: Int,
        val missionRank: String,
        val priority: String,
        val location: String
    ) : ParsedAiCommand

    data class RescheduleTask(
        val searchKeyword: String,
        val newDateIso: String,
        val newStartMinutes: Int?
    ) : ParsedAiCommand

    data class DeleteTaskRequest(val searchKeyword: String) : ParsedAiCommand
    data class FindFreeTime(val dateIso: String) : ParsedAiCommand
    data object SummarizeWeek : ParsedAiCommand
    data object GenerateStudyPlan : ParsedAiCommand
    data object ChatOnly : ParsedAiCommand
}

interface ShinobiAiBackend {
    suspend fun generateContent(
        modelName: String,
        promptText: String,
        jsonSchema: Schema? = null
    ): String?

    suspend fun generateMultimodalContent(
        modelName: String,
        promptText: String,
        images: List<Bitmap>,
        jsonSchema: Schema
    ): String?

    fun streamChatReply(
        modelName: String,
        systemInstruction: String = "",
        history: List<Content>,
        userMessage: String
    ): Flow<String>
}

class FirebaseShinobiAiBackend : ShinobiAiBackend {
    private val ai by lazy { Firebase.ai }

    override suspend fun generateContent(
        modelName: String,
        promptText: String,
        jsonSchema: Schema?
    ): String? {
        val model = if (jsonSchema != null) {
            ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = jsonSchema
                }
            )
        } else {
            ai.generativeModel(modelName = modelName)
        }
        return model.generateContent(promptText).text
    }

    override suspend fun generateMultimodalContent(
        modelName: String,
        promptText: String,
        images: List<Bitmap>,
        jsonSchema: Schema
    ): String? {
        val model = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = jsonSchema
            }
        )
        val promptContent = content {
            text(promptText)
            images.forEach { bmp -> image(bmp) }
        }
        return model.generateContent(promptContent).text
    }

    override fun streamChatReply(
        modelName: String,
        systemInstruction: String,
        history: List<Content>,
        userMessage: String
    ): Flow<String> = flow {
        val model = if (systemInstruction.isNotBlank()) {
            ai.generativeModel(
                modelName = modelName,
                systemInstruction = content { text(systemInstruction) }
            )
        } else {
            ai.generativeModel(modelName = modelName)
        }
        val chat = model.startChat(history = history)
        chat.sendMessageStream(userMessage).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) emit(piece)
        }
    }
}

class AiMentorService(
    private val backend: ShinobiAiBackend = FirebaseShinobiAiBackend(),
    private val retryDelayMs: Long = 350L
) {
    companion object {
        const val PRIMARY_MODEL = "gemini-3.8-flash"
        const val LITE_MODEL = "gemini-3.5-flash-lite"
        const val DEEP_REASONING_MODEL = "gemini-4-argon"

        internal fun modelsForTier(tier: AiResponseTier): List<String> = when (tier) {
            AiResponseTier.FAST -> listOf(LITE_MODEL, PRIMARY_MODEL)
            AiResponseTier.BALANCED -> listOf(PRIMARY_MODEL, LITE_MODEL)
            AiResponseTier.DEEP_REASONING -> listOf(DEEP_REASONING_MODEL, PRIMARY_MODEL, LITE_MODEL)
        }

        internal fun sanitizeChatHistory(
            history: List<ChatMessageEntity>,
            pendingUserMessage: String
        ): List<Content> {
            val cleaned = mutableListOf<Pair<String, String>>()
            for (msg in history) {
                val text = msg.content.trim()
                if (text.isEmpty()) continue
                val role = if (msg.role.equals("model", ignoreCase = true)) "model" else "user"
                if (cleaned.isEmpty() && role == "model") continue
                if (cleaned.isNotEmpty() && cleaned.last().first == role) {
                    val prev = cleaned.removeAt(cleaned.lastIndex)
                    cleaned.add(role to "${prev.second}\n\n$text")
                } else {
                    cleaned.add(role to text)
                }
            }

            // Strip trailing user turn(s) so pending sendMessageStream(userMessage) never follows a user turn
            val trimmedPending = pendingUserMessage.trim()
            if (cleaned.isNotEmpty() && cleaned.last().first == "user") {
                if (cleaned.last().second == trimmedPending || cleaned.size % 2 == 1) {
                    cleaned.removeAt(cleaned.lastIndex)
                }
            }
            while (cleaned.isNotEmpty() && cleaned.last().first != "model") {
                cleaned.removeAt(cleaned.lastIndex)
            }

            val window = cleaned.takeLast(12).toMutableList()
            while (window.isNotEmpty() && window.first().first != "user") {
                window.removeAt(0)
            }

            return window.map { (role, text) ->
                content(role) { text(text) }
            }
        }

        internal fun isQuotaOrRateLimitError(t: Throwable): Boolean {
            var cur: Throwable? = t
            while (cur != null) {
                val msg = cur.message.orEmpty().lowercase()
                if (
                    msg.contains("429") ||
                    msg.contains("resource_exhausted") ||
                    msg.contains("quota") ||
                    msg.contains("rate limit") ||
                    msg.contains("too many requests") ||
                    msg.contains("503") ||
                    msg.contains("unavailable")
                ) {
                    return true
                }
                cur = cur.cause
            }
            return false
        }

        internal fun isBillingOrModelUnavailableError(t: Throwable): Boolean {
            var cur: Throwable? = t
            while (cur != null) {
                val msg = cur.message.orEmpty().lowercase()
                if (
                    msg.contains("billing") ||
                    msg.contains("blaze") ||
                    msg.contains("model not found") ||
                    msg.contains("not supported") ||
                    msg.contains("permission denied") ||
                    msg.contains("404")
                ) {
                    return true
                }
                cur = cur.cause
            }
            return false
        }

        internal fun looksLikeScheduleCommand(prompt: String): Boolean {
            val q = prompt.trim().lowercase()
            val explicitStudyPrefixes = listOf(
                "explain ", "what is ", "what are ", "why ", "how does ", "how do ", "how to solve",
                "quiz me", "test me", "define ", "solve ", "prove ", "derive ", "compare ",
                "mnemonic", "cheat sheet", "flashcard", "help me understand"
            )
            if (explicitStudyPrefixes.any { q.startsWith(it) }) return false
            val scheduleKeywords = listOf(
                "add ", "schedule ", "reschedule", "move ", "delete ", "remove ",
                "free time", "free slot", "summarize my week", "summarize my upcoming",
                "study plan", "timetable", "tomorrow at", "today at"
            )
            return scheduleKeywords.any { q.contains(it) }
        }
    }

    private val commandSchema = Schema.obj(
        properties = mapOf(
            "actionType" to Schema.enumeration(
                values = listOf(
                    "CREATE_TASK",
                    "RESCHEDULE_TASK",
                    "DELETE_TASK",
                    "FIND_FREE_TIME",
                    "SUMMARIZE_WEEK",
                    "GENERATE_STUDY_PLAN",
                    "CHAT_ONLY"
                ),
                description = "Intent classified from the student's message"
            ),
            "title" to Schema.string("Task or session title"),
            "subjectName" to Schema.string("Academic subject name"),
            "category" to Schema.enumeration(
                listOf("CLASS", "STUDY", "ASSIGNMENT", "EXAM", "DEADLINE", "PERSONAL", "BREAK", "EXERCISE")
            ),
            "dateIso" to Schema.string("Target date in yyyy-MM-dd format"),
            "startTimeMinutes" to Schema.integer("Minutes from midnight 0..1439, e.g. 960 for 4:00 PM"),
            "endTimeMinutes" to Schema.integer("Minutes from midnight 0..1439"),
            "missionRank" to Schema.enumeration(listOf("D", "C", "B", "A", "S")),
            "priority" to Schema.enumeration(listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")),
            "location" to Schema.string("Room or location if specified"),
            "searchKeyword" to Schema.string("Keyword matching existing task for move/delete")
        ),
        optionalProperties = listOf(
            "title", "subjectName", "category", "dateIso", "startTimeMinutes",
            "endTimeMinutes", "missionRank", "priority", "location", "searchKeyword"
        )
    )

    private val timetableSchema = Schema.obj(
        properties = mapOf(
            "events" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "subject" to Schema.string("Subject or course code/name"),
                        "title" to Schema.string("Concise event title"),
                        "dayOfWeek" to Schema.string("Day name e.g. MONDAY"),
                        "dateIso" to Schema.string("Resolved yyyy-MM-dd date"),
                        "startHour" to Schema.integer("Start hour 0..23"),
                        "startMinute" to Schema.integer("Start minute 0..59"),
                        "endHour" to Schema.integer("End hour 0..23"),
                        "endMinute" to Schema.integer("End minute 0..59"),
                        "location" to Schema.string("Classroom or building"),
                        "teacher" to Schema.string("Instructor name if visible"),
                        "category" to Schema.enumeration(listOf("CLASS", "STUDY", "ASSIGNMENT", "EXAM", "DEADLINE")),
                        "missionRank" to Schema.enumeration(listOf("D", "C", "B", "A", "S")),
                        "recurrence" to Schema.enumeration(listOf("NONE", "DAILY", "WEEKLY", "WEEKDAYS"))
                    ),
                    optionalProperties = listOf("location", "teacher")
                )
            )
        )
    )

    private suspend fun <T> executeWithQuotaResilience(
        primaryModel: String,
        fallbackModel: String?,
        block: suspend (modelName: String) -> T
    ): T {
        val candidates = listOfNotNull(primaryModel, fallbackModel).distinct()
        var lastError: Throwable? = null
        for ((idx, modelName) in candidates.withIndex()) {
            try {
                return block(modelName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                val hasFallback = idx < candidates.lastIndex
                if (hasFallback && isQuotaOrRateLimitError(e)) {
                    if (retryDelayMs > 0L) delay(retryDelayMs)
                    continue
                }
                throw e
            }
        }
        throw lastError ?: IllegalStateException("AI request failed.")
    }

    suspend fun classifyCommand(
        userPrompt: String,
        todayIso: String
    ): ParsedAiCommand {
        val prompt = """
            Today's date is $todayIso.
            Classify the user's request into a planner action if they are asking to create, reschedule, move, delete a task/class/study session, find free time, summarize their week, or generate a study timetable. Otherwise return CHAT_ONLY.
            User request: "$userPrompt"
        """.trimIndent()
        return try {
            val raw = withTimeout(30_000L) {
                executeWithQuotaResilience(LITE_MODEL, PRIMARY_MODEL) { model ->
                    backend.generateContent(model, prompt, commandSchema)
                }
            } ?: return ParsedAiCommand.ChatOnly
            val json = JSONObject(raw)
            when (json.optString("actionType", "CHAT_ONLY")) {
                "CREATE_TASK" -> {
                    val start = json.optInt("startTimeMinutes", 16 * 60).coerceIn(0, 1400)
                    val end = json.optInt("endTimeMinutes", start + 60).coerceIn(start + 15, 1439)
                    ParsedAiCommand.CreateTask(
                        title = json.optString("title", "Study Session").ifBlank { "Study Session" },
                        subjectName = json.optString("subjectName", "General"),
                        category = json.optString("category", "STUDY"),
                        dateIso = json.optString("dateIso", todayIso).ifBlank { todayIso },
                        startTimeMinutes = start,
                        endTimeMinutes = end,
                        missionRank = json.optString("missionRank", "C"),
                        priority = json.optString("priority", "MEDIUM"),
                        location = json.optString("location", "")
                    )
                }
                "RESCHEDULE_TASK" -> ParsedAiCommand.RescheduleTask(
                    searchKeyword = json.optString("searchKeyword", ""),
                    newDateIso = json.optString("dateIso", todayIso).ifBlank { todayIso },
                    newStartMinutes = if (json.has("startTimeMinutes")) json.optInt("startTimeMinutes") else null
                )
                "DELETE_TASK" -> ParsedAiCommand.DeleteTaskRequest(
                    searchKeyword = json.optString("searchKeyword", "")
                )
                "FIND_FREE_TIME" -> ParsedAiCommand.FindFreeTime(
                    dateIso = json.optString("dateIso", todayIso).ifBlank { todayIso }
                )
                "SUMMARIZE_WEEK" -> ParsedAiCommand.SummarizeWeek
                "GENERATE_STUDY_PLAN" -> ParsedAiCommand.GenerateStudyPlan
                else -> ParsedAiCommand.ChatOnly
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AiMentorService", "classifyCommand fell back to CHAT_ONLY: ${e.message}")
            ParsedAiCommand.ChatOnly
        }
    }

    fun streamMentorReply(
        personality: AiPersonality,
        history: List<ChatMessageEntity>,
        userMessage: String,
        scheduleContext: String,
        studyStance: StudyStance = StudyStance.DOJO_TUTOR,
        focusSubject: String = "",
        responseTier: AiResponseTier = AiResponseTier.BALANCED
    ): Flow<String> = flow {
        val subjectLine = if (focusSubject.isNotBlank()) {
            "Active study subject focus: $focusSubject"
        } else {
            "Active study subject focus: General Academic Training"
        }
        val systemAndContext = """
            ${personality.systemTone}
            Character rank: ${personality.ninjaRank}.
            Tutoring stance (${studyStance.label}): ${studyStance.promptDirective}
            Response depth mode: ${responseTier.label} (${responseTier.description}).
            $subjectLine
            Current student academic context:
            $scheduleContext
            Formatting rules for mobile chat:
            - Address the student naturally as a shinobi scholar in character, without sacrificing technical or academic accuracy.
            - Keep paragraphs crisp and scannable with bullet points or numbered steps when teaching.
            - Never invent fake grades, fake test scores, or claim a schedule action happened unless confirmed in the system context above.
        """.trimIndent()

        val chatHistory = sanitizeChatHistory(history, userMessage)

        val modelsToTry = modelsForTier(responseTier)
        var emittedAny = false
        var lastError: Throwable? = null

        for ((index, modelName) in modelsToTry.withIndex()) {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    backend.streamChatReply(
                        modelName = modelName,
                        systemInstruction = systemAndContext,
                        history = chatHistory,
                        userMessage = userMessage
                    ).collect { piece ->
                        if (piece.isNotEmpty()) {
                            emittedAny = true
                            accumulated.append(piece)
                            emit(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isBlank()) {
                    throw IllegalStateException("AI Mentor returned an empty response.")
                }
                return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                val allowFallback = isQuotaOrRateLimitError(e) ||
                    (modelName == DEEP_REASONING_MODEL && isBillingOrModelUnavailableError(e))
                val canFallback = index < modelsToTry.lastIndex && !emittedAny && allowFallback
                if (canFallback) {
                    if (retryDelayMs > 0L) delay(retryDelayMs)
                    continue
                }
                throw e
            }
        }
        throw lastError ?: IllegalStateException("AI Mentor returned an empty response.")
    }

    suspend fun generateQuickSuggestion(tasks: List<PlannerTaskEntity>, todayIso: String): String {
        val summary = tasks.take(15).joinToString("; ") {
            "${it.dateIso} ${it.title} (${it.category}, rank=${it.missionRank}, done=${it.completed})"
        }.ifBlank { "No tasks scheduled yet." }
        val prompt =
            "Today is $todayIso. Give ONE warm, spirited 2-sentence anime shinobi sensei study briefing & tip based on these missions: $summary"
        val raw = withTimeout(35_000L) {
            executeWithQuotaResilience(LITE_MODEL, PRIMARY_MODEL) { model ->
                backend.generateContent(model, prompt, jsonSchema = null)
            }
        }
        return raw?.trim()?.ifBlank { null }
            ?: throw IllegalStateException("AI quick briefing returned empty output.")
    }

    suspend fun scanTimetableImages(
        bitmaps: List<Bitmap>,
        todayIso: String,
        accurateMode: Boolean
    ): List<ScannedTimetableEvent> {
        require(bitmaps.isNotEmpty()) { "No image or PDF pages provided." }
        val primary = if (accurateMode) PRIMARY_MODEL else LITE_MODEL
        val fallback = if (accurateMode) LITE_MODEL else PRIMARY_MODEL
        val promptText = """
            Today's date is $todayIso.
            Extract all university classes, labs, exams, deadlines, or study sessions visible in this timetable image/document.
            For recurring weekly classes, resolve dateIso to the next matching date starting from $todayIso and set recurrence to WEEKLY.
            Assign appropriate category (CLASS, STUDY, ASSIGNMENT, EXAM, DEADLINE) and missionRank (D-S).
        """.trimIndent()
        val images = bitmaps.take(4)

        val jsonText = withTimeout(60_000L) {
            executeWithQuotaResilience(primary, fallback) { model ->
                backend.generateMultimodalContent(model, promptText, images, timetableSchema)
            }
        } ?: throw IllegalStateException("Scanner returned empty output.")

        val root = JSONObject(jsonText)
        val array = root.optJSONArray("events") ?: return emptyList()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val sH = obj.optInt("startHour", 9).coerceIn(0, 23)
            val sM = obj.optInt("startMinute", 0).coerceIn(0, 59)
            val eH = obj.optInt("endHour", (sH + 1).coerceAtMost(23)).coerceIn(0, 23)
            val eM = obj.optInt("endMinute", sM).coerceIn(0, 59)
            val startMins = sH * 60 + sM
            val endMins = (eH * 60 + eM).coerceAtLeast(startMins + 30).coerceAtMost(1439)
            ScannedTimetableEvent(
                subject = obj.optString("subject", "General"),
                title = obj.optString("title", obj.optString("subject", "Class")),
                dayOfWeek = obj.optString("dayOfWeek", "MONDAY"),
                dateIso = obj.optString("dateIso", todayIso).ifBlank { todayIso },
                startTimeMinutes = startMins,
                endTimeMinutes = endMins,
                location = obj.optString("location", ""),
                teacher = obj.optString("teacher", ""),
                category = obj.optString("category", "CLASS"),
                missionRank = obj.optString("missionRank", "C"),
                recurrence = obj.optString("recurrence", "WEEKLY")
            )
        }
    }
}
