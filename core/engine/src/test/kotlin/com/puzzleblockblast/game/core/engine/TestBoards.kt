package com.puzzleblockblast.game.core.engine

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.random.Pcg32
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog

/** Builds a board from rows of `.` (empty), `G` (gem) and `X` (tile 1). */
fun boardOf(vararg rows: String): Board {
    val tiles = rows.flatMap { row ->
        row.map {
            when (it) {
                '.' -> Board.EMPTY
                'G' -> Board.GEM
                else -> 1
            }
        }
    }.toIntArray()
    return Board.fromTiles(rows.size, rows.first().length, tiles)
}

fun piece(id: String, tile: Int = 2) = Piece(requireNotNull(ShapeCatalog.Classic[id]) { "unknown $id" }, tile)

/** A hand-built mid-game state for focused rule tests. */
fun stateOf(board: Board, vararg tray: Piece?, combo: Int = 0, undos: Int = 1, hints: Int = 3) = GameState(
    seed = 1L,
    board = board,
    tray = tray.toList(),
    score = 0,
    combo = combo,
    moves = 0,
    linesCleared = 0,
    rng = Pcg32.seeded(1L).snapshot(),
    status = GameStatus.PLAYING,
    undosRemaining = undos,
    hintsRemaining = hints,
    previous = null,
)
