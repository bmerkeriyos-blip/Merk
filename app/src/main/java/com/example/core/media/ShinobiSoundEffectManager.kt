package com.example.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class ShinobiSfx {
    SCROLL_OPEN,
    SHURIKEN,
    JUTSU,
    MISSION_COMPLETE,
    XP_GAIN,
    RANK_UP,
    CHAKRA_PULSE,
    BUTTON_TAP
}

/**
 * Synthesizes and plays subtle shinobi sound effects (e.g., shuriken throw on task completion,
 * jutsu seal chime on starting focus training) via SoundPool with haptic coordination.
 */
class ShinobiSoundEffectManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var soundPool: SoundPool? = null
    private val sfxMap = mutableMapOf<ShinobiSfx, Int>()

    @Volatile
    private var sfxVolume: Float = 0.8f

    @Volatile
    private var soundEnabled: Boolean = true

    @Volatile
    private var hapticsEnabled: Boolean = true

    @Volatile
    private var quietHoursMuted: Boolean = false

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()
        soundPool = pool

        scope.launch {
            val sfxDir = File(context.cacheDir, "shinobi_sfx_v2").apply { mkdirs() }
            ShinobiSfx.entries.forEach { sfx ->
                val file = File(sfxDir, "${sfx.name.lowercase()}.wav")
                if (!file.exists()) {
                    writeSfxWav(sfx, file)
                }
                val id = pool.load(file.absolutePath, 1)
                sfxMap[sfx] = id
            }
        }
    }

    fun updateConfig(
        volume: Float,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        quietMuted: Boolean
    ) {
        this.sfxVolume = volume.coerceIn(0f, 1f)
        this.soundEnabled = soundEnabled
        this.hapticsEnabled = hapticsEnabled
        this.quietHoursMuted = quietMuted
    }

    fun play(sfx: ShinobiSfx) {
        if (quietHoursMuted) return
        val id = sfxMap[sfx]
        // Keep SFX naturally subtle (capped at 55% normalized pool gain) so alerts never startle
        val effectiveGain = (sfxVolume * 0.55f).coerceIn(0f, 1f)
        if (id != null && soundEnabled && effectiveGain > 0.01f) {
            soundPool?.play(id, effectiveGain, effectiveGain, 1, 0, 1f)
        }
        if (hapticsEnabled) {
            triggerHaptic(sfx)
        }
    }

    fun release() {
        scope.cancel()
        soundPool?.release()
        soundPool = null
        sfxMap.clear()
    }

    private fun triggerHaptic(sfx: ShinobiSfx) {
        val duration = when (sfx) {
            ShinobiSfx.RANK_UP, ShinobiSfx.MISSION_COMPLETE -> 40L
            ShinobiSfx.JUTSU -> 30L
            ShinobiSfx.SHURIKEN, ShinobiSfx.XP_GAIN -> 22L
            else -> 14L
        }
        val amp = when (sfx) {
            ShinobiSfx.RANK_UP -> 180
            ShinobiSfx.JUTSU, ShinobiSfx.MISSION_COMPLETE -> 130
            ShinobiSfx.SHURIKEN -> 105
            else -> 80
        }
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(duration, amp))
    }

    private fun writeSfxWav(sfx: ShinobiSfx, outFile: File) {
        val sampleRate = 22050
        val durationMs = when (sfx) {
            ShinobiSfx.RANK_UP -> 520
            ShinobiSfx.MISSION_COMPLETE -> 380
            ShinobiSfx.JUTSU -> 360
            ShinobiSfx.CHAKRA_PULSE -> 300
            ShinobiSfx.SCROLL_OPEN -> 250
            ShinobiSfx.SHURIKEN -> 220
            ShinobiSfx.XP_GAIN -> 210
            ShinobiSfx.BUTTON_TAP -> 120
        }
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ByteArrayOutputStream(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Smooth 12ms fade-in to avoid speaker clicks
            val attack = (t / 0.012).coerceIn(0.0, 1.0)
            val wave: Double = when (sfx) {
                ShinobiSfx.SHURIKEN -> {
                    // Subtle spinning blade air-whoosh + crisp light metallic glint
                    val whooshEnv = exp(-t * 15.0) * attack
                    val glintStart = (t - 0.045).coerceAtLeast(0.0)
                    val glintEnv = if (t >= 0.045) exp(-glintStart * 18.0) else 0.0
                    val sweepFreq = 1350.0 * exp(-t * 9.0) + 320.0
                    val flutter = 0.72 + 0.28 * sin(2.0 * PI * 36.0 * t)
                    val whoosh = sin(2.0 * PI * sweepFreq * t) * flutter * whooshEnv * 0.24
                    val glint = (
                        sin(2.0 * PI * 1860.0 * glintStart) * 0.6 +
                            sin(2.0 * PI * 2790.0 * glintStart) * 0.4
                        ) * glintEnv * 0.16
                    whoosh + glint
                }

                ShinobiSfx.JUTSU -> {
                    // Subtle hand-seal bamboo/wood clack followed by calm pentatonic chime
                    val seal1Env = exp(-t * 48.0) * attack
                    val seal1 = (sin(2.0 * PI * 390.0 * t) + 0.5 * sin(2.0 * PI * 780.0 * t)) * seal1Env * 0.22
                    val chimeStart = (t - 0.065).coerceAtLeast(0.0)
                    val chimeEnv = if (t >= 0.065) {
                        val cAttack = (chimeStart / 0.015).coerceIn(0.0, 1.0)
                        exp(-chimeStart * 8.5) * cAttack
                    } else 0.0
                    val chime = (
                        sin(2.0 * PI * 587.33 * chimeStart) * 0.65 +
                            sin(2.0 * PI * 880.0 * chimeStart) * 0.35
                        ) * chimeEnv * 0.26
                    seal1 + chime
                }

                ShinobiSfx.RANK_UP -> {
                    val env = exp(-t * 5.8) * attack
                    val freq = 523.25 + (t * 620.0)
                    sin(2.0 * PI * freq * t) * env * 0.36
                }

                ShinobiSfx.MISSION_COMPLETE -> {
                    val env = exp(-t * 7.5) * attack
                    val freq = if (t < 0.16) 587.33 else 880.0
                    sin(2.0 * PI * freq * t) * env * 0.32
                }

                ShinobiSfx.XP_GAIN -> {
                    val env = exp(-t * 10.0) * attack
                    sin(2.0 * PI * (659.25 + t * 320.0) * t) * env * 0.30
                }

                ShinobiSfx.SCROLL_OPEN -> {
                    val env = exp(-t * 11.0) * attack
                    sin(2.0 * PI * (330.0 + t * 420.0) * t) * env * 0.28
                }

                ShinobiSfx.CHAKRA_PULSE -> {
                    val env = exp(-t * 8.0) * attack
                    val freq = 293.66 * (1.0 + 0.10 * sin(2.0 * PI * 10.0 * t))
                    sin(2.0 * PI * freq * t) * env * 0.28
                }

                ShinobiSfx.BUTTON_TAP -> {
                    val env = exp(-t * 22.0) * attack
                    sin(2.0 * PI * 740.0 * t) * env * 0.20
                }
            }

            val clamped = (wave * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            pcm.write(clamped.toInt() and 0xFF)
            pcm.write((clamped.toInt() shr 8) and 0xFF)
        }
        writeWavFile(outFile, pcm.toByteArray(), sampleRate)
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
