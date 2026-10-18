package com.puzzleblockblast.game.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Original "dusk lagoon" brand palette: deep indigo base, coral and lagoon-teal accents.
internal val Indigo900 = Color(0xFF1B1840)
internal val Indigo800 = Color(0xFF262257)
internal val Indigo700 = Color(0xFF332E70)
internal val Indigo100 = Color(0xFFE4E2FA)
internal val Indigo50 = Color(0xFFF4F3FD)
internal val Coral500 = Color(0xFFFF6B5A)
internal val Coral200 = Color(0xFFFFB4A9)
internal val Lagoon500 = Color(0xFF15A3A6)
internal val Lagoon200 = Color(0xFF7FDCDB)
internal val Ink = Color(0xFF15132B)
internal val Snow = Color(0xFFFFFFFF)

/**
 * Colours used by the puzzle board. Tile ids from the engine (1-based) index into [tiles].
 * Every tile colour keeps at least 3:1 contrast against [boardBackground] (WCAG non-text contrast).
 */
@Immutable
data class BoardColors(
    val boardBackground: Color,
    val emptyCell: Color,
    val gridLine: Color,
    val ghostValid: Color,
    val ghostInvalid: Color,
    val tiles: List<Color>,
) {
    fun tile(tileId: Int): Color = tiles[(tileId - 1).mod(tiles.size)]
}

internal val DefaultBoardColors = BoardColors(
    boardBackground = Indigo900,
    emptyCell = Indigo800,
    gridLine = Indigo700,
    ghostValid = Color(0x8CFFFFFF),
    ghostInvalid = Color(0x8CFF5A5A),
    tiles = listOf(
        Color(0xFFFF6B5A), // coral
        Color(0xFFFFC145), // sunflower
        Color(0xFF5BD97A), // mint
        Color(0xFF3FC8F2), // sky
        Color(0xFF8C7BFF), // lavender
        Color(0xFFFF7FC8), // bubblegum
        Color(0xFFFF9F43), // tangerine
    ),
)

val LocalBoardColors = staticCompositionLocalOf { DefaultBoardColors }
