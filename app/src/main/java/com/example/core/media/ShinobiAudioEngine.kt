package com.example.core.media

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.MusicTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String = "No Songs Added",
    val currentArtist: String = "Import Your Music",
    val currentTrackIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val trackCount: Int = 0
)

class ShinobiAudioEngine(private val context: Context) {
    val soundEffectManager = ShinobiSoundEffectManager(context)

    private var player: ExoPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var musicVolume = 0.65f
    private var motivationalVolume = 0.85f
    private var quietHoursMuted = false

    private var loadedTracks: List<MusicTrackEntity> = emptyList()

    init {
        initTts()
    }

    private fun ensurePlayer(): ExoPlayer? {
        val existing = player
        if (existing != null) return existing
        return runCatching {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            val created = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true) // Automatic audio focus + call pause
                .setHandleAudioBecomingNoisy(true)
                .build()
            created.repeatMode = Player.REPEAT_MODE_ALL
            created.volume = if (quietHoursMuted) 0f else musicVolume
            created.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackSnapshot()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updatePlaybackSnapshot()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    updatePlaybackSnapshot()
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    updatePlaybackSnapshot()
                }
            })
            player = created
            created
        }.getOrNull()
    }

    private fun updatePlaybackSnapshot() {
        val p = player ?: return
        val idx = p.currentMediaItemIndex
        val track = loadedTracks.getOrNull(idx)
        _playbackState.value = AudioPlaybackState(
            isPlaying = p.isPlaying,
            currentTitle = track?.title ?: p.mediaMetadata.title?.toString() ?: "No Songs Added",
            currentArtist = track?.artist ?: "Your Study Playlist",
            currentTrackIndex = idx.coerceAtLeast(0),
            shuffleEnabled = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            trackCount = loadedTracks.size
        )
    }


    private fun initTts() {
        runCatching {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching {
                        tts?.language = Locale.US
                        ttsReady = true
                    }
                }
            }
        }
    }

    suspend fun purgeLegacyBuiltInAudioFiles() = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "shinobi_audio")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    fun setPlaylist(tracks: List<MusicTrackEntity>, autoPlay: Boolean = false) {
        loadedTracks = tracks
        if (tracks.isEmpty()) {
            runCatching {
                player?.stop()
                player?.clearMediaItems()
            }
            _playbackState.value = AudioPlaybackState()
            return
        }
        val exo = ensurePlayer() ?: return
        runCatching {
            val currentUri = exo.currentMediaItem?.localConfiguration?.uri?.toString()
            val sameList = exo.mediaItemCount == tracks.size && currentUri == tracks.firstOrNull()?.uriString
            if (!sameList) {
                val items = tracks.map { t ->
                    MediaItem.Builder()
                        .setUri(Uri.parse(t.uriString))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(t.title)
                                .setArtist(t.artist)
                                .build()
                        )
                        .build()
                }
                exo.setMediaItems(items)
                exo.prepare()
            }
            if (autoPlay && !quietHoursMuted) {
                exo.play()
            }
            updatePlaybackSnapshot()
        }
    }

    fun playTrackAt(index: Int) {
        if (loadedTracks.isEmpty()) return
        val exo = ensurePlayer() ?: return
        runCatching {
            val safeIdx = index.coerceIn(0, loadedTracks.lastIndex)
            exo.seekTo(safeIdx, 0L)
            if (!quietHoursMuted) exo.play()
            updatePlaybackSnapshot()
        }
    }

    fun togglePlayPause() {
        if (loadedTracks.isEmpty()) return
        val exo = ensurePlayer() ?: return
        runCatching {
            if (exo.isPlaying) {
                exo.pause()
            } else if (!quietHoursMuted) {
                if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
                exo.play()
            }
            updatePlaybackSnapshot()
        }
    }

    fun playIfReady() {
        if (loadedTracks.isEmpty() || quietHoursMuted) return
        val exo = ensurePlayer() ?: return
        runCatching {
            if (!exo.isPlaying) {
                if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
                exo.play()
                updatePlaybackSnapshot()
            }
        }
    }

    fun pause() {
        runCatching {
            player?.pause()
            updatePlaybackSnapshot()
        }
    }

    fun next() {
        val exo = player ?: return
        runCatching {
            if (exo.hasNextMediaItem()) exo.seekToNextMediaItem() else exo.seekTo(0, 0L)
            updatePlaybackSnapshot()
        }
    }

    fun previous() {
        val exo = player ?: return
        runCatching {
            if (exo.hasPreviousMediaItem()) exo.seekToPreviousMediaItem() else exo.seekTo(0, 0L)
            updatePlaybackSnapshot()
        }
    }

    fun toggleShuffle() {
        val exo = ensurePlayer() ?: return
        runCatching {
            exo.shuffleModeEnabled = !exo.shuffleModeEnabled
            updatePlaybackSnapshot()
        }
    }

    fun cycleRepeatMode() {
        val exo = ensurePlayer() ?: return
        runCatching {
            exo.repeatMode = when (exo.repeatMode) {
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_ALL
            }
            updatePlaybackSnapshot()
        }
    }

    fun updateVolumes(
        music: Float,
        sfx: Float,
        motivational: Float,
        haptics: Boolean,
        quietMuted: Boolean
    ) {
        musicVolume = music.coerceIn(0f, 1f)
        motivationalVolume = motivational.coerceIn(0f, 1f)
        quietHoursMuted = quietMuted
        soundEffectManager.updateConfig(
            volume = sfx,
            soundEnabled = sfx > 0.01f,
            hapticsEnabled = haptics,
            quietMuted = quietMuted
        )
        runCatching {
            player?.volume = if (quietMuted) 0f else musicVolume
            if (quietMuted && player?.isPlaying == true) {
                player?.pause()
            }
        }
    }

    fun playSfx(sfx: ShinobiSfx) {
        soundEffectManager.play(sfx)
    }

    fun speakMotivational(text: String) {
        if (!ttsReady || quietHoursMuted || motivationalVolume <= 0.05f) return
        runCatching {
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, motivationalVolume)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "shinobi_motivation_${System.currentTimeMillis()}")
        }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
        soundEffectManager.release()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }
}
