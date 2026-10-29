package com.puzzleblockblast.game.feature.levels

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.data.level.LevelProgress
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.level.EventDefinition
import com.puzzleblockblast.game.core.engine.level.EventSchedule
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.Objective
import com.puzzleblockblast.game.core.testing.FakeEventRepository
import com.puzzleblockblast.game.core.testing.FakeLevelProgressRepository
import com.puzzleblockblast.game.core.testing.FakeWallClock
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EventViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun level(i: Int) = LevelDefinition(
        "E01L0$i", i, Board.empty(), i.toLong(), listOf("dot"), listOf(Objective.ClearLines(1)), 10, listOf(8, 5), 1, 1,
    )

    private val event = EventDefinition("E01", "Gem Rush", (1..3).map(::level))
    private val monday = 20_745L

    @Test
    fun showsThisWeeksRunWithOrderedUnlocksAndProgressPerRun() = runTest {
        val run = EventSchedule.active(listOf(event), monday)!!
        val progress = FakeLevelProgressRepository(
            mapOf(
                run.runLevelId(event.levels[0]) to LevelProgress(run.runLevelId(event.levels[0])).withWin(2, 40, 0),
                // Last week's run doesn't count this week.
                "E01L02@${run.instance - 1}" to LevelProgress("E01L02@${run.instance - 1}").withWin(3, 90, 0),
            ),
        )
        val vm = EventViewModel(FakeEventRepository(listOf(event)), progress, FakeWallClock(today = monday + 2))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.title).isEqualTo("Gem Rush")
        assertThat(state.daysLeft).isEqualTo(5)
        assertThat(state.tiles.map { it.unlocked }).containsExactly(true, true, false).inOrder()
        assertThat(state.completed).isEqualTo(1)
        assertThat(state.nextUp?.id).isEqualTo(run.runLevelId(event.levels[1]))
    }

    @Test
    fun noEventContentShowsUnavailable() = runTest {
        val vm = EventViewModel(FakeEventRepository(), FakeLevelProgressRepository(), FakeWallClock())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.title).isNull()
    }
}
