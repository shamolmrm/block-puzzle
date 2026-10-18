package com.puzzleblockblast.game.core.engine.shape

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.GridPoint
import org.junit.Assert.assertThrows
import org.junit.Test

class ShapeTest {

    @Test
    fun parseBuildsNormalisedCells() {
        val shape = Shape.parse("L3", "X.", "XX")

        assertThat(shape.cells).containsExactly(GridPoint(0, 0), GridPoint(1, 0), GridPoint(1, 1)).inOrder()
        assertThat(shape.width).isEqualTo(2)
        assertThat(shape.height).isEqualTo(2)
        assertThat(shape.size).isEqualTo(3)
    }

    @Test
    fun cellsAreNormalisedToTopLeft() {
        val shape = Shape("I2", listOf(GridPoint(5, 7), GridPoint(5, 8)))

        assertThat(shape.cells).containsExactly(GridPoint(0, 0), GridPoint(0, 1)).inOrder()
    }

    @Test
    fun cellsAtOffsetsByOrigin() {
        val shape = Shape.parse("I3", "XXX")

        assertThat(shape.cellsAt(GridPoint(2, 4)))
            .containsExactly(GridPoint(2, 4), GridPoint(2, 5), GridPoint(2, 6)).inOrder()
    }

    @Test
    fun rejectsEmptyOrDuplicateCells() {
        assertThrows(IllegalArgumentException::class.java) { Shape("empty", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            Shape("dup", listOf(GridPoint(0, 0), GridPoint(0, 0)))
        }
    }
}
