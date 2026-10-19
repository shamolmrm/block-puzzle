package com.puzzleblockblast.game.core.engine.random

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class Pcg32Test {

    @Test
    fun matchesReferenceImplementationOutput() {
        // First outputs of the reference pcg32-demo with pcg32_srandom_r(42, 54).
        val rng = Pcg32.seeded(seed = 42, stream = 54)
        val expected = listOf(0xa15c02b7, 0x7b47f409, 0xba1d3330, 0x83d2f293, 0xbfa4784b, 0xcbed606e)

        val actual = List(expected.size) { rng.nextInt().toLong() and 0xFFFF_FFFFL }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun sameSeedGivesSameSequence() {
        val a = Pcg32.seeded(123_456_789L)
        val b = Pcg32.seeded(123_456_789L)

        assertThat(List(100) { a.nextInt(1000) }).isEqualTo(List(100) { b.nextInt(1000) })
    }

    @Test
    fun restoreContinuesExactlyWhereSnapshotWasTaken() {
        val rng = Pcg32.seeded(7L)
        repeat(10) { rng.nextInt() }
        val restored = Pcg32.restore(rng.snapshot())

        assertThat(List(20) { restored.nextInt() }).isEqualTo(List(20) { rng.nextInt() })
    }

    @Test
    fun boundedValuesStayInRangeAndCoverIt() {
        val rng = Pcg32.seeded(99L)
        val counts = IntArray(7)
        repeat(70_000) { counts[rng.nextInt(7)]++ }

        // Each bucket expects 10,000; ±5% is far outside normal variance for a fair generator.
        counts.forEach { assertThat(it).isIn(com.google.common.collect.Range.closed(9_500, 10_500)) }
    }

    @Test
    fun rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException::class.java) { Pcg32.seeded(1L).nextInt(0) }
        assertThrows(IllegalArgumentException::class.java) { RngState(state = 1L, inc = 2L) }
    }
}
