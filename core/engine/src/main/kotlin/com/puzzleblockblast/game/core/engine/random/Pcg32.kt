package com.puzzleblockblast.game.core.engine.random

/** Serializable snapshot of a [Pcg32] generator. */
data class RngState(val state: Long, val inc: Long) {
    init {
        require(inc and 1L == 1L) { "PCG increment must be odd" }
    }
}

/**
 * PCG32 (XSH-RR) random generator, after O'Neill's reference `pcg32_random_r`.
 *
 * Unlike `kotlin.random.Random`, the algorithm is fixed and owned by this project, so a seed
 * produces the same piece sequence on every device and app version (needed for daily challenges,
 * replays and save/restore).
 */
class Pcg32 private constructor(private var state: Long, private val inc: Long) {

    fun snapshot(): RngState = RngState(state, inc)

    /** Next uniformly distributed 32-bit value (as a signed Int bit pattern). */
    fun nextInt(): Int {
        val old = state
        state = old * MULTIPLIER + inc
        val xorShifted = (((old ushr 18) xor old) ushr 27).toInt()
        val rot = (old ushr 59).toInt()
        return (xorShifted ushr rot) or (xorShifted shl ((-rot) and 31))
    }

    /** Unbiased value in `0 until bound`. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        val b = bound.toLong()
        // Reject the low (2^32 mod bound) values so every residue is equally likely.
        val threshold = (1L shl 32) % b
        while (true) {
            val r = nextInt().toLong() and 0xFFFF_FFFFL
            if (r >= threshold) return (r % b).toInt()
        }
    }

    companion object {
        private const val MULTIPLIER = 6364136223846793005L
        private const val DEFAULT_STREAM = 0x5EEDL

        /** Equivalent to the reference `pcg32_srandom_r(seed, stream)`. */
        fun seeded(seed: Long, stream: Long = DEFAULT_STREAM): Pcg32 {
            val rng = Pcg32(0L, (stream shl 1) or 1L)
            rng.nextInt()
            rng.state += seed
            rng.nextInt()
            return rng
        }

        fun restore(snapshot: RngState): Pcg32 = Pcg32(snapshot.state, snapshot.inc)
    }
}
