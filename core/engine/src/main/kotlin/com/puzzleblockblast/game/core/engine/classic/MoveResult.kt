package com.puzzleblockblast.game.core.engine.classic

import com.puzzleblockblast.game.core.engine.board.FullLines
import com.puzzleblockblast.game.core.engine.board.GridPoint

/** What a committed move did. Used for UI feedback, analytics and tests. */
data class MoveOutcome(
    val move: Move,
    val placedCells: List<GridPoint>,
    val clearedLines: FullLines,
    val clearedCellCount: Int,
    val score: MoveScore,
    val newTrayDealt: Boolean,
    val gameOver: Boolean,
)

sealed interface MoveResult {
    data class Accepted(val state: GameState, val outcome: MoveOutcome) : MoveResult

    /** The move was illegal; the game state is unchanged. */
    data class Rejected(val reason: RejectReason) : MoveResult
}

enum class RejectReason { GAME_OVER, INVALID_SLOT, EMPTY_SLOT, DOES_NOT_FIT }

sealed interface HintResult {
    data class Shown(val move: Move, val state: GameState) : HintResult
    data object NoHintsRemaining : HintResult
    data object NoValidMove : HintResult
}
