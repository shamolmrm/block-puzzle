package com.puzzleblockblast.game.core.engine.classic

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.random.RngState
import com.puzzleblockblast.game.core.engine.shape.Shape

/** A dealt piece: a shape and the tile colour id it will paint on the board. */
data class Piece(val shape: Shape, val tile: Int)

/** Place the piece in tray slot [slot] with its top-left bounding corner at [origin]. */
data class Move(val slot: Int, val origin: GridPoint)

enum class GameStatus { PLAYING, GAME_OVER }

/**
 * Complete, immutable Classic game state. Everything needed to resume a game (including the RNG)
 * is here, so a saved state restores exactly.
 *
 * [previous] is the single-step undo snapshot; its own [previous] is always null.
 */
data class GameState(
    val seed: Long,
    val board: Board,
    val tray: List<Piece?>,
    val score: Int,
    val combo: Int,
    val moves: Int,
    val linesCleared: Int,
    val rng: RngState,
    val status: GameStatus,
    val undosRemaining: Int,
    val hintsRemaining: Int,
    val previous: GameState?,
) {
    val isGameOver: Boolean get() = status == GameStatus.GAME_OVER

    val canUndo: Boolean get() = status == GameStatus.PLAYING && previous != null && undosRemaining > 0
}
