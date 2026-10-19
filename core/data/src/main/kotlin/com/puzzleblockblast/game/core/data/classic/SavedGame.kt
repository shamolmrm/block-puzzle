package com.puzzleblockblast.game.core.data.classic

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.random.RngState
import kotlinx.serialization.Serializable

/**
 * Persisted form of an interrupted Classic game. Kept separate from the engine's [GameState] so the
 * engine stays free of serialization concerns and the storage format can be versioned on its own.
 * Bump [SCHEMA_VERSION] on any incompatible change; older saves are then discarded safely.
 */
@Serializable
internal data class SavedGame(
    val schemaVersion: Int,
    val seed: Long,
    val rows: Int,
    val cols: Int,
    val tiles: List<Int>,
    val tray: List<SavedPiece?>,
    val score: Int,
    val combo: Int,
    val moves: Int,
    val linesCleared: Int,
    val rngState: Long,
    val rngInc: Long,
    val gameOver: Boolean,
    val undosRemaining: Int,
    val hintsRemaining: Int,
    val previous: SavedGame? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
internal data class SavedPiece(val shapeId: String, val tile: Int)

internal fun GameState.toSaved(): SavedGame = SavedGame(
    schemaVersion = SavedGame.SCHEMA_VERSION,
    seed = seed,
    rows = board.rows,
    cols = board.cols,
    tiles = board.toTileArray().toList(),
    tray = tray.map { it?.let { p -> SavedPiece(p.shape.id, p.tile) } },
    score = score,
    combo = combo,
    moves = moves,
    linesCleared = linesCleared,
    rngState = rng.state,
    rngInc = rng.inc,
    gameOver = isGameOver,
    undosRemaining = undosRemaining,
    hintsRemaining = hintsRemaining,
    previous = previous?.toSaved(),
)

/**
 * Converts back to a [GameState], or returns null if the save is from another schema version,
 * references unknown shapes, or describes a state the rules could never produce.
 */
internal fun SavedGame.toGameState(game: ClassicGame): GameState? {
    if (schemaVersion != SavedGame.SCHEMA_VERSION) return null
    val state = try {
        toGameStateUnchecked(game)
    } catch (_: IllegalArgumentException) {
        null
    } ?: return null
    return state.takeIf { game.isConsistent(it) }
}

private fun SavedGame.toGameStateUnchecked(game: ClassicGame): GameState? {
    val pieces = tray.map { saved ->
        if (saved == null) null else Piece(game.catalog[saved.shapeId] ?: return null, saved.tile)
    }
    val previousState = previous?.let { it.toGameStateUnchecked(game) ?: return null }
    return GameState(
        seed = seed,
        board = Board.fromTiles(rows, cols, tiles.toIntArray()),
        tray = pieces,
        score = score,
        combo = combo,
        moves = moves,
        linesCleared = linesCleared,
        rng = RngState(rngState, rngInc),
        status = if (gameOver) GameStatus.GAME_OVER else GameStatus.PLAYING,
        undosRemaining = undosRemaining,
        hintsRemaining = hintsRemaining,
        previous = previousState,
    )
}
