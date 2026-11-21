package com.example.core.common

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.PlannerTaskEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfileEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream

object ExportHelper {

    fun buildJsonExport(
        profile: UserProfileEntity?,
        tasks: List<PlannerTaskEntity>,
        subjects: List<SubjectEntity>,
        sessions: List<StudySessionEntity>
    ): String {
        val root = JSONObject()
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        profile?.let { p ->
            root.put("profile", JSONObject().apply {
                put("displayName", p.displayName)
                put("username", p.username)
                put("university", p.university)
                put("department", p.department)
                put("academicYear", p.academicYear)
                put("xp", p.xp)
                put("level", p.level)
                put("ninjaTitle", p.ninjaTitle)
                put("currentStreak", p.currentStreak)
                put("longestStreak", p.longestStreak)
            })
        }
        val taskArr = JSONArray()
        tasks.forEach { t ->
            taskArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("subjectName", t.subjectName)
                put("category", t.category)
                put("dateIso", t.dateIso)
                put("startTimeMinutes", t.startTimeMinutes)
                put("endTimeMinutes", t.endTimeMinutes)
                put("missionRank", t.missionRank)
                put("priority", t.priority)
                put("completed", t.completed)
                put("xpReward", t.xpReward)
            })
        }
        root.put("tasks", taskArr)

        val subArr = JSONArray()
        subjects.forEach { s ->
            subArr.put(JSONObject().apply {
                put("name", s.name)
                put("code", s.code)
                put("instructor", s.instructor)
                put("room", s.room)
                put("targetHoursPerWeek", s.targetHoursPerWeek)
            })
        }
        root.put("subjects", subArr)

        val sessArr = JSONArray()
        sessions.forEach { s ->
            sessArr.put(JSONObject().apply {
                put("subjectName", s.subjectName)
                put("mode", s.mode)
                put("durationMinutes", s.durationMinutes)
                put("xpEarned", s.xpEarned)
            })
        }
        root.put("studySessions", sessArr)
        return root.toString(2)
    }

    fun buildCsvExport(tasks: List<PlannerTaskEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("Title,Subject,Category,Date,StartTime,EndTime,Rank,Priority,Completed,XP,Location")
        tasks.forEach { t ->
            val start = ShinobiTimeUtil.formatTime(t.startTimeMinutes, true)
            val end = ShinobiTimeUtil.formatTime(t.endTimeMinutes, true)
            sb.appendLine(
                listOf(
                    t.title, t.subjectName, t.category, t.dateIso,
                    start, end, t.missionRank, t.priority,
                    t.completed.toString(), t.xpReward.toString(), t.location
                ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            )
        }
        return sb.toString()
    }

    fun writePdfReport(
        outputStream: OutputStream,
        profile: UserProfileEntity?,
        tasks: List<PlannerTaskEntity>,
        sessions: List<StudySessionEntity>
    ) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0B1124")
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
        }

        var y = 48f
        canvas.drawText("SHINOBI PLANNER — TRAINING REPORT", 40f, y, titlePaint)
        y += 24f
        val scholar = profile?.displayName ?: "Shinobi Scholar"
        val uni = profile?.university?.ifBlank { "University Academy" } ?: "University Academy"
        canvas.drawText("Scholar: $scholar  |  $uni", 40f, y, subtitlePaint)
        y += 18f
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val completedCount = tasks.count { it.completed }
        canvas.drawText(
            "Level ${profile?.level ?: 1} (${profile?.ninjaTitle ?: "Academy Student"})  |  XP: ${profile?.xp ?: 0}  |  Streak: ${profile?.currentStreak ?: 0}d",
            40f, y, subtitlePaint
        )
        y += 18f
        canvas.drawText(
            "Completed Missions: $completedCount/${tasks.size}  |  Focus Time: ${totalMinutes / 60}h ${totalMinutes % 60}m",
            40f, y, subtitlePaint
        )
        y += 28f
        canvas.drawText("MISSIONS & SCHEDULE OVERVIEW", 40f, y, titlePaint.apply { textSize = 14f })
        y += 18f

        tasks.take(30).forEach { task ->
            if (y > 800f) return@forEach
            val status = if (task.completed) "[DONE]" else "[OPEN]"
            val time = "${ShinobiTimeUtil.formatTime(task.startTimeMinutes, true)}-${ShinobiTimeUtil.formatTime(task.endTimeMinutes, true)}"
            val line = "$status [${task.missionRank}] ${task.dateIso} $time  ${task.title.take(38)} (${task.subjectName})"
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 16f
        }

        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open destination file.")
    }
}
