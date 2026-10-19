package com.puzzleblockblast.game.core.engine.shape

import com.puzzleblockblast.game.core.engine.board.GridPoint

/** A shape plus its relative probability of being dealt. */
data class CatalogEntry(val shape: Shape, val weight: Int) {
    init {
        require(weight > 0) { "Weight must be positive" }
    }
}

class ShapeCatalog(val entries: List<CatalogEntry>) {
    private val byId: Map<String, Shape> = entries.associate { it.shape.id to it.shape }

    init {
        require(entries.isNotEmpty()) { "Catalog is empty" }
        require(byId.size == entries.size) { "Duplicate shape ids in catalog" }
    }

    val totalWeight: Int = entries.sumOf { it.weight }

    operator fun get(id: String): Shape? = byId[id]

    /**
     * A catalogue restricted to [ids], keeping each entry's weight. An id is either an exact shape id
     * (`l4_2`, one rotation) or a base id (`l4`, all of its rotations). Unknown ids are an error.
     */
    fun subset(ids: Collection<String>): ShapeCatalog {
        val picked = ids.flatMap { id ->
            val matches = entries.filter { it.shape.id == id || it.shape.id.substringBeforeLast('_') == id }
            require(matches.isNotEmpty()) { "Unknown shape id '$id'" }
            matches
        }
        return ShapeCatalog(picked.distinctBy { it.shape.id })
    }

    /** Whether [id] names a shape or a base shape in this catalogue. */
    fun knows(id: String): Boolean = entries.any { it.shape.id == id || it.shape.id.substringBeforeLast('_') == id }

    companion object {
        /**
         * The Classic catalogue. Each base pattern expands into its distinct 90° rotations, with ids
         * `<base>_<n>`. Weights are per rotation, so a shape with four rotations is dealt more often in
         * total than one with a single orientation. Ids are persisted in saved games: never rename one.
         */
        val Classic: ShapeCatalog = build(
            Base("dot", 2, "X"),
            Base("i2", 3, "XX"),
            Base("i3", 3, "XXX"),
            Base("i4", 2, "XXXX"),
            Base("i5", 1, "XXXXX"),
            Base("sq2", 4, "XX", "XX"),
            Base("sq3", 2, "XXX", "XXX", "XXX"),
            Base("c3", 2, "X.", "XX"),
            Base("l4", 1, "X.", "X.", "XX"),
            Base("j4", 1, ".X", ".X", "XX"),
            Base("t4", 1, "XXX", ".X."),
            Base("s4", 1, ".XX", "XX."),
            Base("z4", 1, "XX.", ".XX"),
            Base("c5", 1, "X..", "X..", "XXX"),
            Base("r6", 1, "XXX", "XXX"),
        )

        private class Base(val id: String, val weight: Int, vararg val rows: String)

        private fun build(vararg bases: Base): ShapeCatalog = ShapeCatalog(
            bases.flatMap { base ->
                distinctRotations(Shape.parse(base.id, *base.rows)).mapIndexed { i, cells ->
                    CatalogEntry(Shape("${base.id}_$i", cells), base.weight)
                }
            },
        )

        private fun distinctRotations(shape: Shape): List<List<GridPoint>> {
            val result = mutableListOf<List<GridPoint>>()
            var cells = shape.cells
            repeat(4) {
                val normalised = Shape("tmp", cells).cells
                if (normalised !in result) result += normalised
                cells = rotateClockwise(normalised)
            }
            return result
        }

        private fun rotateClockwise(cells: List<GridPoint>): List<GridPoint> {
            val maxRow = cells.maxOf { it.row }
            return cells.map { GridPoint(it.col, maxRow - it.row) }
        }
    }
}
