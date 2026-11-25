package com.example.ui.auth

import android.content.Context
import com.example.core.ai.AiPersonality
import com.example.ui.theme.ThemePreset

data class ShinobiLoginPersonalization(
    val callsign: String = "",
    val themePreset: ThemePreset = ThemePreset.NARUTO,
    val aiPersonality: AiPersonality = AiPersonality.MENTOR,
    val academicTrack: String = "Chunin • Year 2"
)

object ShinobiLoginPersonalizationStore {
    private const val PREFS_NAME = "shinobi_auth_prefs"
    private const val KEY_PENDING = "pending_login_personalization"
    private const val KEY_CALLSIGN = "login_pref_callsign"
    private const val KEY_THEME_PRESET = "login_pref_theme_preset"
    private const val KEY_AI_PERSONALITY = "login_pref_ai_personality"
    private const val KEY_ACADEMIC_TRACK = "login_pref_academic_track"

    fun savePending(context: Context, personalization: ShinobiLoginPersonalization) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, true)
            .putString(KEY_CALLSIGN, personalization.callsign.trim())
            .putString(KEY_THEME_PRESET, personalization.themePreset.name)
            .putString(KEY_AI_PERSONALITY, personalization.aiPersonality.code)
            .putString(KEY_ACADEMIC_TRACK, personalization.academicTrack)
            .apply()
    }

    fun loadLastOrDefault(context: Context): ShinobiLoginPersonalization {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetName = prefs.getString(KEY_THEME_PRESET, ThemePreset.NARUTO.name).orEmpty()
        val preset = runCatching { ThemePreset.valueOf(presetName) }.getOrDefault(ThemePreset.NARUTO)
        val personality = AiPersonality.fromCode(
            prefs.getString(KEY_AI_PERSONALITY, AiPersonality.MENTOR.code).orEmpty()
        )
        val track = prefs.getString(KEY_ACADEMIC_TRACK, "Chunin • Year 2").orEmpty()
            .ifBlank { "Chunin • Year 2" }
        val callsign = prefs.getString(KEY_CALLSIGN, "").orEmpty()
        return ShinobiLoginPersonalization(
            callsign = callsign,
            themePreset = preset,
            aiPersonality = personality,
            academicTrack = track
        )
    }

    fun consumePending(context: Context): ShinobiLoginPersonalization? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PENDING, false)) return null
        val loaded = loadLastOrDefault(context)
        prefs.edit().putBoolean(KEY_PENDING, false).apply()
        return loaded
    }
}
