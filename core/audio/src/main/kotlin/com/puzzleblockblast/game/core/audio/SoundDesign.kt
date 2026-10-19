package com.puzzleblockblast.game.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** One plucked note: starts at [start] seconds and rings for [length] seconds. */
internal data class Tone(val freq: Double, val start: Double, val length: Double, val gain: Double = 0.5)

/** The game's original sound palette: soft mallet-like plucks built from a few sine partials. */
internal object SoundDesign {
    const val SAMPLE_RATE = 22_050

    private const val C4 = 261.63
    private const val E4 = 329.63
    private const val G4 = 392.00
    private const val C5 = 523.25
    private const val E5 = 659.25
    private const val G5 = 783.99
    private const val B5 = 987.77
    private const val C6 = 1046.50
    private const val E6 = 1318.51
    private const val G6 = 1567.98

    fun tones(sound: Sound): List<Tone> = when (sound) {
        Sound.PLACE -> listOf(Tone(G4, 0.0, 0.09, 0.45), Tone(G5, 0.0, 0.05, 0.15))
        Sound.CLEAR -> arpeggio(0.06, 0.24, C5, E5, G5)
        Sound.CLEAR_MULTI -> arpeggio(0.05, 0.26, C5, E5, G5, C6)
        Sound.COMBO -> arpeggio(0.04, 0.22, E5, G5, B5, E6)
        Sound.BOARD_CLEAR -> arpeggio(0.05, 0.32, C5, E5, G5, C6, E6, G6)
        Sound.GAME_OVER -> arpeggio(0.18, 0.36, G4, E4, C4)
    }

    fun render(sound: Sound): ShortArray = synth(tones(sound))

    private fun arpeggio(step: Double, length: Double, vararg freqs: Double) =
        freqs.mapIndexed { i, f -> Tone(f, i * step, length) }

    fun synth(tones: List<Tone>, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val total = tones.maxOf { it.start + it.length }
        val mix = DoubleArray((total * sampleRate).toInt() + 1)
        for (tone in tones) {
            val first = (tone.start * sampleRate).toInt()
            val count = (tone.length * sampleRate).toInt()
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                // 4 ms attack avoids clicks; exponential decay gives the pluck.
                val envelope = min(1.0, t / 0.004) * exp(-5.0 * t / tone.length)
                val phase = 2 * PI * tone.freq * t
                val wave = (sin(phase) + 0.25 * sin(2 * phase) + 0.1 * sin(3 * phase)) / 1.35
                mix[first + i] += tone.gain * envelope * wave
            }
        }
        return ShortArray(mix.size) { (mix[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.8).toInt().toShort() }
    }
}

/** Minimal PCM 16-bit mono WAV encoder. */
internal object Wav {
    fun encode(samples: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = samples.size * 2
        val out = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        out.put("data".toByteArray()).putInt(dataSize)
        samples.forEach { out.putShort(it) }
        return out.array()
    }
}
