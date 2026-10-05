package com.colortube.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * SoundManager
 *
 * Provides responsive physical audio effects (authentic liquid pour, bubbles, cork pop, completion chime, etc.)
 * and haptic feedback.
 */
class SoundManager(private val context: Context, private val prefs: GamePreferences) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sampleRate = 22050

    // High-performance low-latency SoundPool for authentic pour audio
    private var soundPool: SoundPool? = null
    private var pourSoundId: Int = 0
    private var isPourSoundLoaded: Boolean = false
    private var activePourStreamId: Int = 0

    // Robust MediaPlayer fallback
    private var pourMediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopPourRunnable: Runnable? = null

    init {
        initPourSound()
    }

    private fun initPourSound() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (sampleId == pourSoundId && status == 0) {
                            isPourSoundLoaded = true
                        }
                    }
                }
            pourSoundId = soundPool?.load(context, R.raw.sound_pour, 1) ?: 0
        } catch (_: Exception) {}

        try {
            pourMediaPlayer = MediaPlayer.create(context, R.raw.sound_pour)?.apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttributes)
                isLooping = false
            }
        } catch (_: Exception) {}
    }

    fun playClick() {
        if (!prefs.soundEnabled) return
        playTone(frequency = 600.0, durationMs = 35, decay = 20.0)
    }

    fun playTubeSelect() {
        if (!prefs.soundEnabled) return
        playTone(frequency = 880.0, durationMs = 60, decay = 15.0)
        vibrate(20)
    }

    /**
     * Plays the authentic recorded liquid-pouring sound from user audio resource (R.raw.sound_pour).
     * Synchronized with actual liquid pour duration and liquid amount:
     * - Small pour (1 unit) -> short sound (~420ms)
     * - Medium pour (2 units) -> medium-duration sound (~720ms)
     * - Large/full pour (3-4 units) -> longer sound (~1020ms - 1320ms)
     * Stops immediately when the pour stream finishes.
     */
    fun playPourStream(volumeUnits: Int, flowDurationMs: Long) {
        if (!prefs.soundEnabled) return

        stopPourRunnable?.let {
            mainHandler.removeCallbacks(it)
            stopPourRunnable = null
        }

        val vol = when (volumeUnits) {
            1 -> 0.85f
            2 -> 0.95f
            else -> 1.0f
        }

        try {
            if (isPourSoundLoaded && pourSoundId != 0) {
                // Primary: SoundPool for zero-latency instant playback
                if (activePourStreamId != 0) {
                    soundPool?.stop(activePourStreamId)
                }
                activePourStreamId = soundPool?.play(pourSoundId, vol, vol, 1, 0, 1.0f) ?: 0
            } else {
                // Secondary: MediaPlayer fallback
                pourMediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                        mp.seekTo(0)
                        mp.setVolume(vol, vol)
                        mp.start()
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        // Automatically schedule stop to match exact flowDurationMs
        val runnable = Runnable {
            stopPourStream()
        }
        stopPourRunnable = runnable
        mainHandler.postDelayed(runnable, flowDurationMs)

        vibrate(30L + (volumeUnits * 15L).coerceAtMost(60L))
    }

    /**
     * Stops the active liquid-pouring sound immediately when the pour finishes.
     */
    fun stopPourStream() {
        stopPourRunnable?.let {
            mainHandler.removeCallbacks(it)
            stopPourRunnable = null
        }

        try {
            if (activePourStreamId != 0) {
                soundPool?.stop(activePourStreamId)
                activePourStreamId = 0
            }
        } catch (_: Exception) {}

        try {
            pourMediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    mp.seekTo(0)
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopPourStream()
        try {
            soundPool?.release()
            soundPool = null
        } catch (_: Exception) {}
        try {
            pourMediaPlayer?.release()
            pourMediaPlayer = null
        } catch (_: Exception) {}
    }

    /**
     * Crisp glass chime & settling droplet sound when liquid settles into destination tube.
     */
    fun playPourLanding() {
        if (!prefs.soundEnabled) return
        thread {
            try {
                val durationMs = 120
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-28.0 * t)
                    // High glass plink + liquid drop
                    val wave = sin(2.0 * PI * 2150.0 * t) * 0.45 + sin(2.0 * PI * 820.0 * t) * 0.55
                    buffer[i] = (wave * env * Short.MAX_VALUE * 0.60).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 15)
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    @Deprecated("Use playPourStream with volume and duration synchronization")
    fun playPour() {
        playPourStream(volumeUnits = 1, flowDurationMs = 550L)
    }

    fun playTubeComplete() {
        if (!prefs.soundEnabled) return
        thread {
            // Cork pop followed by happy ascending chime
            playToneSync(frequency = 280.0, durationMs = 80, decay = 30.0) // pop
            Thread.sleep(40)
            playToneSync(frequency = 523.25, durationMs = 120, decay = 8.0) // C5
            playToneSync(frequency = 659.25, durationMs = 120, decay = 8.0) // E5
            playToneSync(frequency = 783.99, durationMs = 180, decay = 6.0) // G5
            playToneSync(frequency = 1046.50, durationMs = 260, decay = 4.0) // C6
        }
        vibratePattern(longArrayOf(0, 30, 40, 60))
    }

    fun playInvalidMove() {
        if (!prefs.soundEnabled) return
        thread {
            playToneSync(frequency = 220.0, durationMs = 90, decay = 15.0)
            playToneSync(frequency = 180.0, durationMs = 110, decay = 15.0)
        }
        vibratePattern(longArrayOf(0, 50, 40, 50))
    }

    fun playLevelWin() {
        if (!prefs.soundEnabled) return
        thread {
            val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0, 1108.73)
            for (freq in notes) {
                playToneSync(freq, 140, 6.0)
                Thread.sleep(30)
            }
        }
        vibratePattern(longArrayOf(0, 40, 60, 40, 60, 100))
    }

    fun playReward() {
        if (!prefs.soundEnabled) return
        thread {
            playToneSync(frequency = 784.0, durationMs = 100, decay = 8.0)
            playToneSync(frequency = 988.0, durationMs = 100, decay = 8.0)
            playToneSync(frequency = 1175.0, durationMs = 200, decay = 5.0)
        }
        vibrate(50)
    }

    fun vibrate(durationMs: Long) {
        if (!prefs.hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibratePattern(timings: LongArray) {
        if (!prefs.hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun playTone(frequency: Double, durationMs: Int, decay: Double) {
        thread {
            playToneSync(frequency, durationMs, decay)
        }
    }

    private fun playToneSync(frequency: Double, durationMs: Int, decay: Double) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val decayFactor = decay / durationMs

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-decayFactor * (i.toDouble() / sampleRate * 1000.0))
                val wave = sin(2.0 * PI * frequency * t)
                buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 30)
            audioTrack.release()
        } catch (_: Exception) {}
    }
}
