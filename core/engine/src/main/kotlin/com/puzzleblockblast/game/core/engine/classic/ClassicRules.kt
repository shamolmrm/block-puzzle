package com.puzzleblockblast.game.core.engine.classic

import com.puzzleblockblast.game.core.engine.board.Board

/** Tunable Classic Mode constants. Defaults are the v1 values in docs/SCORING.md. */
data class ClassicRules(
    val boardSize: Int = Board.CLASSIC_SIZE,
    val traySize: Int = 3,
    val tileColors: Int = 7,
    val pointsPerCell: Int = 1,
    val lineBasePoints: Int = 10,
    /** Combo multiplier in halves: streak c gives min(1 + c, cap) / 2, i.e. ×1, ×1.5, ×2 … capped at ×3. */
    val maxComboHalves: Int = 6,
    val boardClearBonus: Int = 300,
    val undosPerGame: Int = 1,
    val hintsPerGame: Int = 3,
    /** Re-deal attempts before the fairness rule forces a fitting piece into the tray. */
    val fairDealAttempts: Int = 8,
) {
    init {
        require(boardSize in 4..Board.MAX_SIZE)
        require(traySize in 1..5)
        require(tileColors >= 1)
        require(maxComboHalves >= 2)
        require(undosPerGame >= 0 && hintsPerGame in 0..MAX_HINTS_PER_GAME && fairDealAttempts >= 1)
    }

    companion object {
        /** Upper bound for any configured hint count (remote config clamps to this too). */
        const val MAX_HINTS_PER_GAME = 5
    }
}
