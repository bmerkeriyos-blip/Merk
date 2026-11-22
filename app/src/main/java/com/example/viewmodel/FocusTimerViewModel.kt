package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.media.ShinobiAudioEngine
import com.example.core.media.ShinobiSfx
import com.example.data.repository.ShinobiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FocusModeType(val label: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15),
    CUSTOM("Custom Focus", 45),
    EXAM_PREP("Exam Prep", 50),
    STOPWATCH("Stopwatch", 0)
}

data class FocusTimerUiState(
    val mode: FocusModeType = FocusModeType.POMODORO,
    val selectedSubject: String = "General Study",
    val customMinutes: Int = 45,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val elapsedStopwatchSeconds: Int = 0,
    val isRunning: Boolean = false,
    val distractionFreeMode: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val completedPomodorosToday: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val weeklyGoalMinutes: Int = 600,
    val lastCompletedMessage: String? = null
)

class FocusTimerViewModel(
    private val repository: ShinobiRepository,
    private val audioEngine: ShinobiAudioEngine,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusTimerUiState())
    val uiState: StateFlow<FocusTimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun selectMode(mode: FocusModeType) {
        stopTimer()
        val mins = if (mode == FocusModeType.CUSTOM) _uiState.value.customMinutes else mode.defaultMinutes
        _uiState.update {
            it.copy(
                mode = mode,
                totalSeconds = mins * 60,
                remainingSeconds = mins * 60,
                elapsedStopwatchSeconds = 0,
                isRunning = false
            )
        }
    }

    fun setCustomMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 180)
        _uiState.update {
            if (it.mode == FocusModeType.CUSTOM && !it.isRunning) {
                it.copy(customMinutes = clamped, totalSeconds = clamped * 60, remainingSeconds = clamped * 60)
            } else {
                it.copy(customMinutes = clamped)
            }
        }
    }

    fun selectSubject(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun toggleDistractionFree() {
        _uiState.update { it.copy(distractionFreeMode = !it.distractionFreeMode) }
    }

    fun toggleKeepScreenAwake() {
        _uiState.update { it.copy(keepScreenAwake = !it.keepScreenAwake) }
    }

    fun setDailyGoal(minutes: Int) {
        _uiState.update { it.copy(dailyGoalMinutes = minutes.coerceIn(15, 600)) }
    }

    fun toggleStartPause(playFocusMusic: Boolean) {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer(playFocusMusic)
        }
    }

    private fun startTimer(playFocusMusic: Boolean) {
        if (timerJob?.isActive == true) return
        audioEngine.soundEffectManager.play(ShinobiSfx.JUTSU)
        if (playFocusMusic) audioEngine.playIfReady()
        _uiState.update { it.copy(isRunning = true, lastCompletedMessage = null) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000L)
                val current = _uiState.value
                if (current.mode == FocusModeType.STOPWATCH) {
                    _uiState.update { it.copy(elapsedStopwatchSeconds = it.elapsedStopwatchSeconds + 1) }
                } else {
                    if (current.remainingSeconds > 1) {
                        _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                    } else {
                        completeActiveSession()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun stopAndSaveEarly() {
        val current = _uiState.value
        val elapsedMins = if (current.mode == FocusModeType.STOPWATCH) {
            (current.elapsedStopwatchSeconds / 60).coerceAtLeast(1)
        } else {
            ((current.totalSeconds - current.remainingSeconds) / 60).coerceAtLeast(0)
        }
        pauseTimer()
        if (elapsedMins >= 1) {
            viewModelScope.launch {
                val (xp, rankUp) = repository.recordStudySession(
                    userId = currentUserId,
                    subjectName = current.selectedSubject,
                    mode = current.mode.name,
                    durationMinutes = elapsedMins
                )
                audioEngine.playSfx(if (rankUp != null) ShinobiSfx.RANK_UP else ShinobiSfx.XP_GAIN)
                _uiState.update {
                    it.copy(
                        remainingSeconds = it.totalSeconds,
                        elapsedStopwatchSeconds = 0,
                        lastCompletedMessage = rankUp?.let { r -> "RANK UP! $r (+$xp XP)" }
                            ?: "Logged $elapsedMins min study (+$xp XP)"
                    )
                }
            }
        } else {
            resetTimer()
        }
    }

    fun resetTimer() {
        stopTimer()
        val current = _uiState.value
        _uiState.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                elapsedStopwatchSeconds = 0,
                isRunning = false
            )
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun completeActiveSession() {
        val current = _uiState.value
        val mins = (current.totalSeconds / 60).coerceAtLeast(1)
        stopTimer()
        val isBreak = current.mode == FocusModeType.SHORT_BREAK || current.mode == FocusModeType.LONG_BREAK
        if (!isBreak) {
            val (xp, rankUp) = repository.recordStudySession(
                userId = currentUserId,
                subjectName = current.selectedSubject,
                mode = current.mode.name,
                durationMinutes = mins
            )
            audioEngine.playSfx(if (rankUp != null) ShinobiSfx.RANK_UP else ShinobiSfx.MISSION_COMPLETE)
            _uiState.update {
                it.copy(
                    isRunning = false,
                    remainingSeconds = it.totalSeconds,
                    completedPomodorosToday = it.completedPomodorosToday + 1,
                    lastCompletedMessage = rankUp?.let { r -> "RANK UP! $r (+$xp XP)" }
                        ?: "${current.mode.label} Complete! +$xp XP"
                )
            }
        } else {
            audioEngine.playSfx(ShinobiSfx.CHAKRA_PULSE)
            _uiState.update {
                it.copy(
                    isRunning = false,
                    remainingSeconds = it.totalSeconds,
                    lastCompletedMessage = "Recovery complete. Ready for next mission!"
                )
            }
        }
    }

    fun clearLastMessage() {
        _uiState.update { it.copy(lastCompletedMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
