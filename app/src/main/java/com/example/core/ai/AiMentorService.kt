package com.example.core.ai

import android.graphics.Bitmap
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PlannerTaskEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiPersonality(val code: String, val label: String, val systemTone: String) {
    MENTOR(
        "MENTOR",
        "Ninja Mentor",
        "You are Shinobi Mentor, a disciplined, encouraging cyber-ninja academic tactician. Speak concisely with subtle shinobi wisdom and practical university study advice."
    ),
    STRICT(
        "STRICT",
        "Strict Sensei",
        "You are a strict high-standards tactical sensei. Keep responses crisp, direct, accountable, and laser-focused on execution and deadlines."
    ),
    FRIENDLY(
        "FRIENDLY",
        "Friendly Ally",
        "You are a warm, supportive fellow cyber-ninja scholar. Be upbeat, encouraging, and clear."
    ),
    PROFESSIONAL(
        "PROFESSIONAL",
        "Academic Advisor",
        "You are a polished university academic strategist. Provide structured, evidence-based study plans and clear summaries."
    ),
    COMPETITIVE(
        "COMPETITIVE",
        "Rival Tactician",
        "You are an intense, high-energy rival pushing the student to beat their streak, earn S-Rank mastery, and maximize XP."
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

class AiMentorService {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
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

    private val commandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = commandSchema
            }
        )
    }

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

    private val accurateScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = timetableSchema
            }
        )
    }

    private val quickScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = timetableSchema
            }
        )
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
            val response = withTimeout(30_000L) {
                commandModel.generateContent(prompt)
            }
            val raw = response.text ?: return ParsedAiCommand.ChatOnly
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
        } catch (_: Exception) {
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

        val chatHistory = buildList {
            add(content("user") { text(systemAndContext) })
            add(content("model") { text("Understood. I am ready to guide your training and academic missions.") })
            history.takeLast(12).forEach { msg ->
                add(content(msg.role) { text(msg.content) })
            }
        }

        val chat = chatModel.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Mentor returned an empty response.")
        }
    }

    suspend fun generateQuickSuggestion(tasks: List<PlannerTaskEntity>, todayIso: String): String {
        val summary = tasks.take(15).joinToString("; ") {
            "${it.dateIso} ${it.title} (${it.category}, rank=${it.missionRank}, done=${it.completed})"
        }.ifBlank { "No tasks scheduled yet." }
        val response = withTimeout(35_000L) {
            chatModel.generateContent(
                "Today is $todayIso. Give ONE crisp 2-sentence cyber-ninja study briefing & tip based on these missions: $summary"
            )
        }
        return response.text?.trim()?.ifBlank { null }
            ?: "Review your highest-rank mission first and lock in a 25-minute Focus Dojo block."
    }

    suspend fun scanTimetableImages(
        bitmaps: List<Bitmap>,
        todayIso: String,
        accurateMode: Boolean
    ): List<ScannedTimetableEvent> {
        require(bitmaps.isNotEmpty()) { "No image or PDF pages provided." }
        val model = if (accurateMode) accurateScanModel else quickScanModel
        val promptContent = content {
            text(
                """
                Today's date is $todayIso.
                Extract all university classes, labs, exams, deadlines, or study sessions visible in this timetable image/document.
                For recurring weekly classes, resolve dateIso to the next matching date starting from $todayIso and set recurrence to WEEKLY.
                Assign appropriate category (CLASS, STUDY, ASSIGNMENT, EXAM, DEADLINE) and missionRank (D-S).
                """.trimIndent()
            )
            bitmaps.take(4).forEach { bmp ->
                image(bmp)
            }
        }
        val response = withTimeout(60_000L) {
            model.generateContent(promptContent)
        }
        val jsonText = response.text ?: throw IllegalStateException("Scanner returned empty output.")
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
