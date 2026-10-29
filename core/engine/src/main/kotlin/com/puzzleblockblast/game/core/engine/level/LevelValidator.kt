package com.puzzleblockblast.game.core.engine.level

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog

/**
 * Structural checks for any level, bundled or (later) downloaded. Returns the problems found; an
 * empty list means the level is safe to load. Winnability is checked separately by [LevelBot].
 */
object LevelValidator {
    /** Pack levels `L0001`; generated dailies `D` + epoch day; event levels `E01L03`. */
    private val ID_PATTERN = Regex("L\\d{4}|D-?\\d{1,9}|E\\d{2}L\\d{2}")
    const val MIN_BOARD = 8
    const val MAX_MOVES = 200
    const val MAX_ALLOWANCE = 3

    fun validate(level: LevelDefinition, catalog: ShapeCatalog = ShapeCatalog.Classic): List<String> = buildList {
        if (!ID_PATTERN.matches(level.id)) add("id '${level.id}' must look like L0001 (or D<day> for a daily)")
        if (level.index < 1) add("index must be >= 1")
        val board = level.board
        if (board.rows != board.cols || board.rows !in MIN_BOARD..Board.CLASSIC_SIZE) {
            add("board must be square, $MIN_BOARD..${Board.CLASSIC_SIZE}")
        }
        if (!Placement.findFullLines(board).isEmpty) add("starting board already has a full line")
        if (level.pool.isEmpty()) add("piece pool is empty")
        level.pool.filterNot { catalog.knows(it) }.forEach { add("unknown shape '$it'") }
        if (level.objectives.isEmpty()) add("no objectives")
        level.objectives.forEach { o ->
            if (o.target <= 0) add("objective $o needs a positive target")
            if (o is Objective.ClearGems && o.target != level.gemCount) {
                add("gem objective ${o.target} != ${level.gemCount} gems on the board")
            }
        }
        if (level.objectives.groupBy { it::class }.any { it.value.size > 1 }) add("duplicate objective type")
        if (level.maxMoves !in 1..MAX_MOVES) add("maxMoves must be 1..$MAX_MOVES")
        val stars = level.starMoves
        if (stars.size != 2 || stars[1] < 1 || stars[1] > stars[0] || stars[0] > level.maxMoves) {
            add("starMoves must be [2-star, 3-star] with 1 <= 3-star <= 2-star <= maxMoves")
        }
        if (level.undoAllowance !in 0..MAX_ALLOWANCE) add("undoAllowance must be 0..$MAX_ALLOWANCE")
        if (level.hintAllowance !in 0..MAX_ALLOWANCE) add("hintAllowance must be 0..$MAX_ALLOWANCE")
        if (isEmpty() && level.pool.none { id -> catalog.subset(listOf(id)).entries.any { Placement.fitsAnywhere(board, it.shape) } }) {
            add("no pool shape fits the starting board")
        }
    }

    /** Pack-level checks: unique ids and indexes 1..n in order. */
    fun validatePack(levels: List<LevelDefinition>): List<String> = buildList {
        levels.groupBy { it.id }.filter { it.value.size > 1 }.keys.forEach { add("duplicate id $it") }
        if (levels.map { it.index } != (1..levels.size).toList()) add("indexes must run 1..${levels.size} in order")
        levels.forEach { level -> validate(level).forEach { add("${level.id}: $it") } }
    }
}
