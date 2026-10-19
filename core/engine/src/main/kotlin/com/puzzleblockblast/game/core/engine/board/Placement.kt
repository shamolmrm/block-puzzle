package com.puzzleblockblast.game.core.engine.board

import com.puzzleblockblast.game.core.engine.shape.Shape

/** Rows and columns that are completely filled. */
data class FullLines(val rows: List<Int>, val cols: List<Int>) {
    val count: Int get() = rows.size + cols.size
    val isEmpty: Boolean get() = count == 0

    fun cells(board: Board): Set<GridPoint> = buildSet {
        rows.forEach { r -> (0 until board.cols).forEach { c -> add(GridPoint(r, c)) } }
        cols.forEach { c -> (0 until board.rows).forEach { r -> add(GridPoint(r, c)) } }
    }

    companion object {
        val None = FullLines(emptyList(), emptyList())
    }
}

/** Pure placement and line-clear rules shared by every game mode. */
object Placement {

    fun canPlace(board: Board, shape: Shape, origin: GridPoint): Boolean =
        shape.cells.all { offset ->
            val p = offset + origin
            board.inBounds(p) && !board.isOccupied(p)
        }

    /** Every top-left origin where [shape] fits, in row-major order. */
    fun validOrigins(board: Board, shape: Shape): List<GridPoint> = buildList {
        for (r in 0..board.rows - shape.height) {
            for (c in 0..board.cols - shape.width) {
                val origin = GridPoint(r, c)
                if (canPlace(board, shape, origin)) add(origin)
            }
        }
    }

    fun fitsAnywhere(board: Board, shape: Shape): Boolean {
        for (r in 0..board.rows - shape.height) {
            for (c in 0..board.cols - shape.width) {
                if (canPlace(board, shape, GridPoint(r, c))) return true
            }
        }
        return false
    }

    /** Places [shape] without checking legality; callers must check [canPlace] first. */
    fun place(board: Board, shape: Shape, origin: GridPoint, tile: Int): Board =
        board.withTiles(shape.cellsAt(origin), tile)

    fun findFullLines(board: Board): FullLines = FullLines(
        rows = (0 until board.rows).filter { r -> (0 until board.cols).all { c -> board[r, c] != Board.EMPTY } },
        cols = (0 until board.cols).filter { c -> (0 until board.rows).all { r -> board[r, c] != Board.EMPTY } },
    )

    /**
     * Clears all [lines] simultaneously. A cell at a row/column intersection is cleared once.
     * Returns the new board and the number of distinct cells cleared.
     */
    fun clear(board: Board, lines: FullLines): Pair<Board, Int> {
        if (lines.isEmpty) return board to 0
        val cells = lines.cells(board)
        return board.withTiles(cells, Board.EMPTY) to cells.size
    }
}
