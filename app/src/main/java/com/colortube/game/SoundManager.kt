package com.colortube.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
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
 * Provides responsive synthesized audio effects (water pour, bubbles, cork pop, completion chime, etc.)
 * and haptic feedback. Works 100% offline with zero third-party copyrighted assets.
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
     * Synthesizes and plays a realistic physical liquid-pouring stream sound.
     * Starts when liquid leaves the source spout, lasts for the exact flow duration,
     * and accurately simulates acoustic fluid turbulence, cavity cavitation bubbles,
     * and subtle glass resonance with rising Helmholtz pitch.
     */
    fun playPourStream(volumeUnits: Int, flowDurationMs: Long) {
        if (!prefs.soundEnabled) return
        thread {
            try {
                val clampedMs = flowDurationMs.coerceIn(250L, 2500L).toInt()
                val numSamples = (sampleRate * (clampedMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                var pinkB0 = 0.0
                var pinkB1 = 0.0
                var pinkB2 = 0.0
                val random = java.util.Random(42)

                val attackSamples = (sampleRate * 0.05).toInt()
                val releaseSamples = (sampleRate * 0.08).toInt()

                // Bubble bursts phase
                var bubblePhase = 0.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples

                    // Envelope: fast smooth attack, sustained stream body, smooth release
                    val env = when {
                        i < attackSamples -> i.toDouble() / attackSamples
                        i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
                        else -> 1.0
                    }

                    // 1. Turbulent fluid noise (Pink noise approximation)
                    val white = (random.nextDouble() * 2.0 - 1.0)
                    pinkB0 = 0.99765 * pinkB0 + white * 0.0990460
                    pinkB1 = 0.96300 * pinkB1 + white * 0.2965164
                    pinkB2 = 0.57000 * pinkB2 + white * 1.0526913
                    val pink = (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.18

                    // 2. Liquid cavitation & bubbling inside glass cavity
                    // Helmholtz pitch rises gently from ~500Hz to ~780Hz as liquid fills the glass
                    val baseBubbleFreq = 480.0 + (progress * 260.0)
                    val bubbleMod = 90.0 * sin(2.0 * PI * 14.0 * t) + 40.0 * sin(2.0 * PI * 33.0 * t)
                    val bubbleFreq = baseBubbleFreq + bubbleMod
                    bubblePhase += 2.0 * PI * bubbleFreq / sampleRate
                    val bubbleWave = sin(bubblePhase) * 0.35

                    // 3. Delicate glass resonance harmonic (crystal ringing ~1850Hz)
                    val glassResonance = sin(2.0 * PI * 1850.0 * t) * 0.07

                    // Combined physical fluid sound
                    val mixed = (pink * 0.55 + bubbleWave * 0.38 + glassResonance) * env
                    val scaled = (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.72).toInt()
                    buffer[i] = scaled.toShort()
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
                Thread.sleep(clampedMs.toLong() + 25)
                audioTrack.release()
            } catch (_: Exception) {}
        }
        vibrate(35L + (volumeUnits * 15L).coerceAtMost(60L))
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
