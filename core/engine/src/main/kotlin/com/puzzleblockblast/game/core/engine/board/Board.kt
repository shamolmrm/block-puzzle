package com.puzzleblockblast.game.core.engine.board

/**
 * Immutable square-or-rectangular puzzle grid.
 *
 * Each cell holds a tile value: [EMPTY] or a positive tile id (colour / special tile type).
 * Every mutating operation returns a new [Board]; instances are safe to share across threads and
 * to keep as undo snapshots.
 */
class Board private constructor(
    val rows: Int,
    val cols: Int,
    private val cells: IntArray,
) {
    init {
        require(rows in 1..MAX_SIZE && cols in 1..MAX_SIZE) { "Board size $rows x $cols out of range" }
        require(cells.size == rows * cols) { "Cell array size ${cells.size} != $rows x $cols" }
        require(cells.all { it >= EMPTY }) { "Tile ids must be >= 0" }
    }

    val cellCount: Int get() = cells.size

    val filledCount: Int get() = cells.count { it != EMPTY }

    val isEmpty: Boolean get() = filledCount == 0

    fun inBounds(point: GridPoint): Boolean =
        point.row in 0 until rows && point.col in 0 until cols

    operator fun get(point: GridPoint): Int = get(point.row, point.col)

    operator fun get(row: Int, col: Int): Int {
        require(row in 0 until rows && col in 0 until cols) { "($row, $col) outside $rows x $cols board" }
        return cells[row * cols + col]
    }

    fun isOccupied(point: GridPoint): Boolean = get(point) != EMPTY

    /** Returns a copy with [points] set to [tile]. Use [EMPTY] to clear cells. */
    fun withTiles(points: Iterable<GridPoint>, tile: Int): Board {
        require(tile >= EMPTY) { "Tile id must be >= 0" }
        val copy = cells.copyOf()
        for (p in points) {
            require(inBounds(p)) { "$p outside $rows x $cols board" }
            copy[p.row * cols + p.col] = tile
        }
        return Board(rows, cols, copy)
    }

    /** Row-major snapshot of tile values, for persistence and rendering. */
    fun toTileArray(): IntArray = cells.copyOf()

    override fun equals(other: Any?): Boolean =
        other is Board && rows == other.rows && cols == other.cols && cells.contentEquals(other.cells)

    override fun hashCode(): Int = 31 * (31 * rows + cols) + cells.contentHashCode()

    override fun toString(): String = buildString {
        for (r in 0 until rows) {
            for (c in 0 until cols) append(if (cells[r * cols + c] == EMPTY) '.' else 'X')
            if (r < rows - 1) append('\n')
        }
    }

    companion object {
        const val EMPTY = 0
        const val CLASSIC_SIZE = 10
        const val MAX_SIZE = 12

        fun empty(rows: Int = CLASSIC_SIZE, cols: Int = rows): Board = Board(rows, cols, IntArray(rows * cols))

        fun fromTiles(rows: Int, cols: Int, tiles: IntArray): Board = Board(rows, cols, tiles.copyOf())
    }
}
