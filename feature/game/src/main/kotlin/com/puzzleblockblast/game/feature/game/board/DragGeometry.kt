package com.puzzleblockblast.game.feature.game.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.shape.Shape
import kotlin.math.roundToInt

/** Where the board is drawn, in root coordinates. */
@Immutable
data class BoardGeometry(val topLeft: Offset, val cellSize: Float) {
    fun cellTopLeft(point: GridPoint): Offset =
        Offset(topLeft.x + point.col * cellSize, topLeft.y + point.row * cellSize)
}

/** A legal drop under the finger: the cells it would fill and the lines it would clear. */
@Immutable
data class PlacementPreview(
    val slot: Int,
    val origin: GridPoint,
    val cells: List<GridPoint>,
    val tile: Int,
    val clearingRows: Set<Int>,
    val clearingCols: Set<Int>,
)

object DragGeometry {

    /**
     * Top-left of the dragged piece in root coordinates. The piece is drawn at board scale, centred
     * horizontally on the finger and lifted [liftPx] above it so the finger never hides it.
     */
    fun pieceTopLeft(pointer: Offset, shape: Shape, cellSize: Float, liftPx: Float): Offset =
        Offset(pointer.x - shape.width * cellSize / 2f, pointer.y - shape.height * cellSize - liftPx)

    /** The board cell the piece's top-left would snap to (may be out of bounds). */
    fun snapOrigin(pieceTopLeft: Offset, geometry: BoardGeometry): GridPoint = GridPoint(
        row = ((pieceTopLeft.y - geometry.topLeft.y) / geometry.cellSize).roundToInt(),
        col = ((pieceTopLeft.x - geometry.topLeft.x) / geometry.cellSize).roundToInt(),
    )

    /** The preview for dropping [piece] at [origin], or null if that drop is illegal. */
    fun preview(board: Board, slot: Int, piece: Piece, origin: GridPoint): PlacementPreview? {
        if (!Placement.canPlace(board, piece.shape, origin)) return null
        val lines = Placement.findFullLines(Placement.place(board, piece.shape, origin, piece.tile))
        return PlacementPreview(
            slot = slot,
            origin = origin,
            cells = piece.shape.cellsAt(origin),
            tile = piece.tile,
            clearingRows = lines.rows.toSet(),
            clearingCols = lines.cols.toSet(),
        )
    }
}
