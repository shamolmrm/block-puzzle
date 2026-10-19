package com.puzzleblockblast.game.core.engine.classic

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.engine.level.LevelGame
import com.puzzleblockblast.game.core.engine.level.LevelMoveResult
import com.puzzleblockblast.game.core.engine.level.testLevel
import com.puzzleblockblast.game.core.engine.board.GridPoint
import org.junit.Test

class RewardGrantTest {

    private val game = ClassicGame()

    @Test
    fun undosAreCappedAtThreePerGame() {
        var state = game.newGame(1L)
        repeat(2) { state = requireNotNull(game.grantUndo(state)) }

        assertThat(state.undosRemaining).isEqualTo(ClassicRules.MAX_UNDOS_PER_GAME)
        assertThat(game.grantUndo(state)).isNull()
        assertThat(game.isConsistent(state)).isTrue()
    }

    @Test
    fun hintsAreCappedAndSavedStatesStayValid() {
        var state = game.newGame(1L)
        repeat(2) { state = requireNotNull(game.grantHint(state)) }

        assertThat(state.hintsRemaining).isEqualTo(5)
        assertThat(game.grantHint(state)).isNull()
        assertThat(game.isConsistent(state)).isTrue()
    }

    @Test
    fun nothingIsGrantedAfterGameOver() {
        val over = game.newGame(1L).copy(status = GameStatus.GAME_OVER)

        assertThat(game.grantUndo(over)).isNull()
        assertThat(game.grantHint(over)).isNull()
    }

    @Test
    fun levelHintsCanBeGrantedOnlyWhileInProgress() {
        val level = LevelGame(testLevel(hints = 0))
        val start = level.start()

        val granted = requireNotNull(level.grantHint(start))
        assertThat(granted.game.hintsRemaining).isEqualTo(1)

        val won = (level.place(start, Move(0, GridPoint(0, 9))) as LevelMoveResult.Accepted).state
        assertThat(level.grantHint(won)).isNull()
    }
}
