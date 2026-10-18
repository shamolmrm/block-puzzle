package com.puzzleblockblast.game.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.feature.game.R

/**
 * Draws the board in a single Canvas pass. Drag-and-drop, ghost preview and clear animations are
 * layered onto this in Phase 2/3.
 */
@Composable
fun BoardCanvas(board: Board, modifier: Modifier = Modifier) {
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
            .semantics { contentDescription = description },
    ) {
        val cell = minOf(size.width / board.cols, size.height / board.rows)
        val gap = cell * 0.06f
        val radius = CornerRadius(cell * 0.18f)

        drawRoundRect(colors.boardBackground, cornerRadius = CornerRadius(cell * 0.3f))
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val tile = board[r, c]
                drawRoundRect(
                    color = if (tile == Board.EMPTY) colors.emptyCell else colors.tile(tile),
                    topLeft = Offset(c * cell + gap, r * cell + gap),
                    size = Size(cell - 2 * gap, cell - 2 * gap),
                    cornerRadius = radius,
                )
            }
        }
    }
}
