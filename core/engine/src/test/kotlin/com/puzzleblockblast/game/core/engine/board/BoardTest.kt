package com.puzzleblockblast.game.core.engine.board

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BoardTest {

    @Test
    fun emptyClassicBoardIs10x10AndEmpty() {
        val board = Board.empty()

        assertThat(board.rows).isEqualTo(10)
        assertThat(board.cols).isEqualTo(10)
        assertThat(board.cellCount).isEqualTo(100)
        assertThat(board.isEmpty).isTrue()
    }

    @Test
    fun withTilesReturnsNewBoardAndLeavesOriginalUnchanged() {
        val original = Board.empty()
        val updated = original.withTiles(listOf(GridPoint(0, 0), GridPoint(9, 9)), tile = 3)

        assertThat(original.isEmpty).isTrue()
        assertThat(updated[0, 0]).isEqualTo(3)
        assertThat(updated[9, 9]).isEqualTo(3)
        assertThat(updated.filledCount).isEqualTo(2)
    }

    @Test
    fun clearingWithEmptyTileRemovesCells() {
        val board = Board.empty().withTiles(listOf(GridPoint(4, 4)), tile = 1)

        assertThat(board.withTiles(listOf(GridPoint(4, 4)), Board.EMPTY).isEmpty).isTrue()
    }

    @Test
    fun boundsChecks() {
        val board = Board.empty()

        assertThat(board.inBounds(GridPoint(0, 0))).isTrue()
        assertThat(board.inBounds(GridPoint(9, 9))).isTrue()
        assertThat(board.inBounds(GridPoint(10, 0))).isFalse()
        assertThat(board.inBounds(GridPoint(0, -1))).isFalse()
        assertThrows(IllegalArgumentException::class.java) { board[10, 0] }
        assertThrows(IllegalArgumentException::class.java) {
            board.withTiles(listOf(GridPoint(-1, 0)), tile = 1)
        }
    }

    @Test
    fun equalityIsByContent() {
        val a = Board.empty().withTiles(listOf(GridPoint(2, 3)), tile = 1)
        val b = Board.fromTiles(10, 10, a.toTileArray())

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun rejectsInvalidDimensionsAndTiles() {
        assertThrows(IllegalArgumentException::class.java) { Board.empty(0) }
        assertThrows(IllegalArgumentException::class.java) { Board.fromTiles(2, 2, IntArray(3)) }
        assertThrows(IllegalArgumentException::class.java) { Board.fromTiles(1, 1, intArrayOf(-1)) }
    }
}
