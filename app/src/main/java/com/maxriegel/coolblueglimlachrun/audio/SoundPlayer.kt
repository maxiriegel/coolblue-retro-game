package com.maxriegel.coolblueglimlachrun.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class SoundEffect {
    JUMP,
    LAND,
    SMILE_COIN,
    BOX_HIT,
    BOX_EMPTY,
    POWERUP,
    STOMP,
    SPRING,
    SHIELD_SAVE,
    HURT,
    SPLASH,
    LEVEL_CLEAR,
    VICTORY,
    GAME_OVER,
    SELECT
}

enum class MusicScene {
    NONE,
    TITLE,
    WAREHOUSE,
    CANALS
}

interface SoundPlayer {
    var isMuted: Boolean
    var isMusicEnabled: Boolean
        get() = false
        set(_) {}

    fun play(effect: SoundEffect)

    fun playCoinStreak(streak: Int) {
        play(SoundEffect.SMILE_COIN)
    }

    fun playCharacterMotif(character: CharacterType) {}

    fun setMusicScene(scene: MusicScene) {}

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun toggleMusic(): Boolean {
        isMusicEnabled = !isMusicEnabled
        return isMusicEnabled
    }

    fun onPause() {}
    fun onResume() {}
    fun release() {}
}

class NoOpSoundPlayer : SoundPlayer {
    override var isMuted: Boolean = false
    override var isMusicEnabled: Boolean = true
    val playedEffects = mutableListOf<SoundEffect>()
    var currentScene: MusicScene = MusicScene.NONE
        private set

    override fun play(effect: SoundEffect) {
        if (!isMuted) {
            playedEffects.add(effect)
        }
    }

    override fun setMusicScene(scene: MusicScene) {
        currentScene = scene
    }
}

class ChiptuneSoundPlayer : SoundPlayer {
    @Volatile
    override var isMuted: Boolean = false

    @Volatile
    override var isMusicEnabled: Boolean = true

    private val paused = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val activeScene = AtomicReference(MusicScene.NONE)

    private val threadId = AtomicInteger(1)
    private val sfxPool = Executors.newFixedThreadPool(6) { runnable ->
        Thread(runnable, "CoolblueSfx-${threadId.getAndIncrement()}").apply { isDaemon = true }
    }

    private val sampleRate = 44100

    private data class VoiceNote(
        val startHz: Double,
        val endHz: Double = startHz,
        val durationMs: Int,
        val wave: WaveType = WaveType.PULSE_25,
        val volume: Double = 0.25,
        val vibratoHz: Double = 0.0,
        val vibratoDepth: Double = 0.0,
        val noiseMix: Double = 0.0
    )

    private enum class WaveType {
        PULSE_12,
        PULSE_25,
        SQUARE_50,
        TRIANGLE,
        NOISE
    }

    private val buffers: Map<SoundEffect, ShortArray> by lazy {
        mapOf(
            SoundEffect.JUMP to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(230.0, 540.0, 65, WaveType.PULSE_25, 0.22),
                        VoiceNote(540.0, 760.0, 75, WaveType.PULSE_25, 0.19)
                    )
                ),
                renderSequence(
                    listOf(VoiceNote(140.0, 380.0, 130, WaveType.TRIANGLE, 0.20))
                )
            ),
            SoundEffect.LAND to renderSequence(
                listOf(VoiceNote(155.0, 68.0, 55, WaveType.TRIANGLE, 0.18, noiseMix = 0.28))
            ),
            SoundEffect.SMILE_COIN to buildCoinSound(semitoneOffset = 0),
            SoundEffect.BOX_HIT to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(190.0, 420.0, 45, WaveType.PULSE_25, 0.24, noiseMix = 0.30),
                        VoiceNote(587.33, 783.99, 75, WaveType.TRIANGLE, 0.22)
                    )
                )
            ),
            SoundEffect.BOX_EMPTY to renderSequence(
                listOf(
                    VoiceNote(180.0, 105.0, 65, WaveType.TRIANGLE, 0.24, noiseMix = 0.35)
                )
            ),
            SoundEffect.POWERUP to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(523.25, 523.25, 52, WaveType.PULSE_25, 0.21),
                        VoiceNote(659.25, 659.25, 52, WaveType.PULSE_25, 0.21),
                        VoiceNote(783.99, 783.99, 52, WaveType.PULSE_25, 0.22),
                        VoiceNote(987.77, 987.77, 56, WaveType.PULSE_25, 0.22),
                        VoiceNote(1046.50, 1046.50, 62, WaveType.PULSE_12, 0.23),
                        VoiceNote(1318.51, 1318.51, 150, WaveType.PULSE_25, 0.25, vibratoHz = 9.0, vibratoDepth = 0.012)
                    )
                ),
                renderSequence(
                    listOf(
                        VoiceNote(261.63, 261.63, 156, WaveType.TRIANGLE, 0.18),
                        VoiceNote(392.00, 392.00, 118, WaveType.TRIANGLE, 0.18),
                        VoiceNote(523.25, 523.25, 150, WaveType.TRIANGLE, 0.20)
                    )
                )
            ),
            SoundEffect.STOMP to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(210.0, 130.0, 36, WaveType.NOISE, 0.18),
                        VoiceNote(440.0, 659.25, 55, WaveType.PULSE_25, 0.23),
                        VoiceNote(880.0, 1174.66, 85, WaveType.PULSE_25, 0.22)
                    )
                ),
                renderSequence(
                    listOf(
                        VoiceNote(220.0, 440.0, 165, WaveType.TRIANGLE, 0.18)
                    )
                )
            ),
            SoundEffect.SPRING to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(195.0, 980.0, 195, WaveType.PULSE_25, 0.23, vibratoHz = 26.0, vibratoDepth = 0.045),
                        VoiceNote(980.0, 1320.0, 75, WaveType.PULSE_12, 0.17)
                    )
                ),
                renderSequence(
                    listOf(VoiceNote(130.0, 660.0, 240, WaveType.TRIANGLE, 0.21))
                )
            ),
            SoundEffect.SHIELD_SAVE to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(880.0, 440.0, 70, WaveType.PULSE_12, 0.23, noiseMix = 0.25),
                        VoiceNote(659.25, 987.77, 110, WaveType.PULSE_25, 0.22)
                    )
                ),
                renderSequence(
                    listOf(VoiceNote(220.0, 330.0, 180, WaveType.TRIANGLE, 0.18))
                )
            ),
            SoundEffect.HURT to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(440.0, 293.66, 70, WaveType.PULSE_12, 0.25, noiseMix = 0.30),
                        VoiceNote(293.66, 196.00, 75, WaveType.PULSE_12, 0.24, noiseMix = 0.20),
                        VoiceNote(196.00, 123.47, 110, WaveType.SQUARE_50, 0.22)
                    )
                ),
                renderSequence(
                    listOf(VoiceNote(160.0, 75.0, 240, WaveType.TRIANGLE, 0.20))
                )
            ),
            SoundEffect.SPLASH to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(520.0, 160.0, 95, WaveType.NOISE, 0.22),
                        VoiceNote(320.0, 620.0, 90, WaveType.TRIANGLE, 0.18, noiseMix = 0.35)
                    )
                )
            ),
            SoundEffect.LEVEL_CLEAR to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(523.25, 523.25, 85, WaveType.PULSE_25, 0.22),
                        VoiceNote(659.25, 659.25, 85, WaveType.PULSE_25, 0.22),
                        VoiceNote(783.99, 783.99, 85, WaveType.PULSE_25, 0.23),
                        VoiceNote(1046.50, 1046.50, 145, WaveType.PULSE_25, 0.25),
                        VoiceNote(880.00, 880.00, 85, WaveType.PULSE_25, 0.22),
                        VoiceNote(1046.50, 1046.50, 280, WaveType.PULSE_25, 0.26, vibratoHz = 7.5, vibratoDepth = 0.01)
                    )
                ),
                renderSequence(
                    listOf(
                        VoiceNote(261.63, 261.63, 255, WaveType.TRIANGLE, 0.20),
                        VoiceNote(349.23, 349.23, 230, WaveType.TRIANGLE, 0.20),
                        VoiceNote(523.25, 523.25, 280, WaveType.TRIANGLE, 0.22)
                    )
                )
            ),
            SoundEffect.VICTORY to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(523.25, 523.25, 80, WaveType.PULSE_25, 0.23),
                        VoiceNote(659.25, 659.25, 80, WaveType.PULSE_25, 0.23),
                        VoiceNote(783.99, 783.99, 80, WaveType.PULSE_25, 0.23),
                        VoiceNote(1046.50, 1046.50, 130, WaveType.PULSE_25, 0.25),
                        VoiceNote(1174.66, 1174.66, 95, WaveType.PULSE_25, 0.24),
                        VoiceNote(1318.51, 1318.51, 115, WaveType.PULSE_25, 0.25),
                        VoiceNote(1567.98, 1567.98, 340, WaveType.PULSE_25, 0.27, vibratoHz = 8.0, vibratoDepth = 0.012)
                    )
                ),
                renderSequence(
                    listOf(
                        VoiceNote(261.63, 261.63, 240, WaveType.TRIANGLE, 0.20),
                        VoiceNote(392.00, 392.00, 240, WaveType.TRIANGLE, 0.20),
                        VoiceNote(523.25, 523.25, 440, WaveType.TRIANGLE, 0.22)
                    )
                )
            ),
            SoundEffect.GAME_OVER to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(523.25, 493.88, 120, WaveType.PULSE_25, 0.22),
                        VoiceNote(466.16, 440.00, 120, WaveType.PULSE_25, 0.22),
                        VoiceNote(392.00, 349.23, 140, WaveType.PULSE_25, 0.21),
                        VoiceNote(329.63, 261.63, 260, WaveType.SQUARE_50, 0.22, vibratoHz = 6.0, vibratoDepth = 0.015)
                    )
                ),
                renderSequence(
                    listOf(
                        VoiceNote(261.63, 196.00, 320, WaveType.TRIANGLE, 0.19),
                        VoiceNote(196.00, 130.81, 320, WaveType.TRIANGLE, 0.19)
                    )
                )
            ),
            SoundEffect.SELECT to renderSequence(
                listOf(
                    VoiceNote(659.25, 659.25, 42, WaveType.PULSE_25, 0.19),
                    VoiceNote(987.77, 987.77, 68, WaveType.TRIANGLE, 0.21)
                )
            )
        )
    }

    private val coinStreakBuffers: List<ShortArray> by lazy {
        (0..6).map { streak -> buildCoinSound(semitoneOffset = streak) }
    }

    private val characterMotifs: Map<CharacterType, ShortArray> by lazy {
        mapOf(
            CharacterType.MAX to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(523.25, 523.25, 50, WaveType.PULSE_25, 0.20),
                        VoiceNote(659.25, 659.25, 50, WaveType.PULSE_25, 0.21),
                        VoiceNote(783.99, 783.99, 55, WaveType.PULSE_25, 0.22),
                        VoiceNote(1046.50, 1046.50, 110, WaveType.PULSE_25, 0.24)
                    )
                ),
                renderSequence(listOf(VoiceNote(261.63, 523.25, 260, WaveType.TRIANGLE, 0.16)))
            ),
            CharacterType.REMI to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(587.33, 587.33, 48, WaveType.PULSE_25, 0.20),
                        VoiceNote(739.99, 739.99, 48, WaveType.PULSE_25, 0.21),
                        VoiceNote(880.00, 987.77, 58, WaveType.PULSE_12, 0.22),
                        VoiceNote(1174.66, 1174.66, 110, WaveType.PULSE_25, 0.24)
                    )
                ),
                renderSequence(listOf(VoiceNote(293.66, 587.33, 260, WaveType.TRIANGLE, 0.16)))
            ),
            CharacterType.VERO to mixLayers(
                renderSequence(
                    listOf(
                        VoiceNote(659.25, 659.25, 46, WaveType.PULSE_25, 0.20),
                        VoiceNote(880.00, 880.00, 46, WaveType.PULSE_25, 0.21),
                        VoiceNote(1046.50, 1046.50, 52, WaveType.TRIANGLE, 0.23),
                        VoiceNote(1318.51, 1567.98, 115, WaveType.PULSE_25, 0.24, vibratoHz = 8.5, vibratoDepth = 0.01)
                    )
                ),
                renderSequence(listOf(VoiceNote(329.63, 659.25, 255, WaveType.TRIANGLE, 0.17)))
            )
        )
    }

    private val sceneLoops: Map<MusicScene, ShortArray> by lazy {
        mapOf(
            MusicScene.TITLE to buildGrooveLoop(
                stepMs = 155,
                melody = doubleArrayOf(
                    523.25, 659.25, 783.99, 659.25, 880.00, 783.99, 659.25, 587.33,
                    523.25, 659.25, 783.99, 1046.50, 987.77, 880.00, 783.99, 0.0
                ),
                bass = doubleArrayOf(
                    130.81, 0.0, 196.00, 0.0, 220.00, 0.0, 174.61, 0.0,
                    130.81, 0.0, 196.00, 0.0, 146.83, 0.0, 196.00, 0.0
                ),
                masterVol = 0.085
            ),
            MusicScene.WAREHOUSE to buildGrooveLoop(
                stepMs = 132,
                melody = doubleArrayOf(
                    523.25, 0.0, 659.25, 783.99, 880.00, 783.99, 659.25, 523.25,
                    587.33, 659.25, 783.99, 659.25, 523.25, 587.33, 659.25, 0.0,
                    523.25, 0.0, 659.25, 783.99, 1046.50, 987.77, 880.00, 783.99,
                    698.46, 783.99, 880.00, 783.99, 659.25, 587.33, 783.99, 0.0
                ),
                bass = doubleArrayOf(
                    130.81, 130.81, 196.00, 196.00, 220.00, 220.00, 174.61, 174.61,
                    146.83, 146.83, 196.00, 196.00, 130.81, 146.83, 164.81, 196.00,
                    130.81, 130.81, 196.00, 196.00, 220.00, 220.00, 164.81, 164.81,
                    174.61, 174.61, 196.00, 196.00, 130.81, 196.00, 130.81, 0.0
                ),
                masterVol = 0.095
            ),
            MusicScene.CANALS to buildGrooveLoop(
                stepMs = 128,
                melody = doubleArrayOf(
                    587.33, 739.99, 880.00, 0.0, 987.77, 880.00, 739.99, 659.25,
                    587.33, 659.25, 739.99, 880.00, 739.99, 659.25, 587.33, 0.0,
                    659.25, 783.99, 987.77, 1174.66, 1174.66, 987.77, 880.00, 739.99,
                    659.25, 739.99, 880.00, 987.77, 880.00, 739.99, 880.00, 0.0
                ),
                bass = doubleArrayOf(
                    146.83, 220.00, 146.83, 220.00, 196.00, 246.94, 164.81, 220.00,
                    146.83, 220.00, 185.00, 220.00, 164.81, 220.00, 146.83, 0.0,
                    164.81, 246.94, 196.00, 246.94, 220.00, 277.18, 185.00, 220.00,
                    164.81, 220.00, 220.00, 164.81, 146.83, 220.00, 146.83, 0.0
                ),
                masterVol = 0.095
            )
        )
    }

    private val musicThread = Thread(::runMusicLoop, "CoolblueMusicSynth").apply {
        isDaemon = true
        start()
    }

    override fun play(effect: SoundEffect) {
        if (isMuted || paused.get() || released.get()) return
        try {
            sfxPool.execute {
                val pcm = buffers[effect] ?: return@execute
                playPcm(pcm)
            }
        } catch (_: Throwable) {
        }
    }

    override fun playCoinStreak(streak: Int) {
        if (isMuted || paused.get() || released.get()) return
        val idx = streak.coerceIn(0, 6)
        try {
            sfxPool.execute {
                playPcm(coinStreakBuffers[idx])
            }
        } catch (_: Throwable) {
        }
    }

    override fun playCharacterMotif(character: CharacterType) {
        if (isMuted || paused.get() || released.get()) return
        try {
            sfxPool.execute {
                val pcm = characterMotifs[character] ?: return@execute
                playPcm(pcm)
            }
        } catch (_: Throwable) {
        }
    }

    override fun setMusicScene(scene: MusicScene) {
        activeScene.set(scene)
    }

    override fun onPause() {
        paused.set(true)
    }

    override fun onResume() {
        paused.set(false)
    }

    private fun runMusicLoop() {
        var streamTrack: AudioTrack? = null
        var cursor = 0
        var lastScene = MusicScene.NONE
        val chunkSize = 2048

        try {
            while (!released.get()) {
                val scene = activeScene.get()
                val shouldPlay = !isMuted && isMusicEnabled && !paused.get() && scene != MusicScene.NONE
                if (!shouldPlay) {
                    if (streamTrack != null) {
                        try {
                            streamTrack.pause()
                            streamTrack.flush()
                        } catch (_: Throwable) {
                        }
                    }
                    Thread.sleep(60L)
                    continue
                }

                if (scene != lastScene) {
                    cursor = 0
                    lastScene = scene
                }
                val loopBuffer = sceneLoops[scene]
                if (loopBuffer == null || loopBuffer.isEmpty()) {
                    Thread.sleep(60L)
                    continue
                }

                if (streamTrack == null) {
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(chunkSize * 4)
                    streamTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBuf)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                }

                if (streamTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    streamTrack.play()
                }

                val remaining = loopBuffer.size - cursor
                val toWrite = min(chunkSize, remaining)
                streamTrack.write(loopBuffer, cursor, toWrite)
                cursor += toWrite
                if (cursor >= loopBuffer.size) cursor = 0
            }
        } catch (_: Throwable) {
            // Audio hardware unavailable in headless / CI environments
        } finally {
            try {
                streamTrack?.stop()
                streamTrack?.release()
            } catch (_: Throwable) {
            }
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
            val durationMs = (pcm.size * 1000L / sampleRate) + 18L
            Thread.sleep(durationMs)
        } catch (_: Throwable) {
            // Safe no-op in headless test environments
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun buildCoinSound(semitoneOffset: Int): ShortArray {
        val ratio = 2.0.pow(semitoneOffset / 12.0)
        val b5 = 987.77 * ratio
        val e6 = 1318.51 * ratio
        return mixLayers(
            renderSequence(
                listOf(
                    VoiceNote(b5, b5, 52, WaveType.PULSE_25, 0.22),
                    VoiceNote(e6, e6, 138, WaveType.PULSE_25, 0.24, vibratoHz = 8.0, vibratoDepth = 0.006)
                )
            ),
            renderSequence(
                listOf(
                    VoiceNote(b5 * 0.5, b5 * 0.5, 52, WaveType.TRIANGLE, 0.16),
                    VoiceNote(e6 * 0.5, e6 * 0.5, 138, WaveType.TRIANGLE, 0.18)
                )
            )
        )
    }

    private fun renderSequence(notes: List<VoiceNote>): ShortArray {
        val totalSamples = notes.sumOf { (sampleRate * it.durationMs) / 1000 }
        val out = ShortArray(totalSamples)
        var cursor = 0
        var lfsr = 0x5A31
        for (note in notes) {
            val count = ((sampleRate * note.durationMs) / 1000).coerceAtLeast(1)
            val fadeSamples = min(140, count / 4).coerceAtLeast(1)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                var freq = note.startHz + (note.endHz - note.startHz) * t
                if (note.vibratoHz > 0.0 && note.vibratoDepth > 0.0) {
                    freq *= 1.0 + sin(2.0 * PI * note.vibratoHz * (i.toDouble() / sampleRate)) * note.vibratoDepth
                }
                phase = (phase + freq / sampleRate) % 1.0

                // Update retro LFSR noise register periodically
                if (i % 6 == 0) {
                    val bit = (lfsr xor (lfsr ushr 1)) and 1
                    lfsr = (lfsr ushr 1) or (bit shl 14)
                }
                val noise = if ((lfsr and 1) == 1) 0.85 else -0.85

                val tone = when (note.wave) {
                    WaveType.PULSE_12 -> if (phase < 0.125) 0.85 else -0.25
                    WaveType.PULSE_25 -> if (phase < 0.25) 0.82 else -0.34
                    WaveType.SQUARE_50 -> if (phase < 0.5) 0.72 else -0.72
                    WaveType.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
                    WaveType.NOISE -> noise
                }
                val mixedWave = if (note.noiseMix > 0.0 && note.wave != WaveType.NOISE) {
                    tone * (1.0 - note.noiseMix) + noise * note.noiseMix
                } else {
                    tone
                }

                val attack = if (i < fadeSamples) i.toDouble() / fadeSamples else 1.0
                val release = if (i > count - fadeSamples) (count - i).toDouble() / fadeSamples else 1.0
                val decay = (1.0 - t * 0.55).coerceIn(0.28, 1.0)
                val env = attack * release * decay
                val sample = (mixedWave * env * note.volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                if (cursor < out.size) {
                    out[cursor++] = sample.toShort()
                }
            }
        }
        return out
    }

    private fun mixLayers(vararg layers: ShortArray): ShortArray {
        val maxLen = layers.maxOfOrNull { it.size } ?: 0
        val out = ShortArray(maxLen)
        for (i in 0 until maxLen) {
            var sum = 0
            for (layer in layers) {
                if (i < layer.size) sum += layer[i].toInt()
            }
            out[i] = sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun buildGrooveLoop(
        stepMs: Int,
        melody: DoubleArray,
        bass: DoubleArray,
        masterVol: Double
    ): ShortArray {
        val steps = melody.size
        val stepSamples = (sampleRate * stepMs) / 1000
        val out = ShortArray(steps * stepSamples)
        var melPhase = 0.0
        var bassPhase = 0.0
        var lfsr = 0x3D7F

        for (step in 0 until steps) {
            val mFreq = melody[step]
            val bFreq = bass[step % bass.size]
            val isHatStep = step % 2 == 1
            val isKickStep = step % 4 == 0

            for (i in 0 until stepSamples) {
                val t = i.toDouble() / stepSamples
                var sample = 0.0

                if (mFreq > 1.0) {
                    melPhase = (melPhase + mFreq / sampleRate) % 1.0
                    val pulse = if (melPhase < 0.25) 0.65 else -0.25
                    val env = min(1.0, i / 90.0) * (1.0 - t * 0.68).coerceAtLeast(0.12)
                    sample += pulse * env * 0.52
                }

                if (bFreq > 1.0) {
                    bassPhase = (bassPhase + bFreq / sampleRate) % 1.0
                    val tri = 1.0 - 4.0 * abs(bassPhase - 0.5)
                    val env = min(1.0, i / 70.0) * (1.0 - t * 0.48).coerceAtLeast(0.20)
                    sample += tri * env * 0.48
                }

                if (isHatStep && i < stepSamples / 5) {
                    if (i % 4 == 0) {
                        val bit = (lfsr xor (lfsr ushr 1)) and 1
                        lfsr = (lfsr ushr 1) or (bit shl 14)
                    }
                    val hatEnv = 1.0 - (i.toDouble() / (stepSamples / 5))
                    val noise = if ((lfsr and 1) == 1) 1.0 else -1.0
                    sample += noise * hatEnv * 0.14
                }

                if (isKickStep && i < stepSamples / 4) {
                    val kt = i.toDouble() / (stepSamples / 4)
                    val kickFreq = 120.0 - 78.0 * kt
                    val kickEnv = 1.0 - kt
                    sample += sin(2.0 * PI * kickFreq * kt * 0.04) * kickEnv * 0.22
                }

                val scaled = (sample * masterVol * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[step * stepSamples + i] = scaled.toShort()
            }
        }
        return out
    }

    override fun release() {
        released.set(true)
        sfxPool.shutdownNow()
        musicThread.interrupt()
    }
}
