package com.puzzleblockblast.game.feature.game.tutorial

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.testing.FakeSettingsRepository
import com.puzzleblockblast.game.core.testing.FakeSoundEffects
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import com.puzzleblockblast.game.feature.game.GameMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class TutorialViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val classic = ClassicGame()
    private val settings = FakeSettingsRepository()
    private val analytics = FakeAnalyticsTracker()
    private val sounds = FakeSoundEffects()

    private fun viewModel() = TutorialViewModel(classic, settings, analytics, sounds, CoroutineScope(mainDispatcher.dispatcher))

    private fun TutorialViewModel.playTarget() {
        val step = TutorialScript.steps[uiState.value.stepIndex]
        onPlace(0, step.target)
        mainDispatcher.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun everyScriptedTargetIsLegalAndClearsALine() {
        TutorialScript.steps.forEach { step ->
            val vm = viewModel()
            val state = vm.uiState.value.game.game!!.copy(board = step.board, tray = listOf(step.piece, null, null))

            assertThat(classic.canPlace(state, 0, step.target)).isTrue()
        }
    }

    @Test
    fun startsAtFirstStepWithTargetHinted() {
        val vm = viewModel()

        val state = vm.uiState.value
        assertThat(state.stepIndex).isEqualTo(0)
        assertThat(state.game.hint?.origin).isEqualTo(TutorialScript.steps[0].target)
        assertThat(analytics.events.single().name).isEqualTo(AnalyticsEvents.TUTORIAL_STARTED)
    }

    @Test
    fun wrongSpotIsRejectedWithAGentleMessage() {
        val vm = viewModel()
        val before = vm.uiState.value.game.game

        vm.onPlace(0, GridPoint(0, 0))

        assertThat(vm.uiState.value.game.game).isEqualTo(before)
        assertThat(vm.uiState.value.game.message).isEqualTo(GameMessage.TUTORIAL_TRY_TARGET)
    }

    @Test
    fun targetMoveClearsTheLineAndAdvances() {
        val vm = viewModel()

        vm.onPlace(0, TutorialScript.steps[0].target)

        val afterMove = vm.uiState.value
        assertThat(afterMove.game.lastOutcome!!.clearedLines.count).isEqualTo(1)
        assertThat(afterMove.game.game!!.tray).containsExactly(null, null, null)
        assertThat(sounds.played).containsExactly(Sound.CLEAR)

        mainDispatcher.dispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.stepIndex).isEqualTo(1)
        assertThat(vm.uiState.value.game.game!!.score).isEqualTo(afterMove.game.game!!.score)
    }

    @Test
    fun finalStepClearsTwoLinesThenFinishes() {
        val vm = viewModel()
        repeat(2) { vm.playTarget() }

        vm.onPlace(0, TutorialScript.steps[2].target)
        assertThat(vm.uiState.value.game.lastOutcome!!.clearedLines.count).isEqualTo(2)
        assertThat(sounds.played).containsExactly(Sound.CLEAR, Sound.CLEAR, Sound.CLEAR_MULTI).inOrder()
        mainDispatcher.dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.finished).isTrue()
    }

    @Test
    fun completingMarksTutorialDoneOnce() {
        val vm = viewModel()

        vm.onComplete(skipped = true)
        vm.onComplete(skipped = true)

        assertThat(runBlocking { settings.tutorialCompleted.first() }).isTrue()
        val completions = analytics.events.filter { it.name == AnalyticsEvents.TUTORIAL_COMPLETED }
        assertThat(completions).hasSize(1)
        assertThat(completions.single().params).containsEntry(AnalyticsEvents.Params.SKIPPED, true)
    }
}
