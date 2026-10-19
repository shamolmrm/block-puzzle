package com.puzzleblockblast.game.core.engine.level

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.ClassicRules
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.HintResult
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.MoveOutcome
import com.puzzleblockblast.game.core.engine.classic.MoveResult
import com.puzzleblockblast.game.core.engine.classic.RejectReason
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog

enum class LevelResult { IN_PROGRESS, WON, OUT_OF_MOVES, NO_MOVES }

data class LevelState(val game: GameState, val result: LevelResult) {
    val isFinished: Boolean get() = result != LevelResult.IN_PROGRESS
}

/** Progress towards one objective, for the HUD. */
data class ObjectiveProgress(val objective: Objective, val current: Int) {
    val isMet: Boolean get() = current >= objective.target
}

sealed interface LevelMoveResult {
    data class Accepted(val state: LevelState, val outcome: MoveOutcome) : LevelMoveResult
    data class Rejected(val reason: RejectReason) : LevelMoveResult
}

/**
 * Level Mode rules: Classic placement, clearing, scoring and fair dealing, played on the level's
 * starting board with its own piece pool, plus a move limit and objectives. Deterministic: the
 * level's seed and the player's moves fully decide every deal.
 *
 * Order of checks after a move: objectives met wins (even on the last move or with no moves left);
 * otherwise the move limit fails the level; otherwise no fitting piece fails it.
 */
class LevelGame(val level: LevelDefinition, catalog: ShapeCatalog = ShapeCatalog.Classic) {

    private val classic = ClassicGame(
        rules = ClassicRules(
            boardSize = level.board.rows,
            undosPerGame = level.undoAllowance,
            hintsPerGame = level.hintAllowance,
        ),
        catalog = catalog.subset(level.pool),
    )

    fun start(): LevelState {
        val game = classic.newGame(level.seed, level.board)
        // A starting board where nothing fits would be a content bug; the validator rejects it.
        return LevelState(game, if (game.isGameOver) LevelResult.NO_MOVES else LevelResult.IN_PROGRESS)
    }

    fun slotsThatFit(state: LevelState): List<Boolean> = classic.slotsThatFit(state.game)

    fun canPlace(state: LevelState, slot: Int, origin: GridPoint): Boolean =
        !state.isFinished && classic.canPlace(state.game, slot, origin)

    fun place(state: LevelState, move: Move): LevelMoveResult {
        if (state.isFinished) return LevelMoveResult.Rejected(RejectReason.GAME_OVER)
        return when (val result = classic.place(state.game, move)) {
            is MoveResult.Rejected -> LevelMoveResult.Rejected(result.reason)
            is MoveResult.Accepted -> {
                val game = result.state
                val levelResult = when {
                    progress(game).all { it.isMet } -> LevelResult.WON
                    game.moves >= level.maxMoves -> LevelResult.OUT_OF_MOVES
                    game.isGameOver -> LevelResult.NO_MOVES
                    else -> LevelResult.IN_PROGRESS
                }
                LevelMoveResult.Accepted(LevelState(game, levelResult), result.outcome)
            }
        }
    }

    /** Single-step undo within the level's allowance; never after the level has ended. */
    fun undo(state: LevelState): LevelState? {
        if (state.isFinished) return null
        return classic.undo(state.game)?.let { LevelState(it, LevelResult.IN_PROGRESS) }
    }

    /** Adds one hint (rewarded ad) to an attempt in progress. Null if finished or capped. */
    fun grantHint(state: LevelState): LevelState? =
        if (state.isFinished) null else classic.grantHint(state.game)?.let { state.copy(game = it) }

    fun requestHint(state: LevelState): HintResult =
        if (state.isFinished) HintResult.NoValidMove else classic.requestHint(state.game)

    fun bestMove(state: LevelState): Move? = if (state.isFinished) null else classic.bestMove(state.game)

    fun movesLeft(state: LevelState): Int = (level.maxMoves - state.game.moves).coerceAtLeast(0)

    fun progress(game: GameState): List<ObjectiveProgress> = level.objectives.map { objective ->
        val current = when (objective) {
            is Objective.ScoreAtLeast -> game.score
            is Objective.ClearLines -> game.linesCleared
            is Objective.ClearGems -> objective.target - game.board.toTileArray().count { it == Board.GEM }
        }
        ObjectiveProgress(objective, current)
    }

    /** 0 unless won; then 1, plus one each for winning within the 2- and 3-star move counts. */
    fun stars(state: LevelState): Int {
        if (state.result != LevelResult.WON) return 0
        return 1 + level.starMoves.count { state.game.moves <= it }
    }
}
