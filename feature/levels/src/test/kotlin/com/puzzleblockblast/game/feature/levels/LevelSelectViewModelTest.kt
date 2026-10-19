package com.puzzleblockblast.game.feature.levels

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.data.level.LevelProgress
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.Objective
import com.puzzleblockblast.game.core.testing.FakeLevelProgressRepository
import com.puzzleblockblast.game.core.testing.FakeLevelRepository
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class LevelSelectViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun level(index: Int) = LevelDefinition(
        id = "L%04d".format(index),
        index = index,
        board = Board.empty(),
        seed = index.toLong(),
        pool = listOf("dot"),
        objectives = listOf(Objective.ClearLines(1)),
        maxMoves = 10,
        starMoves = listOf(8, 5),
        undoAllowance = 1,
        hintAllowance = 1,
    )

    @Test
    fun tilesShowStarsAndUnlockInOrder() = runTest {
        val progress = FakeLevelProgressRepository(
            mapOf("L0001" to LevelProgress("L0001").withWin(stars = 2, score = 50, atMillis = 0)),
        )
        val vm = LevelSelectViewModel(FakeLevelRepository((1..4).map(::level)), progress)

        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertThat(state.tiles.map { it.unlocked }).containsExactly(true, true, false, false).inOrder()
            assertThat(state.tiles.first().stars).isEqualTo(2)
            assertThat(state.totalStars).isEqualTo(2)
            assertThat(state.maxStars).isEqualTo(12)
            assertThat(state.nextUp?.id).isEqualTo("L0002")

            progress.recordWin("L0002", stars = 3, score = 80, atMillis = 1)
            val updated = awaitItem()
            assertThat(updated.tiles[2].unlocked).isTrue()
            assertThat(updated.totalStars).isEqualTo(5)
        }
    }
}
