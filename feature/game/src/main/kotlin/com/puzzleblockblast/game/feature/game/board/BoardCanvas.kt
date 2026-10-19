package com.puzzleblockblast.game.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.puzzleblockblast.game.core.designsystem.component.drawBlock
import com.puzzleblockblast.game.core.designsystem.component.drawBlocks
import com.puzzleblockblast.game.core.designsystem.theme.BoardColors
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.feature.game.R

/** Cells removed by the last move, animated as a burst while [progress] runs from 0 to 1. */
data class ClearBurst(val cells: List<GridPoint>, val color: Color, val progress: Float)

/**
 * Draws the board in a single Canvas pass: tiles, the drop preview (ghost piece plus the lines it
 * would clear, tinted in the piece's colour), the hint outline and the line-clear burst. Reports its
 * on-screen geometry for drag-and-drop.
 */
@Composable
fun BoardCanvas(
    board: Board,
    modifier: Modifier = Modifier,
    preview: PlacementPreview? = null,
    hintCells: List<GridPoint> = emptyList(),
    burst: ClearBurst? = null,
    onGeometry: (BoardGeometry) -> Unit = {},
) {
    val colors = PbbTheme.boardColors
    val description = pluralStringResource(
        R.plurals.game_board_description,
        board.filledCount,
        board.rows,
        board.cols,
        board.filledCount,
    )
    Canvas(
        modifier = modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .onGloballyPositioned { coords ->
                onGeometry(BoardGeometry(coords.positionInRoot(), coords.size.width.toFloat() / board.cols))
            }
            .semantics { contentDescription = description },
    ) {
        val cell = size.width / board.cols
        drawRoundRect(colors.boardBackground, cornerRadius = CornerRadius(cell * 0.35f))
        drawRoundRect(
            colors.gridLine,
            cornerRadius = CornerRadius(cell * 0.35f),
            style = Stroke(width = cell * 0.05f),
        )

        val previewCells = preview?.cells?.toSet().orEmpty()
        val previewColor = preview?.let { colors.tile(it.tile) }
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val tile = board[r, c]
                val point = GridPoint(r, c)
                val clearing = preview != null && (r in preview.clearingRows || c in preview.clearingCols)
                when {
                    point in previewCells -> drawCellBlock(r, c, cell, previewColor!!, if (clearing) 1f else GHOST_ALPHA)
                    tile == Board.EMPTY -> drawEmptyCell(r, c, cell, colors.emptyCell)
                    // Filled cells in a line about to clear take the dragged piece's colour.
                    clearing -> drawCellBlock(r, c, cell, previewColor!!)
                    else -> drawCellBlock(r, c, cell, colors.tile(tile))
                }
            }
        }
        hintCells.forEach { drawHintOutline(it, cell, colors) }
        if (burst != null) drawBurst(burst, cell)
    }
}

private const val GHOST_ALPHA = 0.5f

private fun DrawScope.drawCellBlock(row: Int, col: Int, cell: Float, color: Color, alpha: Float = 1f) {
    val gap = cell * 0.06f
    drawBlock(Offset(col * cell + gap, row * cell + gap), cell - 2 * gap, color, alpha)
}

private fun DrawScope.drawEmptyCell(row: Int, col: Int, cell: Float, color: Color) {
    val gap = cell * 0.08f
    drawRoundRect(
        color = color,
        topLeft = Offset(col * cell + gap, row * cell + gap),
        size = Size(cell - 2 * gap, cell - 2 * gap),
        cornerRadius = CornerRadius(cell * 0.18f),
    )
}

/** Cleared blocks flash white, then shrink and fade away. */
private fun DrawScope.drawBurst(burst: ClearBurst, cell: Float) {
    val t = burst.progress
    val scale = 1f - t
    val flash = Color.White.copy(alpha = (1f - t) * 0.9f)
    burst.cells.forEach { p ->
        val edge = (cell * 0.88f) * scale
        val topLeft = Offset(p.col * cell + (cell - edge) / 2f, p.row * cell + (cell - edge) / 2f)
        if (edge > 0f) {
            drawBlock(topLeft, edge, burst.color, alpha = 1f - t)
            drawRoundRect(flash, topLeft, Size(edge, edge), CornerRadius(edge * 0.2f))
        }
    }
}

private fun DrawScope.drawHintOutline(point: GridPoint, cell: Float, colors: BoardColors) {
    val inset = cell * 0.1f
    drawRoundRect(
        color = colors.ghostValid.copy(alpha = 1f),
        topLeft = Offset(point.col * cell + inset, point.row * cell + inset),
        size = Size(cell - 2 * inset, cell - 2 * inset),
        cornerRadius = CornerRadius(cell * 0.16f),
        style = Stroke(width = cell * 0.08f),
    )
}

/** Draws a piece's [cells] at [cellSize] with its top-left at [origin]. Shared by the tray and the drag overlay. */
internal fun DrawScope.drawPiece(
    cells: List<GridPoint>,
    tileColor: Color,
    cellSize: Float,
    origin: Offset = Offset.Zero,
) = drawBlocks(cells.map { it.row to it.col }, tileColor, cellSize, origin)
