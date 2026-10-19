package com.puzzleblockblast.game.core.engine.classic

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.boardOf
import com.puzzleblockblast.game.core.engine.piece
import com.puzzleblockblast.game.core.engine.stateOf
import org.junit.Test

class ClassicGameTest {

    private val game = ClassicGame()

    private fun accept(state: GameState, slot: Int, row: Int, col: Int): MoveResult.Accepted {
        val result = game.place(state, Move(slot, GridPoint(row, col)))
        assertThat(result).isInstanceOf(MoveResult.Accepted::class.java)
        return result as MoveResult.Accepted
    }

    // ---- New game -------------------------------------------------------------------------

    @Test
    fun newGameIsDeterministicPerSeed() {
        assertThat(game.newGame(42L)).isEqualTo(game.newGame(42L))
        assertThat(game.newGame(42L).tray).isNotEqualTo(game.newGame(43L).tray)
    }

    @Test
    fun newGameStartsWithEmptyBoardFullTrayAndAllowances() {
        val state = game.newGame(7L)

        assertThat(state.board).isEqualTo(Board.empty())
        assertThat(state.tray).hasSize(3)
        assertThat(state.tray.all { it != null }).isTrue()
        assertThat(state.status).isEqualTo(GameStatus.PLAYING)
        assertThat(state.undosRemaining).isEqualTo(1)
        assertThat(state.hintsRemaining).isEqualTo(3)
        assertThat(state.canUndo).isFalse()
        assertThat(game.isConsistent(state)).isTrue()
    }

    // ---- Placement and rejection ------------------------------------------------------------

    @Test
    fun acceptedMoveScoresCellsEmptiesSlotAndRecordsUndoSnapshot() {
        val start = stateOf(Board.empty(), piece("sq2_0"), piece("dot_0"), piece("dot_0"))

        val (next, outcome) = accept(start, slot = 0, row = 4, col = 4)

        assertThat(next.score).isEqualTo(4)
        assertThat(next.tray[0]).isNull()
        assertThat(next.moves).isEqualTo(1)
        assertThat(next.board.filledCount).isEqualTo(4)
        assertThat(next.previous).isEqualTo(start)
        assertThat(outcome.placedCells).containsExactly(
            GridPoint(4, 4), GridPoint(4, 5), GridPoint(5, 4), GridPoint(5, 5),
        )
        assertThat(outcome.newTrayDealt).isFalse()
    }

    @Test
    fun invalidMovesAreRejectedWithReason() {
        val board = boardOf(*Array(10) { if (it == 0) "X........." else ".........." })
        val state = stateOf(board, piece("sq2_0"), null, piece("dot_0"))

        fun reason(slot: Int, row: Int, col: Int) =
            (game.place(state, Move(slot, GridPoint(row, col))) as MoveResult.Rejected).reason

        assertThat(reason(0, 0, 0)).isEqualTo(RejectReason.DOES_NOT_FIT) // overlap
        assertThat(reason(0, 9, 0)).isEqualTo(RejectReason.DOES_NOT_FIT) // off the bottom
        assertThat(reason(0, 0, 9)).isEqualTo(RejectReason.DOES_NOT_FIT) // off the right
        assertThat(reason(2, -1, 0)).isEqualTo(RejectReason.DOES_NOT_FIT)
        assertThat(reason(1, 5, 5)).isEqualTo(RejectReason.EMPTY_SLOT)
        assertThat(reason(3, 5, 5)).isEqualTo(RejectReason.INVALID_SLOT)
        assertThat(reason(-1, 5, 5)).isEqualTo(RejectReason.INVALID_SLOT)
    }

    // ---- Clearing and scoring ---------------------------------------------------------------

    @Test
    fun completingRowClearsItAndScores() {
        val board = boardOf(
            "...XXXXXXX", "..........", "..........", "..........", "..........",
            ".....X....", "..........", "..........", "..........", "..........",
        )
        val (next, outcome) = accept(stateOf(board, piece("i3_0"), piece("dot_0"), null), 0, 0, 0)

        assertThat(outcome.clearedLines.rows).containsExactly(0)
        assertThat(outcome.clearedCellCount).isEqualTo(10)
        assertThat(next.score).isEqualTo(3 + 10)
        assertThat(next.combo).isEqualTo(1)
        assertThat(next.linesCleared).isEqualTo(1)
        assertThat(next.board.filledCount).isEqualTo(1)
    }

    @Test
    fun simultaneousRowAndColumnClear() {
        val rows = Array(10) { r ->
            when (r) {
                9 -> ".XXXXXXXXX"
                4 -> "X....X...."
                else -> "X........."
            }
        }
        val (next, outcome) = accept(stateOf(boardOf(*rows), piece("dot_0"), piece("dot_0"), null), 0, 9, 0)

        assertThat(outcome.clearedLines.rows).containsExactly(9)
        assertThat(outcome.clearedLines.cols).containsExactly(0)
        assertThat(outcome.clearedCellCount).isEqualTo(19)
        assertThat(next.score).isEqualTo(1 + 40)
        assertThat(next.board.filledCount).isEqualTo(1)
    }

    @Test
    fun clearingTheWholeBoardAddsBonus() {
        val rows = Array(10) { if (it == 0) ".XXXXXXXXX" else ".........." }
        val (next, outcome) = accept(stateOf(boardOf(*rows), piece("dot_0"), piece("dot_0"), null), 0, 0, 0)

        assertThat(outcome.score.boardClearBonus).isEqualTo(300)
        assertThat(next.score).isEqualTo(1 + 10 + 300)
        assertThat(next.board.isEmpty).isTrue()
    }

    @Test
    fun consecutiveClearsBuildComboAndMissResetsIt() {
        val rows = Array(10) {
            when (it) {
                0, 1 -> "...XXXXXXX"
                5 -> ".....X...."
                else -> ".........."
            }
        }
        val start = stateOf(boardOf(*rows), piece("i3_0"), piece("i3_0"), piece("dot_0"))

        val first = accept(start, 0, 0, 0)
        val second = accept(first.state, 1, 1, 0)
        val third = accept(second.state, 2, 9, 9)

        assertThat(first.outcome.score.total).isEqualTo(3 + 10)
        assertThat(second.outcome.score.total).isEqualTo(3 + 15)
        assertThat(second.state.combo).isEqualTo(2)
        assertThat(third.state.combo).isEqualTo(0)
    }

    // ---- Tray and game over ----------------------------------------------------------------

    @Test
    fun newTrayIsDealtWhenAllThreePiecesAreUsed() {
        val start = stateOf(Board.empty(), piece("dot_0"), piece("dot_0"), piece("dot_0"))

        val a = accept(start, 0, 0, 0)
        val b = accept(a.state, 1, 2, 2)
        val c = accept(b.state, 2, 4, 4)

        assertThat(a.outcome.newTrayDealt).isFalse()
        assertThat(c.outcome.newTrayDealt).isTrue()
        assertThat(c.state.tray.all { it != null }).isTrue()
        assertThat(c.state.rng).isNotEqualTo(start.rng)
    }

    /** Checkerboard: dots fit, but no 2x2 square ever can, and no line is full. */
    private val checkerboard = boardOf(*Array(10) { r -> String(CharArray(10) { c -> if ((r + c) % 2 == 0) 'X' else '.' }) })

    @Test
    fun gameOverWhenNoRemainingPieceFits() {
        val start = stateOf(checkerboard, piece("dot_0"), piece("sq2_0"), null)

        val (over, outcome) = accept(start, 0, 0, 1)

        assertThat(outcome.gameOver).isTrue()
        assertThat(over.status).isEqualTo(GameStatus.GAME_OVER)
        assertThat(game.slotsThatFit(over)).containsExactly(false, false, false).inOrder()
        assertThat((game.place(over, Move(1, GridPoint(0, 0))) as MoveResult.Rejected).reason)
            .isEqualTo(RejectReason.GAME_OVER)
        assertThat(game.bestMove(over)).isNull()
        assertThat(game.undo(over)).isNull()
        assertThat(game.requestHint(over)).isEqualTo(HintResult.NoValidMove)
    }

    // ---- Undo -------------------------------------------------------------------------------

    @Test
    fun undoRestoresPreviousStateAndConsumesAllowance() {
        val start = game.newGame(11L)
        val move = requireNotNull(game.bestMove(start))
        val after = accept(start, move.slot, move.origin.row, move.origin.col).state

        val undone = requireNotNull(game.undo(after))

        assertThat(undone).isEqualTo(start.copy(undosRemaining = 0))
        assertThat(undone.canUndo).isFalse()
        assertThat(game.undo(undone)).isNull()
    }

    @Test
    fun replayAfterUndoIsIdenticalBecauseRngIsRestored() {
        var state = game.newGame(5L)
        // Play until a new tray is dealt so the undone move involves the RNG.
        while (true) {
            val move = requireNotNull(game.bestMove(state))
            val result = accept(state, move.slot, move.origin.row, move.origin.col)
            if (result.outcome.newTrayDealt) {
                val undone = requireNotNull(game.undo(result.state))
                val replay = accept(undone, move.slot, move.origin.row, move.origin.col).state
                assertThat(replay.tray).isEqualTo(result.state.tray)
                assertThat(replay.rng).isEqualTo(result.state.rng)
                assertThat(replay.board).isEqualTo(result.state.board)
                return
            }
            state = result.state
        }
    }

    @Test
    fun undoDoesNotRefundHints() {
        val start = stateOf(Board.empty(), piece("dot_0"), piece("dot_0"), null, hints = 3)
        val after = accept(start, 0, 0, 0).state
        val hinted = (game.requestHint(after) as HintResult.Shown).state

        assertThat(requireNotNull(game.undo(hinted)).hintsRemaining).isEqualTo(2)
    }

    // ---- Hints -------------------------------------------------------------------------------

    @Test
    fun hintPrefersLineClearAndConsumesOneHint() {
        val rows = Array(10) {
            when (it) {
                0 -> "...XXXXXXX"
                5 -> ".....X...."
                else -> ".........."
            }
        }
        val state = stateOf(boardOf(*rows), piece("dot_0"), piece("i3_0"), null)

        val hint = game.requestHint(state) as HintResult.Shown

        assertThat(hint.move).isEqualTo(Move(1, GridPoint(0, 0)))
        assertThat(hint.state.hintsRemaining).isEqualTo(2)
        assertThat(game.canPlace(state, hint.move.slot, hint.move.origin)).isTrue()
    }

    @Test
    fun noHintsRemaining() {
        val state = stateOf(Board.empty(), piece("dot_0"), null, null, hints = 0)

        assertThat(game.requestHint(state)).isEqualTo(HintResult.NoHintsRemaining)
    }

    // ---- Restore validation -----------------------------------------------------------------

    @Test
    fun isConsistentRejectsImpossibleStates() {
        val valid = stateOf(Board.empty(), piece("dot_0"), null, null)
        val fullRow = boardOf(*Array(10) { if (it == 0) "XXXXXXXXXX" else ".........." })

        assertThat(game.isConsistent(valid)).isTrue()
        assertThat(game.isConsistent(valid.copy(board = Board.empty(8)))).isFalse()
        assertThat(game.isConsistent(valid.copy(board = fullRow))).isFalse()
        assertThat(game.isConsistent(valid.copy(tray = listOf(null, null, null)))).isFalse()
        assertThat(game.isConsistent(valid.copy(tray = listOf(piece("dot_0", tile = 99), null, null)))).isFalse()
        assertThat(game.isConsistent(valid.copy(score = -1))).isFalse()
        assertThat(game.isConsistent(valid.copy(undosRemaining = 5))).isFalse()
        assertThat(game.isConsistent(valid.copy(previous = valid.copy(previous = valid)))).isFalse()
    }

    // ---- Property-style playthroughs --------------------------------------------------------

    @Test
    fun randomPlaythroughsKeepInvariants() {
        for (seed in 1L..200L) {
            val picker = kotlin.random.Random(seed)
            var state = game.newGame(seed)
            var guard = 0
            while (!state.isGameOver && guard++ < 1_000) {
                val legal = state.tray.withIndex().filter { it.value != null }.flatMap { (slot, p) ->
                    Placement.validOrigins(state.board, p!!.shape).map { Move(slot, it) }
                }
                assertThat(legal).isNotEmpty() // PLAYING always has a legal move
                val result = game.place(state, legal[picker.nextInt(legal.size)]) as MoveResult.Accepted

                assertThat(result.state.score).isEqualTo(state.score + result.outcome.score.total)
                assertThat(game.isConsistent(result.state)).isTrue()
                if (result.outcome.newTrayDealt) {
                    // Fairness: a freshly dealt tray always has a piece that fits.
                    assertThat(result.state.isGameOver).isFalse()
                }
                state = result.state
            }
            assertThat(state.isGameOver).isTrue()
        }
    }

    @Test
    fun sameSeedAndMovesReproduceTheSameGame() {
        fun play(seed: Long): GameState {
            var state = game.newGame(seed)
            repeat(60) {
                val move = game.bestMove(state) ?: return state
                state = (game.place(state, move) as MoveResult.Accepted).state
            }
            return state
        }

        for (seed in listOf(3L, 17L, 2026L)) {
            assertThat(play(seed)).isEqualTo(play(seed))
        }
    }
}
