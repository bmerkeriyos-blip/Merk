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

enum class AiPersonality(val code: String, val label: String, val systemTone: String) {
    MENTOR(
        "MENTOR",
        "Shinobi Sensei",
        "You are a warm, spirited Shinobi Sensei mentoring a university student. Speak like a caring anime master with practical study advice, wisdom, and genuine belief in your student's growth."
    ),
    STRICT(
        "STRICT",
        "Jonin Captain",
        "You are a disciplined Jonin squad captain. Keep guidance crisp, honorable, accountable, and focused on mastering deadlines with steady training."
    ),
    FRIENDLY(
        "FRIENDLY",
        "Squad Teammate",
        "You are an enthusiastic, loyal Hidden Leaf squadmate studying alongside the user. Cheer them on with warmth, determination, and upbeat anime energy."
    ),
    PROFESSIONAL(
        "PROFESSIONAL",
        "Kage Advisor",
        "You are a composed Hokage-tower academic strategist. Provide structured, thoughtful study plans and calm, clear summaries."
    ),
    COMPETITIVE(
        "COMPETITIVE",
        "Eternal Rival",
        "You are a spirited friendly rival pushing the student to surpass their limits, protect their training streak, and achieve S-Rank mastery!"
    );

    companion object {
        fun fromCode(raw: String): AiPersonality =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: MENTOR
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
    override suspend fun generateContent(
        modelName: String,
        promptText: String,
        jsonSchema: Schema?
    ): String? {
        val model = if (jsonSchema != null) {
            Firebase.ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = jsonSchema
                }
            )
        } else {
            Firebase.ai.generativeModel(modelName = modelName)
        }
        return model.generateContent(promptText).text
    }

    override suspend fun generateMultimodalContent(
        modelName: String,
        promptText: String,
        images: List<Bitmap>,
        jsonSchema: Schema
    ): String? {
        val model = Firebase.ai.generativeModel(
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
            Firebase.ai.generativeModel(
                modelName = modelName,
                systemInstruction = content { text(systemInstruction) }
            )
        } else {
            Firebase.ai.generativeModel(modelName = modelName)
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
        scheduleContext: String
    ): Flow<String> = flow {
        val systemAndContext = """
            ${personality.systemTone}
            Current student academic context:
            $scheduleContext
            Provide clear, actionable, motivating guidance. Never invent fake grades or pretend an action happened if it didn't.
        """.trimIndent()

        val chatHistory = sanitizeChatHistory(history, userMessage)

        val modelsToTry = listOf(PRIMARY_MODEL, LITE_MODEL)
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
                val canFallback = index < modelsToTry.lastIndex && !emittedAny && isQuotaOrRateLimitError(e)
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
