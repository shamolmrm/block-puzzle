package com.puzzleblockblast.game.core.engine.classic

/** Points awarded for one move, broken down for UI feedback. */
data class MoveScore(
    val placementPoints: Int,
    val clearPoints: Int,
    val boardClearBonus: Int,
    /** Combo streak after this move (0 if the move cleared nothing). */
    val combo: Int,
) {
    val total: Int get() = placementPoints + clearPoints + boardClearBonus
}

object Scoring {
    /**
     * @param cellsPlaced cells in the placed piece
     * @param linesCleared rows + columns cleared by this move
     * @param comboBefore streak before this move
     * @param boardEmptyAfter whether the board is empty after clearing
     */
    fun score(
        rules: ClassicRules,
        cellsPlaced: Int,
        linesCleared: Int,
        comboBefore: Int,
        boardEmptyAfter: Boolean,
    ): MoveScore {
        val placement = cellsPlaced * rules.pointsPerCell
        if (linesCleared == 0) return MoveScore(placement, 0, 0, combo = 0)

        val combo = comboBefore + 1
        val base = rules.lineBasePoints * linesCleared * linesCleared
        val halves = minOf(1 + combo, rules.maxComboHalves)
        return MoveScore(
            placementPoints = placement,
            clearPoints = base * halves / 2,
            boardClearBonus = if (boardEmptyAfter) rules.boardClearBonus else 0,
            combo = combo,
        )
    }
}
