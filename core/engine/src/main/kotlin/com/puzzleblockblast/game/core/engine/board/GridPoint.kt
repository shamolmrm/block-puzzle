package com.puzzleblockblast.game.core.engine.board

/** A cell coordinate. `row` grows downwards, `col` grows to the right; (0, 0) is the top-left cell. */
data class GridPoint(val row: Int, val col: Int) {
    operator fun plus(other: GridPoint): GridPoint = GridPoint(row + other.row, col + other.col)
}
