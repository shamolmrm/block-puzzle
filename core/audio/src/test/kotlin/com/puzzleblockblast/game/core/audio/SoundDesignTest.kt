package com.puzzleblockblast.game.core.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SoundDesignTest {

    @Test
    fun everySoundRendersShortNonSilentAudio() {
        Sound.entries.forEach { sound ->
            val samples = SoundDesign.render(sound)
            val seconds = samples.size.toDouble() / SoundDesign.SAMPLE_RATE

            assertThat(seconds).isAtMost(1.0)
            assertThat(samples.maxOf { it.toInt() }).isGreaterThan(Short.MAX_VALUE / 10)
        }
    }

    @Test
    fun renderingIsDeterministic() {
        assertThat(SoundDesign.render(Sound.CLEAR).toList()).isEqualTo(SoundDesign.render(Sound.CLEAR).toList())
    }

    @Test
    fun startsAndEndsWithoutAClick() {
        val samples = SoundDesign.render(Sound.PLACE)

        assertThat(kotlin.math.abs(samples.first().toInt())).isLessThan(200)
        assertThat(kotlin.math.abs(samples.last().toInt())).isLessThan(Short.MAX_VALUE / 20)
    }

    @Test
    fun wavHeaderIsValidPcm16Mono() {
        val samples = shortArrayOf(0, 1000, -1000)
        val wav = ByteBuffer.wrap(Wav.encode(samples, 22_050)).order(ByteOrder.LITTLE_ENDIAN)

        assertThat(String(wav.array(), 0, 4)).isEqualTo("RIFF")
        assertThat(wav.getInt(4)).isEqualTo(36 + 6)
        assertThat(String(wav.array(), 8, 4)).isEqualTo("WAVE")
        assertThat(wav.getShort(20).toInt()).isEqualTo(1) // PCM
        assertThat(wav.getShort(22).toInt()).isEqualTo(1) // mono
        assertThat(wav.getInt(24)).isEqualTo(22_050)
        assertThat(wav.getShort(34).toInt()).isEqualTo(16)
        assertThat(wav.getInt(40)).isEqualTo(6)
        assertThat(wav.getShort(46).toInt()).isEqualTo(1000) // samples start at byte 44
        assertThat(wav.getShort(48).toInt()).isEqualTo(-1000)
    }
}
