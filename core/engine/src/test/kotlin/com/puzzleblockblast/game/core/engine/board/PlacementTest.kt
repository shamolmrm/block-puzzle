package com.puzzleblockblast.game.core.engine.board

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.boardOf
import com.puzzleblockblast.game.core.engine.shape.Shape
import org.junit.Test

class PlacementTest {

    private val dot = Shape.parse("dot", "X")
    private val i3 = Shape.parse("i3", "XXX")
    private val sq2 = Shape.parse("sq2", "XX", "XX")

    @Test
    fun canPlaceChecksBoundsAndOverlap() {
        val board = boardOf(
            "X...",
            "....",
            "....",
            "....",
        )

        assertThat(Placement.canPlace(board, i3, GridPoint(0, 1))).isTrue()
        assertThat(Placement.canPlace(board, i3, GridPoint(0, 0))).isFalse() // overlap
        assertThat(Placement.canPlace(board, i3, GridPoint(0, 2))).isFalse() // past right edge
        assertThat(Placement.canPlace(board, sq2, GridPoint(3, 0))).isFalse() // past bottom edge
        assertThat(Placement.canPlace(board, dot, GridPoint(-1, 0))).isFalse()
    }

    @Test
    fun validOriginsOnEmptyBoard() {
        val board = Board.empty()

        assertThat(Placement.validOrigins(board, dot)).hasSize(100)
        assertThat(Placement.validOrigins(board, i3)).hasSize(10 * 8)
        assertThat(Placement.validOrigins(board, sq2)).hasSize(9 * 9)
    }

    @Test
    fun fitsAnywhereFalseWhenNoRoom() {
        val board = boardOf(
            "X.X.",
            ".X.X",
            "X.X.",
            ".X.X",
        )

        assertThat(Placement.fitsAnywhere(board, dot)).isTrue()
        assertThat(Placement.fitsAnywhere(board, sq2)).isFalse()
        assertThat(Placement.fitsAnywhere(board, i3)).isFalse()
    }

    @Test
    fun findsFullRowsAndColumns() {
        val board = boardOf(
            "XXXX",
            "X..X",
            "X...",
            "X.X.",
        )

        val lines = Placement.findFullLines(board)

        assertThat(lines.rows).containsExactly(0)
        assertThat(lines.cols).containsExactly(0)
        assertThat(lines.count).isEqualTo(2)
    }

    @Test
    fun simultaneousRowAndColumnClearCountsIntersectionOnce() {
        val board = boardOf(
            "XXXX",
            "X.X.",
            "X..X",
            "X.X.",
        )
        val lines = Placement.findFullLines(board)

        val (cleared, cellCount) = Placement.clear(board, lines)

        assertThat(cellCount).isEqualTo(7) // 4 + 4 - 1 shared corner
        assertThat(cleared).isEqualTo(
            boardOf(
                "....",
                "..X.",
                "...X",
                "..X.",
            ),
        )
    }

    @Test
    fun multipleRowsClearTogether() {
        val board = boardOf(
            "XXX",
            "XXX",
            ".X.",
        )

        val lines = Placement.findFullLines(board)
        val (cleared, cellCount) = Placement.clear(board, lines)

        assertThat(lines.rows).containsExactly(0, 1)
        assertThat(lines.cols).containsExactly(1)
        assertThat(cellCount).isEqualTo(7)
        assertThat(cleared.isEmpty).isTrue()
    }

    @Test
    fun clearWithNoLinesIsNoOp() {
        val board = boardOf("X.", "..")

        assertThat(Placement.clear(board, FullLines.None)).isEqualTo(board to 0)
    }
}
