package com.puzzleblockblast.game.feature.game

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun startsWithEmptyClassicBoard() {
        val state = GameViewModel().uiState.value

        assertThat(state.board.rows).isEqualTo(10)
        assertThat(state.board.isEmpty).isTrue()
        assertThat(state.score).isEqualTo(0)
    }
}
