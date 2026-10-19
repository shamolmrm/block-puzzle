package com.puzzleblockblast.game.core.designsystem.component

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp

/**
 * Draws one glossy block: a darker base lip for depth, the coloured face, and a soft highlight.
 * [size] is the block's edge length; the caller leaves any gap between blocks.
 */
fun DrawScope.drawBlock(topLeft: Offset, size: Float, color: Color, alpha: Float = 1f) {
    val corner = CornerRadius(size * 0.2f)
    drawRoundRect(lerp(color, Color.Black, 0.3f), topLeft, Size(size, size), corner, alpha = alpha)
    drawRoundRect(color, topLeft, Size(size, size * 0.88f), corner, alpha = alpha)
    drawRoundRect(
        color = lerp(color, Color.White, 0.45f),
        topLeft = topLeft + Offset(size * 0.16f, size * 0.1f),
        size = Size(size * 0.68f, size * 0.16f),
        cornerRadius = CornerRadius(size * 0.08f),
        alpha = alpha * 0.75f,
    )
}

/** Draws [cells] (row, col pairs) as blocks of [cellSize] with a small gap, starting at [origin]. */
fun DrawScope.drawBlocks(
    cells: List<Pair<Int, Int>>,
    color: Color,
    cellSize: Float,
    origin: Offset = Offset.Zero,
    alpha: Float = 1f,
) {
    val gap = cellSize * 0.06f
    cells.forEach { (row, col) ->
        drawBlock(
            topLeft = Offset(origin.x + col * cellSize + gap, origin.y + row * cellSize + gap),
            size = cellSize - 2 * gap,
            color = color,
            alpha = alpha,
        )
    }
}
