package com.puzzleblockblast.game.feature.game.board

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog
import org.junit.Test

class DragGeometryTest {

    private val geometry = BoardGeometry(topLeft = Offset(100f, 200f), cellSize = 50f)
    private val sq2 = Piece(ShapeCatalog.Classic["sq2_0"]!!, 3)
    private val i3 = Piece(ShapeCatalog.Classic["i3_0"]!!, 3)

    @Test
    fun pieceIsCentredOnFingerAndLiftedAboveIt() {
        val topLeft = DragGeometry.pieceTopLeft(Offset(300f, 600f), sq2.shape, cellSize = 50f, liftPx = 40f)

        assertThat(topLeft).isEqualTo(Offset(300f - 50f, 600f - 100f - 40f))
    }

    @Test
    fun snapsToNearestCell() {
        assertThat(DragGeometry.snapOrigin(Offset(100f, 200f), geometry)).isEqualTo(GridPoint(0, 0))
        assertThat(DragGeometry.snapOrigin(Offset(124f, 226f), geometry)).isEqualTo(GridPoint(1, 0))
        assertThat(DragGeometry.snapOrigin(Offset(276f, 349f), geometry)).isEqualTo(GridPoint(3, 4))
        assertThat(DragGeometry.snapOrigin(Offset(40f, 120f), geometry)).isEqualTo(GridPoint(-2, -1))
    }

    @Test
    fun previewIsNullForIllegalDrop() {
        assertThat(DragGeometry.preview(Board.empty(), 0, sq2, GridPoint(9, 9))).isNull()
        assertThat(DragGeometry.preview(Board.empty(), 0, sq2, GridPoint(-1, 0))).isNull()
    }

    @Test
    fun previewReportsCellsAndLinesThatWouldClear() {
        val tiles = IntArray(100).also { t -> (3 until 10).forEach { c -> t[c] = 1 } } // row 0: cols 3..9 filled
        val board = Board.fromTiles(10, 10, tiles)

        val preview = requireNotNull(DragGeometry.preview(board, 1, i3, GridPoint(0, 0)))

        assertThat(preview.cells).containsExactly(GridPoint(0, 0), GridPoint(0, 1), GridPoint(0, 2))
        assertThat(preview.clearingRows).containsExactly(0)
        assertThat(preview.clearingCols).isEmpty()
        assertThat(preview.slot).isEqualTo(1)
    }
}
