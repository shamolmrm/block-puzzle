package com.puzzleblockblast.game.core.engine.level

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.boardOf
import com.puzzleblockblast.game.core.engine.classic.HintResult
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.RejectReason
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog
import org.junit.Test

private val EMPTY_ROWS = Array(8) { ".........." }

/** Row 0 has nine filled cells and a gap at column 9; one leftover block keeps the board from emptying. */
private val ROW_GAP = boardOf("XXXXXXXXX.", *EMPTY_ROWS, "X.........")

fun testLevel(
    board: Board = ROW_GAP,
    pool: List<String> = listOf("dot"),
    objectives: List<Objective> = listOf(Objective.ClearLines(1)),
    maxMoves: Int = 10,
    starMoves: List<Int> = listOf(6, 3),
    undo: Int = 1,
    hints: Int = 1,
) = LevelDefinition(
    id = "L0001",
    index = 1,
    board = board,
    seed = 7L,
    pool = pool,
    objectives = objectives,
    maxMoves = maxMoves,
    starMoves = starMoves,
    undoAllowance = undo,
    hintAllowance = hints,
)

class LevelGameTest {

    private val gap = GridPoint(0, 9)

    @Test
    fun startsOnTheLevelBoardAndDealsOnlyFromThePool() {
        val game = LevelGame(testLevel(pool = listOf("i2")))

        val state = game.start()

        assertThat(state.game.board).isEqualTo(ROW_GAP)
        assertThat(state.game.tray.map { it!!.shape.id.substringBeforeLast('_') }).containsExactly("i2", "i2", "i2")
        assertThat(state.result).isEqualTo(LevelResult.IN_PROGRESS)
        assertThat(state.game.undosRemaining).isEqualTo(1)
        assertThat(state.game.hintsRemaining).isEqualTo(1)
    }

    @Test
    fun clearingTheRequiredLinesWins() {
        val game = LevelGame(testLevel())

        val result = game.place(game.start(), Move(0, gap)) as LevelMoveResult.Accepted

        assertThat(result.state.result).isEqualTo(LevelResult.WON)
        assertThat(result.outcome.clearedLines.count).isEqualTo(1)
    }

    @Test
    fun gemsAreCollectedWhenTheirLineClears() {
        val board = boardOf("GXXXXXXXX.", ".....G....", *Array(8) { ".........." })
        val game = LevelGame(testLevel(board = board, objectives = listOf(Objective.ClearGems(2))))
        val start = game.start()
        assertThat(game.progress(start.game).single().current).isEqualTo(0)

        val after = (game.place(start, Move(0, gap)) as LevelMoveResult.Accepted).state

        assertThat(game.progress(after.game).single().current).isEqualTo(1)
        assertThat(after.result).isEqualTo(LevelResult.IN_PROGRESS)
    }

    @Test
    fun runningOutOfMovesFails() {
        val game = LevelGame(testLevel(objectives = listOf(Objective.ClearLines(3)), maxMoves = 1))

        val after = (game.place(game.start(), Move(0, GridPoint(5, 5))) as LevelMoveResult.Accepted).state

        assertThat(after.result).isEqualTo(LevelResult.OUT_OF_MOVES)
        assertThat(game.movesLeft(after)).isEqualTo(0)
    }

    @Test
    fun meetingTheObjectiveOnTheLastMoveStillWins() {
        val game = LevelGame(testLevel(maxMoves = 1))

        val after = (game.place(game.start(), Move(0, gap)) as LevelMoveResult.Accepted).state

        assertThat(after.result).isEqualTo(LevelResult.WON)
    }

    @Test
    fun noFittingPieceAtStartIsReportedAsNoMoves() {
        val checker = Board.fromTiles(10, 10, IntArray(100) { i -> if ((i / 10 + i % 10) % 2 == 0) 1 else 0 })
        val game = LevelGame(testLevel(board = checker, pool = listOf("sq2")))

        assertThat(game.start().result).isEqualTo(LevelResult.NO_MOVES)
    }

    @Test
    fun starsComeFromMovesUsedOnlyAfterWinning() {
        val game = LevelGame(testLevel(starMoves = listOf(2, 1)))
        val start = game.start()
        assertThat(game.stars(start)).isEqualTo(0)

        val fast = (game.place(start, Move(0, gap)) as LevelMoveResult.Accepted).state
        val detour = (game.place(start, Move(0, GridPoint(5, 5))) as LevelMoveResult.Accepted).state
        val slower = (game.place(detour, Move(1, gap)) as LevelMoveResult.Accepted).state

        assertThat(game.stars(fast)).isEqualTo(3)
        assertThat(slower.result).isEqualTo(LevelResult.WON)
        assertThat(game.stars(slower)).isEqualTo(2)
    }

    @Test
    fun undoRestoresTheMoveCounterWithinTheAllowance() {
        val game = LevelGame(testLevel(objectives = listOf(Objective.ClearLines(3))))
        val start = game.start()
        val moved = (game.place(start, Move(0, GridPoint(5, 5))) as LevelMoveResult.Accepted).state

        val undone = requireNotNull(game.undo(moved))

        assertThat(undone.game.moves).isEqualTo(0)
        assertThat(undone.game.board).isEqualTo(start.game.board)
        assertThat(game.undo(undone)).isNull()
    }

    @Test
    fun finishedLevelRejectsMovesUndoAndHints() {
        val game = LevelGame(testLevel())
        val won = (game.place(game.start(), Move(0, gap)) as LevelMoveResult.Accepted).state

        assertThat(game.place(won, Move(1, GridPoint(5, 5)))).isEqualTo(LevelMoveResult.Rejected(RejectReason.GAME_OVER))
        assertThat(game.undo(won)).isNull()
        assertThat(game.requestHint(won)).isEqualTo(HintResult.NoValidMove)
    }

    @Test
    fun sameMovesGiveSameStates() {
        val level = testLevel(pool = listOf("i2", "i3", "sq2", "l4"), objectives = listOf(Objective.ScoreAtLeast(10_000)))
        fun playOut(): List<LevelState> {
            val game = LevelGame(level)
            var state = game.start()
            return buildList {
                repeat(8) {
                    val move = game.bestMove(state) ?: return@buildList
                    state = (game.place(state, move) as LevelMoveResult.Accepted).state
                    add(state)
                }
            }
        }

        assertThat(playOut()).isEqualTo(playOut())
    }

    @Test
    fun catalogSubsetExpandsBaseIdsToRotations() {
        assertThat(ShapeCatalog.Classic.subset(listOf("l4")).entries).hasSize(4)
        assertThat(ShapeCatalog.Classic.subset(listOf("l4_2")).entries.single().shape.id).isEqualTo("l4_2")
        assertThat(ShapeCatalog.Classic.knows("nope")).isFalse()
    }
}
