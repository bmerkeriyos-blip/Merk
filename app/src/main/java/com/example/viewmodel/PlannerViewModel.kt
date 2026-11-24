package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.*
import com.example.core.media.AudioPlaybackState
import com.example.core.media.ShinobiAudioEngine
import com.example.core.media.ShinobiSfx
import com.example.core.workers.CloudSyncWorker
import com.example.data.local.*
import com.example.data.repository.ShinobiRepository
import com.example.data.repository.TaskConflict
import com.example.data.repository.TimeSlot
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class PlannerUiState(
    val profile: UserProfileEntity? = null,
    val settings: UserSettingsEntity = UserSettingsEntity(userId = ""),
    val tasks: List<PlannerTaskEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val history: List<TaskCompletionHistoryEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val wallpapers: List<WallpaperItemEntity> = emptyList(),
    val tracks: List<MusicTrackEntity> = emptyList(),
    val notes: List<StudyNoteEntity> = emptyList(),
    val celebrationMessage: String? = null,
    val statusMessage: String? = null,
    val isSyncing: Boolean = false
)

class PlannerViewModel(
    private val repository: ShinobiRepository,
    private val audioEngine: ShinobiAudioEngine,
    private val currentUserId: String
) : ViewModel() {

    private val _celebration = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)

    val audioState: StateFlow<AudioPlaybackState> = audioEngine.playbackState

    private val primaryDataFlow = combine(
        repository.observeProfile(currentUserId),
        repository.observeSettings(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeAllSubtasks(currentUserId),
        repository.observeSubjects(currentUserId)
    ) { profile, settings, tasks, subtasks, subjects ->
        PrimaryData(profile, settings ?: UserSettingsEntity(currentUserId), tasks, subtasks, subjects)
    }

    private val secondaryDataFlow = combine(
        repository.observeSessions(currentUserId),
        repository.observeHistory(currentUserId),
        repository.observeAchievements(currentUserId),
        repository.observeWallpapers(currentUserId),
        repository.observeTracks(currentUserId)
    ) { sessions, history, achievements, wallpapers, tracks ->
        SecondaryData(sessions, history, achievements, wallpapers, tracks)
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        primaryDataFlow,
        secondaryDataFlow,
        repository.observeNotes(currentUserId),
        _celebration,
        _statusMessage,
        _isSyncing
    ) { array ->
        val primary = array[0] as PrimaryData
        val secondary = array[1] as SecondaryData
        @Suppress("UNCHECKED_CAST")
        val notes = array[2] as List<StudyNoteEntity>
        val celebration = array[3] as String?
        val status = array[4] as String?
        val syncing = array[5] as Boolean
        PlannerUiState(
            profile = primary.profile,
            settings = primary.settings,
            tasks = primary.tasks,
            subtasks = primary.subtasks,
            subjects = primary.subjects,
            sessions = secondary.sessions,
            history = secondary.history,
            achievements = secondary.achievements,
            wallpapers = secondary.wallpapers,
            tracks = secondary.tracks,
            notes = notes,
            celebrationMessage = celebration,
            statusMessage = status,
            isSyncing = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PlannerUiState()
    )

    fun initializeSession(firebaseUser: FirebaseUser?) {
        viewModelScope.launch {
            repository.ensureUserInitialized(firebaseUser)
            val existingTracks = repository.observeTracks(currentUserId).first()
            if (existingTracks.isEmpty()) {
                val builtIn = audioEngine.ensureBuiltInTracks(currentUserId)
                repository.upsertTracks(builtIn)
                audioEngine.setPlaylist(builtIn, autoPlay = false)
            } else {
                audioEngine.setPlaylist(existingTracks, autoPlay = false)
            }
            val currentSettings = repository.observeSettings(currentUserId).first()
                ?: UserSettingsEntity(userId = currentUserId)
            applyAudioVolumes(currentSettings)
            if (currentSettings.musicOnOpen) {
                audioEngine.playIfReady()
            }
        }
    }

    private fun applyAudioVolumes(settings: UserSettingsEntity) {
        val now = ShinobiTimeUtil.nowZoned(settings.timezoneId)
        val inQuiet = if (settings.quietHoursEnabled) {
            val h = now.hour
            val s = settings.quietStartHour
            val e = settings.quietEndHour
            if (s > e) (h >= s || h < e) else (h in s until e)
        } else false
        audioEngine.updateVolumes(
            music = settings.musicVolume,
            sfx = if (settings.soundEnabled) settings.sfxVolume else 0f,
            motivational = settings.motivationalVolume,
            haptics = settings.hapticsEnabled && settings.vibrationEnabled,
            quietMuted = inQuiet
        )
    }

    fun playSfx(sfx: ShinobiSfx) = audioEngine.playSfx(sfx)

    fun saveTask(task: PlannerTaskEntity, subtasks: List<SubtaskEntity> = emptyList()) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SCROLL_OPEN)
            repository.saveTask(task.copy(userId = currentUserId), subtasks)
            _statusMessage.value = "Mission saved: ${task.title}"
        }
    }

    fun toggleTaskCompleted(task: PlannerTaskEntity) {
        viewModelScope.launch {
            val (xpDelta, rankUpTitle) = repository.toggleTaskCompletion(task)
            if (!task.completed) {
                audioEngine.soundEffectManager.play(ShinobiSfx.SHURIKEN)
                if (rankUpTitle != null) {
                    _celebration.value = "RANK UP! $rankUpTitle (+$xpDelta XP)"
                    if (uiState.value.settings.musicOnComplete) {
                        audioEngine.speakMotivational("Rank up! Welcome to $rankUpTitle.")
                    }
                } else {
                    _celebration.value = "Mission Complete! +$xpDelta XP"
                }
            } else {
                audioEngine.soundEffectManager.play(ShinobiSfx.BUTTON_TAP)
            }
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SHURIKEN)
            repository.toggleSubtask(subtask)
        }
    }

    fun deleteTask(task: PlannerTaskEntity) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SHURIKEN)
            repository.deleteTask(task)
            _statusMessage.value = "Mission removed"
        }
    }

    fun duplicateTask(task: PlannerTaskEntity) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SCROLL_OPEN)
            repository.duplicateTask(task)
            _statusMessage.value = "Mission duplicated"
        }
    }

    fun moveTaskUp(task: PlannerTaskEntity, dayTasks: List<PlannerTaskEntity>) {
        val idx = dayTasks.indexOfFirst { it.id == task.id }
        if (idx <= 0) return
        val mutable = dayTasks.toMutableList()
        val prev = mutable[idx - 1]
        mutable[idx - 1] = task
        mutable[idx] = prev
        viewModelScope.launch { repository.reorderTasks(mutable) }
    }

    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SCROLL_OPEN)
            repository.addOrUpdateSubject(subject.copy(userId = currentUserId))
            _statusMessage.value = "Subject saved: ${subject.name}"
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch { repository.deleteSubject(subjectId) }
    }

    fun saveNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            audioEngine.playSfx(ShinobiSfx.SCROLL_OPEN)
            repository.saveNote(note.copy(userId = currentUserId))
            _statusMessage.value = "Note saved"
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    fun updateProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _statusMessage.value = "Profile updated"
        }
    }

    fun completeOnboarding(
        displayName: String,
        university: String,
        department: String,
        academicYear: String,
        timezoneId: String,
        use24HourClock: Boolean,
        showEthiopianTime: Boolean,
        themePreset: String,
        aiPersonality: String
    ) {
        viewModelScope.launch {
            val current = uiState.value.profile ?: return@launch
            repository.saveProfile(
                current.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    university = university,
                    department = department,
                    academicYear = academicYear,
                    onboardingCompleted = true
                )
            )
            val s = uiState.value.settings.copy(
                userId = currentUserId,
                timezoneId = timezoneId,
                use24HourClock = use24HourClock,
                showEthiopianTime = showEthiopianTime,
                themePreset = themePreset,
                aiPersonality = aiPersonality
            )
            repository.saveSettings(s)
            applyAudioVolumes(s)
            audioEngine.playSfx(ShinobiSfx.JUTSU)
        }
    }

    fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        viewModelScope.launch {
            val next = transform(uiState.value.settings.copy(userId = currentUserId))
            repository.saveSettings(next)
            applyAudioVolumes(next)
        }
    }

    fun addImportedWallpaper(uriString: String, screenTarget: String = "GLOBAL") {
        viewModelScope.launch {
            repository.upsertWallpaper(
                WallpaperItemEntity(
                    userId = currentUserId,
                    screenTarget = screenTarget,
                    uriString = uriString,
                    brightness = 0.72f,
                    blurRadius = 2f,
                    overlayOpacity = 0.6f,
                    parallaxEnabled = true
                )
            )
            _statusMessage.value = "Wallpaper applied to $screenTarget"
        }
    }

    fun updateWallpaperTuning(
        wallpaper: WallpaperItemEntity,
        brightness: Float,
        blurRadius: Float,
        overlayOpacity: Float,
        parallaxEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.upsertWallpaper(
                wallpaper.copy(
                    brightness = brightness,
                    blurRadius = blurRadius,
                    overlayOpacity = overlayOpacity,
                    parallaxEnabled = parallaxEnabled
                )
            )
        }
    }

    fun deleteWallpaper(id: String) {
        viewModelScope.launch { repository.deleteWallpaper(id) }
    }

    fun resetWallpapers() {
        viewModelScope.launch {
            uiState.value.wallpapers.filter { !it.builtInResName.equals("bg_ninja_village", true) }
                .forEach { repository.deleteWallpaper(it.id) }
            _statusMessage.value = "Restored default Ninja Village wallpaper"
        }
    }

    fun addImportedTrack(title: String, uriString: String, motivational: Boolean = false) {
        viewModelScope.launch {
            val track = MusicTrackEntity(
                userId = currentUserId,
                title = title.ifBlank { "Imported Shinobi Track" },
                artist = "User Media",
                uriString = uriString,
                motivational = motivational,
                builtInSynth = false,
                sortOrder = uiState.value.tracks.size
            )
            repository.upsertTrack(track)
            val updated = uiState.value.tracks + track
            audioEngine.setPlaylist(updated, autoPlay = true)
            _statusMessage.value = "Added track: ${track.title}"
        }
    }

    fun deleteTrack(trackId: String) {
        viewModelScope.launch {
            repository.deleteTrack(trackId)
            val remaining = uiState.value.tracks.filterNot { it.id == trackId }
            audioEngine.setPlaylist(remaining, autoPlay = false)
        }
    }

    fun togglePlayPauseMusic() = audioEngine.togglePlayPause()
    fun playTrackAt(index: Int) = audioEngine.playTrackAt(index)
    fun nextTrack() = audioEngine.next()
    fun previousTrack() = audioEngine.previous()
    fun toggleShuffle() = audioEngine.toggleShuffle()
    fun cycleRepeatMode() = audioEngine.cycleRepeatMode()
    fun speakMotivation(text: String) = audioEngine.speakMotivational(text)

    fun triggerSyncNow(context: Context) {
        viewModelScope.launch {
            if (currentUserId == ShinobiRepository.LOCAL_GUEST_UID) {
                _statusMessage.value = "Local mode active — sign in with Google to enable cloud sync"
                return@launch
            }
            _isSyncing.value = true
            val res = repository.syncPendingToCloud(currentUserId)
            CloudSyncWorker.triggerImmediateSync(context)
            _isSyncing.value = false
            _statusMessage.value = res.fold(
                onSuccess = { "Cloud sync complete ($it missions synced)" },
                onFailure = { "Offline cache preserved (${it.localizedMessage ?: "sync queued"})" }
            )
        }
    }

    fun exportJson(): String = ExportHelper.buildJsonExport(
        uiState.value.profile,
        uiState.value.tasks,
        uiState.value.subjects,
        uiState.value.sessions
    )

    fun exportCsv(): String = ExportHelper.buildCsvExport(uiState.value.tasks)

    fun writePdfReport(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                ExportHelper.writePdfReport(
                    stream,
                    uiState.value.profile,
                    uiState.value.tasks,
                    uiState.value.sessions
                )
            }
            _statusMessage.value = "PDF Training Report exported"
            true
        }.getOrElse {
            _statusMessage.value = "Export failed: ${it.localizedMessage}"
            false
        }
    }

    fun deleteAccountData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData(currentUserId)
            onDone()
        }
    }

    fun detectConflicts(dateIso: String): List<TaskConflict> =
        repository.detectConflicts(uiState.value.tasks.filter { it.dateIso == dateIso })

    fun findFreeTimeSlots(dateIso: String): List<TimeSlot> =
        repository.findFreeTimeSlots(uiState.value.tasks.filter { it.dateIso == dateIso })

    fun clearCelebration() {
        _celebration.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun reportFocusCompletion(xp: Int, rankUpTitle: String?) {
        if (rankUpTitle != null) {
            audioEngine.playSfx(ShinobiSfx.RANK_UP)
            _celebration.value = "RANK UP! $rankUpTitle (+$xp XP)"
        } else {
            audioEngine.playSfx(ShinobiSfx.XP_GAIN)
            _celebration.value = "Focus Session Complete! +$xp XP"
        }
    }

    fun todayIso(): String =
        ShinobiTimeUtil.nowZoned(uiState.value.settings.timezoneId)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

    private data class PrimaryData(
        val profile: UserProfileEntity?,
        val settings: UserSettingsEntity,
        val tasks: List<PlannerTaskEntity>,
        val subtasks: List<SubtaskEntity>,
        val subjects: List<SubjectEntity>
    )

    private data class SecondaryData(
        val sessions: List<StudySessionEntity>,
        val history: List<TaskCompletionHistoryEntity>,
        val achievements: List<AchievementEntity>,
        val wallpapers: List<WallpaperItemEntity>,
        val tracks: List<MusicTrackEntity>
    )
}
