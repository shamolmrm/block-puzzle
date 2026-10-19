package com.puzzleblockblast.game.core.engine.classic

import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.board.Placement
import com.puzzleblockblast.game.core.engine.random.Pcg32
import com.puzzleblockblast.game.core.engine.shape.Shape
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog

/**
 * Classic Mode rules. Stateless and deterministic: the same seed and the same moves always produce
 * the same states. All functions return new [GameState]s and never mutate their input.
 */
class ClassicGame(
    val rules: ClassicRules = ClassicRules(),
    val catalog: ShapeCatalog = ShapeCatalog.Classic,
) {

    /** Starts a game on [board] (empty by default; levels pass their pre-filled layout). */
    fun newGame(seed: Long, board: Board = Board.empty(rules.boardSize)): GameState {
        require(board.rows == rules.boardSize && board.cols == rules.boardSize) { "Board must be ${rules.boardSize} square" }
        val rng = Pcg32.seeded(seed)
        val tray = deal(board, rng)
        return GameState(
            seed = seed,
            board = board,
            tray = tray,
            score = 0,
            combo = 0,
            moves = 0,
            linesCleared = 0,
            rng = rng.snapshot(),
            // Only possible on a pre-filled (level) board: nothing in the pool fits at all.
            status = if (tray.filterNotNull().any { Placement.fitsAnywhere(board, it.shape) }) GameStatus.PLAYING else GameStatus.GAME_OVER,
            undosRemaining = rules.undosPerGame,
            hintsRemaining = rules.hintsPerGame,
            previous = null,
        )
    }

    fun canPlace(state: GameState, slot: Int, origin: GridPoint): Boolean {
        val piece = state.tray.getOrNull(slot) ?: return false
        return Placement.canPlace(state.board, piece.shape, origin)
    }

    /** Whether each tray slot's piece fits anywhere (false for empty slots). */
    fun slotsThatFit(state: GameState): List<Boolean> =
        state.tray.map { piece -> piece != null && Placement.fitsAnywhere(state.board, piece.shape) }

    fun place(state: GameState, move: Move): MoveResult {
        if (state.isGameOver) return MoveResult.Rejected(RejectReason.GAME_OVER)
        if (move.slot !in state.tray.indices) return MoveResult.Rejected(RejectReason.INVALID_SLOT)
        val piece = state.tray[move.slot] ?: return MoveResult.Rejected(RejectReason.EMPTY_SLOT)
        if (!Placement.canPlace(state.board, piece.shape, move.origin)) {
            return MoveResult.Rejected(RejectReason.DOES_NOT_FIT)
        }

        val placed = Placement.place(state.board, piece.shape, move.origin, piece.tile)
        val lines = Placement.findFullLines(placed)
        val (cleared, clearedCellCount) = Placement.clear(placed, lines)
        val moveScore = Scoring.score(
            rules = rules,
            cellsPlaced = piece.shape.size,
            linesCleared = lines.count,
            comboBefore = state.combo,
            boardEmptyAfter = cleared.isEmpty,
        )

        var tray = state.tray.toMutableList<Piece?>().also { it[move.slot] = null }.toList()
        val rng = Pcg32.restore(state.rng)
        val newTrayDealt = tray.all { it == null }
        if (newTrayDealt) tray = deal(cleared, rng)

        val gameOver = tray.filterNotNull().none { Placement.fitsAnywhere(cleared, it.shape) }
        val next = state.copy(
            board = cleared,
            tray = tray,
            score = state.score + moveScore.total,
            combo = moveScore.combo,
            moves = state.moves + 1,
            linesCleared = state.linesCleared + lines.count,
            rng = rng.snapshot(),
            status = if (gameOver) GameStatus.GAME_OVER else GameStatus.PLAYING,
            previous = state.copy(previous = null),
        )
        return MoveResult.Accepted(
            state = next,
            outcome = MoveOutcome(
                move = move,
                placedCells = piece.shape.cellsAt(move.origin),
                clearedLines = lines,
                clearedCellCount = clearedCellCount,
                score = moveScore,
                newTrayDealt = newTrayDealt,
                gameOver = gameOver,
            ),
        )
    }

    /**
     * Reverts the last placement (board, tray, score, combo, RNG and move count). Returns null when
     * undo isn't allowed: no previous move, no undos left, or the game is over. Hints already used
     * are not refunded.
     */
    fun undo(state: GameState): GameState? {
        if (!state.canUndo) return null
        val previous = state.previous ?: return null
        return previous.copy(
            undosRemaining = state.undosRemaining - 1,
            hintsRemaining = state.hintsRemaining,
            previous = null,
        )
    }

    /** Uses one hint and returns the best legal move. Never consumes a hint when there is no legal move. */
    fun requestHint(state: GameState): HintResult {
        val move = bestMove(state) ?: return HintResult.NoValidMove
        if (state.hintsRemaining <= 0) return HintResult.NoHintsRemaining
        return HintResult.Shown(move, state.copy(hintsRemaining = state.hintsRemaining - 1))
    }

    /**
     * The highest-ranked legal move, or null if none exists. Ranking: most lines cleared, then fewest
     * isolated empty cells, then fewest filled cells; ties go to the lowest slot, row, column.
     */
    fun bestMove(state: GameState): Move? {
        if (state.isGameOver) return null
        var best: Move? = null
        var bestKey: Triple<Int, Int, Int>? = null
        for ((slot, piece) in state.tray.withIndex()) {
            if (piece == null) continue
            for (origin in Placement.validOrigins(state.board, piece.shape)) {
                val placed = Placement.place(state.board, piece.shape, origin, piece.tile)
                val lines = Placement.findFullLines(placed)
                val after = Placement.clear(placed, lines).first
                val key = Triple(-lines.count, isolatedEmptyCells(after), after.filledCount)
                if (bestKey == null || compareKeys(key, bestKey) < 0) {
                    best = Move(slot, origin)
                    bestKey = key
                }
            }
        }
        return best
    }

    /** Fewer is better: an empty cell whose four neighbours are all filled or off the board. */
    private fun isolatedEmptyCells(board: Board): Int {
        var count = 0
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                if (board[r, c] != Board.EMPTY) continue
                val neighbours = listOf(GridPoint(r - 1, c), GridPoint(r + 1, c), GridPoint(r, c - 1), GridPoint(r, c + 1))
                if (neighbours.none { board.inBounds(it) && !board.isOccupied(it) }) count++
            }
        }
        return count
    }

    private fun compareKeys(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int =
        compareValuesBy(a, b, { it.first }, { it.second }, { it.third })

    /**
     * Deals a full tray. Fairness rule: if any catalogue shape fits the board, at least one dealt
     * piece fits. Every random draw comes from [rng], so dealing is deterministic.
     */
    private fun deal(board: Board, rng: Pcg32): List<Piece?> {
        val all = catalog.entries.map { it.shape to it.weight }
        fun randomTray() = List(rules.traySize) { Piece(pickShape(all, rng), pickTile(rng)) }
        fun anyFits(tray: List<Piece>) = tray.any { Placement.fitsAnywhere(board, it.shape) }

        var tray = randomTray()
        var attempts = 1
        while (!anyFits(tray) && attempts < rules.fairDealAttempts) {
            tray = randomTray()
            attempts++
        }
        if (anyFits(tray)) return tray

        val fitting = catalog.entries.filter { Placement.fitsAnywhere(board, it.shape) }
        if (fitting.isEmpty()) return tray
        val slot = rng.nextInt(rules.traySize)
        return tray.toMutableList().also {
            it[slot] = Piece(pickShape(fitting.map { e -> e.shape to e.weight }, rng), pickTile(rng))
        }
    }

    private fun pickShape(weighted: List<Pair<Shape, Int>>, rng: Pcg32): Shape {
        var roll = rng.nextInt(weighted.sumOf { it.second })
        for ((shape, weight) in weighted) {
            if (roll < weight) return shape
            roll -= weight
        }
        error("unreachable: weights are positive")
    }

    private fun pickTile(rng: Pcg32): Int = rng.nextInt(rules.tileColors) + 1

    /**
     * Structural validation for states restored from storage. Rejects anything this rule set could
     * never have produced, so corrupt or tampered saves are discarded instead of crashing the game.
     */
    fun isConsistent(state: GameState): Boolean {
        fun check(s: GameState, isSnapshot: Boolean): Boolean =
            s.board.rows == rules.boardSize && s.board.cols == rules.boardSize &&
                s.tray.size == rules.traySize &&
                s.tray.all { it == null || (catalog[it.shape.id] == it.shape && it.tile in 1..rules.tileColors) } &&
                (s.isGameOver || s.tray.any { it != null }) &&
                s.board.toTileArray().all { it in 0..rules.tileColors } &&
                s.score >= 0 && s.combo >= 0 && s.moves >= 0 && s.linesCleared >= 0 &&
                s.undosRemaining in 0..rules.undosPerGame && s.hintsRemaining in 0..rules.hintsPerGame &&
                Placement.findFullLines(s.board).isEmpty &&
                (if (isSnapshot) s.previous == null else true)
        val previous = state.previous
        return check(state, isSnapshot = false) && (previous == null || check(previous, isSnapshot = true))
    }
}
