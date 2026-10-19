package com.puzzleblockblast.game.feature.game.level

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.LevelResult
import com.puzzleblockblast.game.core.engine.level.Objective
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.testing.FakeLevelProgressRepository
import com.puzzleblockblast.game.core.testing.FakeLevelRepository
import com.puzzleblockblast.game.core.testing.FakeProgressionRepository
import com.puzzleblockblast.game.core.testing.FakeAdsGateway
import com.puzzleblockblast.game.core.testing.FakeEntitlements
import com.puzzleblockblast.game.core.monetization.AdsController
import com.puzzleblockblast.game.core.testing.FakeWallClock
import com.puzzleblockblast.game.core.engine.level.DailyChallenge
import com.puzzleblockblast.game.core.testing.FakeSettingsRepository
import com.puzzleblockblast.game.core.testing.FakeSoundEffects
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test

class LevelGameViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Row 0 is full except column 9; a leftover block in row 9 keeps the board from emptying. */
    private val board = Board.fromTiles(
        10,
        10,
        IntArray(100) { i -> if ((i < 9) || i == 90) 1 else Board.EMPTY },
    )

    private fun level(id: String, index: Int, maxMoves: Int = 5, lines: Int = 1) = LevelDefinition(
        id = id,
        index = index,
        board = board,
        seed = 3L,
        pool = listOf("dot"),
        objectives = listOf(Objective.ClearLines(lines)),
        maxMoves = maxMoves,
        starMoves = listOf(3, 1),
        undoAllowance = 1,
        hintAllowance = 1,
    )

    private val levels = FakeLevelRepository(listOf(level("L0001", 1), level("L0002", 2, maxMoves = 1, lines = 2)))
    private val progress = FakeLevelProgressRepository()
    private val analytics = FakeAnalyticsTracker()
    private val sounds = FakeSoundEffects()
    private val progression = FakeProgressionRepository()
    private val adsGateway = FakeAdsGateway()

    private fun viewModel(id: String = "L0001") = LevelGameViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LevelGameViewModel.LEVEL_ID_KEY to id)),
        levels = levels,
        progress = progress,
        progression = progression,
        analytics = analytics,
        sounds = sounds,
        clock = FakeWallClock(now = 42L),
        settingsRepository = FakeSettingsRepository(),
        appScope = CoroutineScope(mainDispatcher.dispatcher),
        ads = AdsController(adsGateway, FakeEntitlements(), analytics, FakeWallClock(), CoroutineScope(mainDispatcher.dispatcher)),
    )

    @Test
    fun startsTheLevelAndRecordsAnAttempt() {
        val vm = viewModel()

        val state = vm.uiState.value
        assertThat(state.level?.id).isEqualTo("L0001")
        assertThat(state.movesLeft).isEqualTo(5)
        assertThat(state.nextLevelId).isEqualTo("L0002")
        assertThat(state.game.isPlaying).isTrue()
        assertThat(progress.current.getValue("L0001").attempts).isEqualTo(1)
        assertThat(analytics.events.last().name).isEqualTo(AnalyticsEvents.LEVEL_STARTED)
    }

    @Test
    fun winningRecordsStarsAndBlocksInput() {
        val vm = viewModel()

        vm.onPlace(0, GridPoint(0, 9))

        val state = vm.uiState.value
        assertThat(state.result).isEqualTo(LevelResult.WON)
        assertThat(state.stars).isEqualTo(3)
        assertThat(state.game.isPlaying).isFalse()
        assertThat(progression.levelEnds.single()).isEqualTo(FakeProgressionRepository.LevelEnd("L0001", 3, 11, 1, 1))
        assertThat(state.reward).isEqualTo(progression.nextReward)
        assertThat(sounds.played.last()).isEqualTo(Sound.BOARD_CLEAR)
        val completed = analytics.events.last()
        assertThat(completed.name).isEqualTo(AnalyticsEvents.LEVEL_COMPLETED)
        assertThat(completed.params).containsEntry(AnalyticsEvents.Params.STARS, 3)

        val before = vm.uiState.value.game.game
        vm.onPlace(1, GridPoint(5, 5))
        assertThat(vm.uiState.value.game.game).isEqualTo(before)
    }

    @Test
    fun runningOutOfMovesFailsWithoutRecordingAWin() {
        val vm = viewModel("L0002")

        vm.onPlace(0, GridPoint(5, 5))

        assertThat(vm.uiState.value.result).isEqualTo(LevelResult.OUT_OF_MOVES)
        assertThat(progression.levelEnds.single().stars).isEqualTo(0)
        assertThat(analytics.events.last().name).isEqualTo(AnalyticsEvents.LEVEL_FAILED)
        assertThat(sounds.played.last()).isEqualTo(Sound.GAME_OVER)
    }

    @Test
    fun retryStartsANewAttempt() {
        val vm = viewModel("L0002")
        vm.onPlace(0, GridPoint(5, 5))

        vm.onRestart()

        assertThat(vm.uiState.value.result).isEqualTo(LevelResult.IN_PROGRESS)
        assertThat(vm.uiState.value.movesLeft).isEqualTo(1)
        assertThat(progress.current.getValue("L0002").attempts).isEqualTo(2)
    }

    @Test
    fun undoRestoresTheMoveWithinTheAllowance() {
        val vm = viewModel()
        vm.onPlace(0, GridPoint(5, 5))
        assertThat(vm.uiState.value.movesLeft).isEqualTo(4)

        vm.onUndo()

        assertThat(vm.uiState.value.movesLeft).isEqualTo(5)
        assertThat(vm.uiState.value.game.canUndo).isFalse()
    }

    @Test
    fun hintShowsALegalMove() {
        val vm = viewModel()

        vm.onHint()

        assertThat(vm.uiState.value.game.hint).isNotNull()
        assertThat(vm.uiState.value.game.game!!.hintsRemaining).isEqualTo(0)
    }

    @Test
    fun dailyChallengeIsGeneratedForItsDateWithoutUndo() {
        val day = 20_745L
        val vm = viewModel(DailyChallenge.idFor(day))

        val state = vm.uiState.value
        assertThat(state.isDaily).isTrue()
        assertThat(state.level).isEqualTo(DailyChallenge.levelFor(day))
        assertThat(state.nextLevelId).isNull()
        assertThat(state.game.game!!.undosRemaining).isEqualTo(0)
        assertThat(progression.dailyAttempts).containsExactly(day)
        assertThat(progress.current).isEmpty()
    }

    @Test
    fun packLevelsOfferARewardedHintButTheDailyNeverDoes() {
        val vm = viewModel()
        vm.onHint() // uses the only free hint
        assertThat(vm.uiState.value.game.rewardedHintOffer).isTrue()

        vm.onWatchAdForHint(Activity())
        assertThat(vm.uiState.value.game.game!!.hintsRemaining).isEqualTo(1)

        val daily = viewModel(DailyChallenge.idFor(20_745L))
        daily.onHint()
        assertThat(daily.uiState.value.game.rewardedHintOffer).isFalse()
        daily.onWatchAdForHint(Activity())
        assertThat(adsGateway.rewardedShows).isEqualTo(1)
    }

    @Test
    fun unknownLevelShowsNotFound() {
        val vm = viewModel("L9999")

        assertThat(vm.uiState.value.notFound).isTrue()
        assertThat(vm.uiState.value.game.isLoading).isFalse()
    }
}
