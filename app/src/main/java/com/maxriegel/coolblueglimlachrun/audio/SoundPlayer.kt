package com.maxriegel.coolblueglimlachrun.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

enum class SoundEffect {
    JUMP,
    SMILE_COIN,
    BOX_HIT,
    POWERUP,
    STOMP,
    SPRING,
    HURT,
    LEVEL_CLEAR,
    SELECT
}

interface SoundPlayer {
    var isMuted: Boolean
    fun play(effect: SoundEffect)
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }
    fun release() {}
}

class NoOpSoundPlayer : SoundPlayer {
    override var isMuted: Boolean = false
    val playedEffects = mutableListOf<SoundEffect>()

    override fun play(effect: SoundEffect) {
        if (!isMuted) {
            playedEffects.add(effect)
        }
    }
}

class ChiptuneSoundPlayer : SoundPlayer {
    override var isMuted: Boolean = false

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "CoolblueAudioSynth").apply { isDaemon = true }
    }

    private val sampleRate = 22050
    private val buffers: Map<SoundEffect, ShortArray> by lazy {
        mapOf(
            SoundEffect.JUMP to synthesizeSweep(290.0, 680.0, 120, 0.24),
            SoundEffect.SMILE_COIN to synthesizeNotes(
                listOf(987.77 to 60, 1318.51 to 140),
                0.24
            ),
            SoundEffect.BOX_HIT to synthesizeNotes(
                listOf(220.0 to 45, 523.25 to 75),
                0.28
            ),
            SoundEffect.POWERUP to synthesizeNotes(
                listOf(
                    523.25 to 60, 659.25 to 60, 783.99 to 60,
                    1046.50 to 70, 1318.51 to 130
                ),
                0.24
            ),
            SoundEffect.STOMP to synthesizeNotes(
                listOf(440.0 to 45, 659.25 to 60, 880.0 to 90),
                0.26
            ),
            SoundEffect.SPRING to synthesizeSweep(220.0, 1180.0, 200, 0.26),
            SoundEffect.HURT to synthesizeSweep(420.0, 130.0, 210, 0.28),
            SoundEffect.LEVEL_CLEAR to synthesizeNotes(
                listOf(
                    523.25 to 85, 659.25 to 85, 783.99 to 85,
                    1046.50 to 130, 783.99 to 80, 1046.50 to 260
                ),
                0.26
            ),
            SoundEffect.SELECT to synthesizeNotes(
                listOf(659.25 to 55, 880.0 to 80),
                0.22
            )
        )
    }

    override fun play(effect: SoundEffect) {
        if (isMuted) return
        executor.execute {
            val pcm = buffers[effect] ?: return@execute
            playPcm(pcm)
        }
    }

    private fun playPcm(pcm: ShortArray) {
        var track: AudioTrack? = null
        try {
            val byteSize = pcm.size * 2
            track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(byteSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            val durationMs = (pcm.size * 1000L / sampleRate) + 15L
            Thread.sleep(durationMs)
        } catch (_: Throwable) {
            // Audio hardware may be unavailable in headless environments
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun synthesizeSweep(
        startHz: Double,
        endHz: Double,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count.coerceAtLeast(1)
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - t).coerceIn(0.0, 1.0)
            val wave = if (sin(phase) >= 0.0) 1.0 else -1.0
            out[i] = (wave * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthesizeNotes(
        notes: List<Pair<Double, Int>>,
        volume: Double
    ): ShortArray {
        val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
        val out = ShortArray(totalSamples)
        var cursor = 0
        for ((freq, ms) in notes) {
            val count = (sampleRate * ms) / 1000
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count.coerceAtLeast(1)
                phase += 2.0 * PI * freq / sampleRate
                val attack = (t * 14.0).coerceAtMost(1.0)
                val decay = (1.0 - t * 0.72).coerceIn(0.15, 1.0)
                // Warm chiptune pulse+sine blend
                val square = if (sin(phase) >= 0.0) 0.7 else -0.7
                val harmonic = sin(phase) * 0.3
                val sample = (square + harmonic) * attack * decay * volume * Short.MAX_VALUE
                if (cursor < out.size) {
                    out[cursor++] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }
        return out
    }

    override fun release() {
        executor.shutdownNow()
    }
}
