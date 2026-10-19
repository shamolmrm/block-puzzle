package com.puzzleblockblast.game.core.engine.level

import com.puzzleblockblast.game.core.engine.board.Board

/** A level goal. Progress is measured from the current game; a level is won when every goal is met. */
sealed interface Objective {
    val target: Int

    /** Reach at least [value] points. */
    data class ScoreAtLeast(val value: Int) : Objective {
        override val target: Int get() = value
    }

    /** Clear at least [count] lines (rows and columns count separately). */
    data class ClearLines(val count: Int) : Objective {
        override val target: Int get() = count
    }

    /** Clear every gem pre-placed on the board. The target is the number of gems at the start. */
    data class ClearGems(override val target: Int) : Objective
}

/**
 * One level, as loaded from a content pack. Progress is keyed by [id], never by [index], so packs
 * can be re-ordered or rebalanced without losing a player's stars.
 *
 * @param pool shape ids or base ids the level deals from (see `ShapeCatalog.subset`)
 * @param starMoves most moves allowed for 2 and 3 stars; completing the objectives gives 1 star.
 *   Stars use moves, not score, because a level ends the moment its objectives are met.
 */
data class LevelDefinition(
    val id: String,
    val index: Int,
    val board: Board,
    val seed: Long,
    val pool: List<String>,
    val objectives: List<Objective>,
    val maxMoves: Int,
    val starMoves: List<Int>,
    val undoAllowance: Int,
    val hintAllowance: Int,
) {
    val gemCount: Int get() = board.toTileArray().count { it == Board.GEM }
}
