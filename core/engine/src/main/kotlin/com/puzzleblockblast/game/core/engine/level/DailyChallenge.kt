package com.puzzleblockblast.game.core.engine.level

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.random.Pcg32

/**
 * Builds the Daily Challenge for a date, offline and identically on every device: the date alone
 * seeds a [Pcg32] (own implementation, so the sequence never changes with platform or version).
 * The weekday picks a theme. Undo is off so every player's result is comparable.
 *
 * Generator parameters are checked by a bot test over many consecutive days (winnable, 3 stars
 * reachable). Changing anything here changes past and future dailies: bump [VERSION] and re-run it.
 */
object DailyChallenge {
    const val VERSION = 1
    private const val STREAM = 0xDA11L

    /** Level id for a day: `D` + epoch day. Never collides with pack ids (`L0001`). */
    fun idFor(epochDay: Long): String = "D$epochDay"

    fun epochDayOf(id: String): Long? = id.removePrefix("D").takeIf { id.startsWith("D") }?.toLongOrNull()

    fun levelFor(epochDay: Long): LevelDefinition {
        val rng = Pcg32.seeded(seedFor(epochDay), STREAM)
        // 1970-01-01 was a Thursday; 0 = Monday … 6 = Sunday.
        val weekday = Math.floorMod(epochDay + 3, 7L).toInt()
        val theme = THEMES[weekday]
        val board = layout(rng, theme)
        val level = LevelDefinition(
            id = idFor(epochDay),
            index = 1,
            board = board,
            seed = rng.nextInt().toLong() and 0xFFFF_FFFFL,
            pool = theme.pool,
            objectives = theme.objectives(board),
            maxMoves = theme.maxMoves,
            starMoves = listOf(theme.twoStar, theme.threeStar),
            undoAllowance = 0,
            hintAllowance = 1,
        )
        // Generated content goes through the same validation as bundled packs; never ship a bad day.
        return if (LevelValidator.validate(level).isEmpty()) level else fallback(epochDay)
    }

    /**
     * One weekday's recipe. Move numbers come from the bot gate (DailyChallengeTest): [threeStar] is at
     * least the bot's slowest fastest-win across the sampled weeks, and [maxMoves] leaves headroom.
     */
    private class Theme(
        val blocks: IntRange,
        val gems: IntRange,
        val pool: List<String>,
        val maxMoves: Int,
        val twoStar: Int,
        val threeStar: Int,
        val objectives: (Board) -> List<Objective>,
    )

    private val BASIC = listOf("dot", "i2", "i3", "i4", "sq2", "c3", "l4", "j4", "t4")
    private val FULL = BASIC + listOf("i5", "sq3", "s4", "z4", "c5", "r6")

    private fun gems(board: Board) = Objective.ClearGems(board.toTileArray().count { it == Board.GEM })

    private val THEMES = listOf(
        /* Mon */ Theme(8..12, 0..0, BASIC, 28, 25, 22) { listOf(Objective.ClearLines(5)) },
        /* Tue */ Theme(8..12, 3..4, BASIC, 24, 19, 15) { listOf(gems(it)) },
        /* Wed */ Theme(6..10, 0..0, BASIC, 26, 23, 20) { listOf(Objective.ScoreAtLeast(110)) },
        /* Thu */ Theme(10..14, 2..3, BASIC, 26, 21, 16) { listOf(gems(it), Objective.ClearLines(4)) },
        /* Fri */ Theme(8..12, 0..0, FULL, 28, 24, 20) { listOf(Objective.ClearLines(5)) },
        /* Sat */ Theme(10..14, 3..4, FULL, 30, 24, 20) { listOf(gems(it)) },
        /* Sun */ Theme(8..12, 2..3, FULL, 30, 26, 22) { listOf(gems(it), Objective.ScoreAtLeast(140)) },
    )

    private fun layout(rng: Pcg32, theme: Theme): Board {
        val size = Board.CLASSIC_SIZE
        val tiles = IntArray(size * size)
        val empty = (0 until size * size).toMutableList()
        fun take(): Int = empty.removeAt(rng.nextInt(empty.size))
        repeat(pick(rng, theme.blocks)) { tiles[take()] = rng.nextInt(7) + 1 }
        repeat(pick(rng, theme.gems)) { tiles[take()] = Board.GEM }
        // Random filler can't complete a line on a 10x10 board with ≤ 19 blocks, but stay safe.
        var board = Board.fromTiles(size, size, tiles)
        val full = Placement.findFullLines(board)
        if (!full.isEmpty) board = Placement.clear(board, full).first
        return board
    }

    private fun pick(rng: Pcg32, range: IntRange): Int =
        if (range.first == range.last) range.first else range.first + rng.nextInt(range.last - range.first + 1)

    private fun fallback(epochDay: Long) = LevelDefinition(
        id = idFor(epochDay),
        index = 1,
        board = Board.empty(),
        seed = epochDay,
        pool = BASIC,
        objectives = listOf(Objective.ClearLines(4)),
        maxMoves = 24,
        starMoves = listOf(19, 14),
        undoAllowance = 0,
        hintAllowance = 1,
    )

    /** SplitMix64 finaliser over the day and generator version: well spread, platform independent. */
    private fun seedFor(epochDay: Long): Long {
        var z = epochDay * 0x9E3779B97F4A7C15uL.toLong() + VERSION
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}
