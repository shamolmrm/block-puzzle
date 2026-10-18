package com.puzzleblockblast.game.core.engine.shape

import com.puzzleblockblast.game.core.engine.board.GridPoint

/**
 * A fixed-orientation block shape. Cells are normalised so the minimum row and column are 0;
 * pieces are not player-rotatable, so each rotation is a separate catalogue entry.
 */
class Shape(val id: String, cells: Collection<GridPoint>) {
    val cells: List<GridPoint>
    val height: Int
    val width: Int

    init {
        require(id.isNotBlank()) { "Shape id must not be blank" }
        require(cells.isNotEmpty()) { "Shape '$id' has no cells" }
        require(cells.toSet().size == cells.size) { "Shape '$id' has duplicate cells" }
        val minRow = cells.minOf { it.row }
        val minCol = cells.minOf { it.col }
        this.cells = cells
            .map { GridPoint(it.row - minRow, it.col - minCol) }
            .sortedWith(compareBy({ it.row }, { it.col }))
        height = this.cells.maxOf { it.row } + 1
        width = this.cells.maxOf { it.col } + 1
    }

    val size: Int get() = cells.size

    /** Absolute board cells covered when the shape's top-left bounding corner sits at [origin]. */
    fun cellsAt(origin: GridPoint): List<GridPoint> = cells.map { it + origin }

    override fun equals(other: Any?): Boolean = other is Shape && id == other.id && cells == other.cells

    override fun hashCode(): Int = 31 * id.hashCode() + cells.hashCode()

    override fun toString(): String = "Shape($id, ${width}x$height, $size cells)"

    companion object {
        /**
         * Parses an ASCII pattern, e.g. `Shape.parse("L3", "X.", "XX")`. `X` marks a filled cell;
         * any other character is empty.
         */
        fun parse(id: String, vararg rows: String): Shape = Shape(
            id,
            rows.flatMapIndexed { r, line ->
                line.mapIndexedNotNull { c, ch -> if (ch == 'X') GridPoint(r, c) else null }
            },
        )
    }
}
