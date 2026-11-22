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
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String = "No Track Loaded",
    val currentArtist: String = "Shinobi Synth",
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

    private fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
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
        return created
    }

    private fun updatePlaybackSnapshot() {
        val p = player ?: return
        val idx = p.currentMediaItemIndex
        val track = loadedTracks.getOrNull(idx)
        _playbackState.value = AudioPlaybackState(
            isPlaying = p.isPlaying,
            currentTitle = track?.title ?: p.mediaMetadata.title?.toString() ?: "Shinobi Synth",
            currentArtist = track?.artist ?: "Original Focus Audio",
            currentTrackIndex = idx.coerceAtLeast(0),
            shuffleEnabled = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            trackCount = loadedTracks.size
        )
    }


    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    suspend fun ensureBuiltInTracks(userId: String): List<MusicTrackEntity> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "shinobi_audio").apply { mkdirs() }
        val specs = listOf(
            Triple("synth_dojo_$userId", "Hidden Village Dawn (Original)", 220.0),
            Triple("synth_rain_$userId", "Midnight Hidden Village (Original)", 196.0),
            Triple("synth_pulse_$userId", "Chakra Flow Focus (Original)", 246.94)
        )
        specs.mapIndexed { idx, (id, title, baseFreq) ->
            val file = File(dir, "$id.wav")
            if (!file.exists()) {
                writeAmbientSynthWav(file, baseFreq, durationSeconds = 12)
            }
            MusicTrackEntity(
                id = id,
                userId = userId,
                title = title,
                artist = "Shinobi Original Synth",
                uriString = Uri.fromFile(file).toString(),
                durationMs = 12_000L,
                motivational = false,
                builtInSynth = true,
                sortOrder = idx
            )
        }
    }

    fun setPlaylist(tracks: List<MusicTrackEntity>, autoPlay: Boolean = false) {
        loadedTracks = tracks
        if (tracks.isEmpty()) {
            _playbackState.value = AudioPlaybackState()
            return
        }
        val exo = ensurePlayer()
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

    fun playTrackAt(index: Int) {
        if (loadedTracks.isEmpty()) return
        val exo = ensurePlayer()
        val safeIdx = index.coerceIn(0, loadedTracks.lastIndex)
        exo.seekTo(safeIdx, 0L)
        if (!quietHoursMuted) exo.play()
        updatePlaybackSnapshot()
    }

    fun togglePlayPause() {
        if (loadedTracks.isEmpty()) return
        val exo = ensurePlayer()
        if (exo.isPlaying) {
            exo.pause()
        } else if (!quietHoursMuted) {
            if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
            exo.play()
        }
        updatePlaybackSnapshot()
    }

    fun playIfReady() {
        if (loadedTracks.isEmpty() || quietHoursMuted) return
        val exo = ensurePlayer()
        if (!exo.isPlaying) {
            if (exo.playbackState == Player.STATE_IDLE) exo.prepare()
            exo.play()
            updatePlaybackSnapshot()
        }
    }

    fun pause() {
        player?.pause()
        updatePlaybackSnapshot()
    }

    fun next() {
        val exo = player ?: return
        if (exo.hasNextMediaItem()) exo.seekToNextMediaItem() else exo.seekTo(0, 0L)
        updatePlaybackSnapshot()
    }

    fun previous() {
        val exo = player ?: return
        if (exo.hasPreviousMediaItem()) exo.seekToPreviousMediaItem() else exo.seekTo(0, 0L)
        updatePlaybackSnapshot()
    }

    fun toggleShuffle() {
        val exo = ensurePlayer()
        exo.shuffleModeEnabled = !exo.shuffleModeEnabled
        updatePlaybackSnapshot()
    }

    fun cycleRepeatMode() {
        val exo = ensurePlayer()
        exo.repeatMode = when (exo.repeatMode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
        updatePlaybackSnapshot()
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
        player?.volume = if (quietMuted) 0f else musicVolume
        if (quietMuted && player?.isPlaying == true) {
            player?.pause()
        }
    }

    fun playSfx(sfx: ShinobiSfx) {
        soundEffectManager.play(sfx)
    }

    fun speakMotivational(text: String) {
        if (!ttsReady || quietHoursMuted || motivationalVolume <= 0.05f) return
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, motivationalVolume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "shinobi_motivation_${System.currentTimeMillis()}")
    }

    fun release() {
        player?.release()
        player = null
        soundEffectManager.release()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun writeAmbientSynthWav(outFile: File, baseFreq: Double, durationSeconds: Int) {
        val sampleRate = 22050
        val numSamples = sampleRate * durationSeconds
        val pcm = ByteArray(numSamples * 2)
        val pentatonic = doubleArrayOf(1.0, 1.125, 1.25, 1.5, 1.667, 2.0)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIndex = ((t / 2.0).toInt()) % pentatonic.size
            val localT = t % 2.0
            val chimeEnv = exp(-localT * 2.2)
            val pad = sin(2.0 * PI * (baseFreq * 0.5) * t) * 0.18 +
                sin(2.0 * PI * (baseFreq * 0.75) * t) * 0.12
            val chime = sin(2.0 * PI * (baseFreq * pentatonic[noteIndex]) * t) * chimeEnv * 0.25
            val fadeEdge = when {
                t < 0.2 -> t / 0.2
                t > durationSeconds - 0.2 -> (durationSeconds - t) / 0.2
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            val s = ((pad + chime) * fadeEdge * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            pcm[i * 2] = (s and 0xFF).toByte()
            pcm[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        writeWavFile(outFile, pcm, sampleRate)
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + pcmData.size)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // Mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(pcmData.size)
        FileOutputStream(file).use { out ->
            out.write(header.array())
            out.write(pcmData)
        }
    }
}
