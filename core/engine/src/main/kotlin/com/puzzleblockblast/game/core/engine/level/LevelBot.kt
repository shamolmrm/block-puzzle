package com.puzzleblockblast.game.core.engine.level

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.Piece

/**
 * Beam-search player used to validate content: proves a level can be won and how quickly.
 * Deterministic. It sees only what the player sees (the board and current tray), never future deals.
 */
object LevelBot {

    data class Result(
        val won: Boolean,
        /** Fewest moves in any win found (0 if none). Proves 3 stars at this move count is reachable. */
        val fewestMoves: Int,
        /** Stars for that fastest win. */
        val stars: Int,
    )

    fun play(game: LevelGame, beamWidth: Int = 8): Result {
        var beam = listOf(game.start())
        var fastest: LevelState? = null

        // Breadth-first by move count, so the first depth with a win is the fewest moves found.
        while (beam.isNotEmpty() && fastest == null) {
            val children = HashMap<Any, LevelState>()
            for (state in beam) {
                for ((slot, piece) in state.game.tray.withIndex()) {
                    if (piece == null) continue
                    for (origin in Placement.validOrigins(state.game.board, piece.shape)) {
                        val next = (game.place(state, Move(slot, origin)) as? LevelMoveResult.Accepted)?.state ?: continue
                        when (next.result) {
                            LevelResult.WON -> if (fastest == null) fastest = next
                            LevelResult.IN_PROGRESS -> {
                                // Same board, tray and combo: keep only the higher score.
                                val key = Triple(next.game.board, next.game.tray, next.game.combo)
                                val seen = children[key]
                                if (seen == null || next.game.score > seen.game.score) children[key] = next
                            }
                            else -> Unit
                        }
                    }
                }
            }
            beam = children.values.sortedByDescending { value(game, it) }.take(beamWidth)
        }

        val win = fastest
        return Result(won = win != null, fewestMoves = win?.game?.moves ?: 0, stars = win?.let { game.stars(it) } ?: 0)
    }

    /**
     * Heuristic: objective progress dominates, then building towards gems and lines, then score and
     * keeping the board open.
     */
    private fun value(game: LevelGame, state: LevelState): Double {
        val progress = game.progress(state.game).map { minOf(1.0, it.current.toDouble() / it.objective.target) }.average()
        val board = state.game.board
        return progress * 1_000 + gemPotential(board) * 40 + linePotential(board) * 60 + state.game.score / 10.0 -
            isolatedEmptyCells(board) * 3 - unplayable(board, state.game.tray) * 25
    }

    /** Mean squared fill of every row and column (0..1): favours concentrating blocks into lines. */
    private fun linePotential(board: Board): Double {
        var sum = 0.0
        for (r in 0 until board.rows) {
            val f = (0 until board.cols).count { board[r, it] != Board.EMPTY }.toDouble() / board.cols
            sum += f * f
        }
        for (c in 0 until board.cols) {
            val f = (0 until board.rows).count { board[it, c] != Board.EMPTY }.toDouble() / board.rows
            sum += f * f
        }
        return sum / (board.rows + board.cols)
    }

    /** For each gem, how full its fullest line is (0..1), averaged: rewards building towards gems. */
    private fun gemPotential(board: Board): Double {
        val gems = (0 until board.rows).flatMap { r -> (0 until board.cols).map { c -> GridPoint(r, c) } }
            .filter { board[it] == Board.GEM }
        if (gems.isEmpty()) return 1.0
        return gems.map { g ->
            val row = (0 until board.cols).count { board[g.row, it] != Board.EMPTY }.toDouble() / board.cols
            val col = (0 until board.rows).count { board[it, g.col] != Board.EMPTY }.toDouble() / board.rows
            maxOf(row, col)
        }.average()
    }

    private fun unplayable(board: Board, tray: List<Piece?>): Int =
        tray.count { it != null && !Placement.fitsAnywhere(board, it.shape) }

    private fun isolatedEmptyCells(board: Board): Int {
        var count = 0
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                if (board[r, c] != Board.EMPTY) continue
                val open = listOf(GridPoint(r - 1, c), GridPoint(r + 1, c), GridPoint(r, c - 1), GridPoint(r, c + 1))
                    .any { board.inBounds(it) && !board.isOccupied(it) }
                if (!open) count++
            }
        }
        return count
    }
}
