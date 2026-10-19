package com.puzzleblockblast.game.feature.game

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.random.Pcg32
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.model.UserSettings
import com.puzzleblockblast.game.core.testing.FakeClassicGameRepository
import com.puzzleblockblast.game.core.testing.FakeProgressionRepository
import com.puzzleblockblast.game.core.testing.FakeAdsGateway
import com.puzzleblockblast.game.core.testing.FakeEntitlements
import com.puzzleblockblast.game.core.monetization.AdsController
import com.puzzleblockblast.game.core.testing.FakeWallClock
import com.puzzleblockblast.game.core.data.progress.Reward
import com.puzzleblockblast.game.core.testing.FakeSettingsRepository
import com.puzzleblockblast.game.core.testing.FakeSoundEffects
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val classic = ClassicGame()
    private val repository = FakeClassicGameRepository()
    private val analytics = FakeAnalyticsTracker()
    private val settings = FakeSettingsRepository()
    private val sounds = FakeSoundEffects()
    private val progression = FakeProgressionRepository()
    private val adsGateway = FakeAdsGateway()
    private val entitlements = FakeEntitlements()
    private var nextSeed = 100L

    private fun viewModel(repo: FakeClassicGameRepository = repository, seeds: SeedSource = SeedSource { nextSeed++ }) =
        GameViewModel(
            classic = classic,
            repository = repo,
            analytics = analytics,
            seeds = seeds,
            appScope = CoroutineScope(mainDispatcher.dispatcher),
            settingsRepository = settings,
            sounds = sounds,
            progression = progression,
            clock = FakeWallClock(),
            ads = AdsController(adsGateway, entitlements, analytics, FakeWallClock(), CoroutineScope(mainDispatcher.dispatcher)),
        )

    private val GameViewModel.game: GameState get() = requireNotNull(uiState.value.game)

    private fun GameViewModel.playBestMove() {
        val move = requireNotNull(classic.bestMove(game))
        onPlace(move.slot, move.origin)
    }

    /** Two dots on a checkerboard: the first dot ends the game because the 2x2 square never fits. */
    private fun almostOverState(score: Int) = GameState(
        seed = 1L,
        board = Board.fromTiles(10, 10, IntArray(100) { i -> if ((i / 10 + i % 10) % 2 == 0) 1 else 0 }),
        tray = listOf(Piece(ShapeCatalog.Classic["dot_0"]!!, 1), Piece(ShapeCatalog.Classic["sq2_0"]!!, 1), null),
        score = score,
        combo = 0,
        moves = 10,
        linesCleared = 0,
        rng = Pcg32.seeded(1L).snapshot(),
        status = GameStatus.PLAYING,
        undosRemaining = 1,
        hintsRemaining = 3,
        previous = null,
    )

    @Test
    fun startsNewGameWhenNothingSaved() {
        val vm = viewModel()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(vm.game).isEqualTo(classic.newGame(100L))
        assertThat(state.slotsThatFit).containsExactly(true, true, true)
        assertThat(analytics.events.single().params).containsEntry(AnalyticsEvents.Params.RESUMED, false)
    }

    @Test
    fun placingAPieceUpdatesScoreAndAutosaves() {
        val vm = viewModel()

        vm.playBestMove()

        assertThat(vm.game.moves).isEqualTo(1)
        assertThat(vm.game.score).isGreaterThan(0)
        assertThat(repository.saved).isEqualTo(vm.game)
    }

    @Test
    fun illegalPlacementIsIgnored() {
        val vm = viewModel()
        val before = vm.game

        vm.onPlace(slot = 0, origin = GridPoint(-5, -5))
        vm.onPlace(slot = 7, origin = GridPoint(0, 0))

        assertThat(vm.game).isEqualTo(before)
        assertThat(repository.saveCount).isEqualTo(0)
    }

    @Test
    fun interruptedGameIsRestoredByNextViewModel() {
        // Simulates process death: a new ViewModel with only the persisted data.
        val first = viewModel()
        repeat(4) { first.playBestMove() }
        val played = first.game

        val second = viewModel()

        assertThat(second.game).isEqualTo(played)
        assertThat(analytics.events.last().params).containsEntry(AnalyticsEvents.Params.RESUMED, true)
    }

    @Test
    fun undoRevertsOnceAndIsThenDisabled() {
        val vm = viewModel()
        val start = vm.game
        vm.playBestMove()
        assertThat(vm.uiState.value.canUndo).isTrue()

        vm.onUndo()

        assertThat(vm.game).isEqualTo(start.copy(undosRemaining = 0))
        assertThat(vm.uiState.value.canUndo).isFalse()
        assertThat(repository.saved).isEqualTo(vm.game)
        assertThat(analytics.events.map { it.name }).contains(AnalyticsEvents.UNDO_USED)
    }

    @Test
    fun hintShowsALegalMoveAndCountsDown() {
        val vm = viewModel()

        vm.onHint()

        val hint = requireNotNull(vm.uiState.value.hint)
        assertThat(classic.canPlace(vm.game, hint.slot, hint.origin)).isTrue()
        assertThat(vm.game.hintsRemaining).isEqualTo(2)
    }

    @Test
    fun hintsRunOutWithMessage() {
        val vm = viewModel()
        repeat(3) { vm.onHint() }

        vm.onHint()

        assertThat(vm.uiState.value.message).isEqualTo(GameMessage.NO_HINTS_LEFT)
        assertThat(vm.game.hintsRemaining).isEqualTo(0)
        vm.onMessageShown()
        assertThat(vm.uiState.value.message).isNull()
    }

    @Test
    fun hintClearsAfterNextMove() {
        val vm = viewModel()
        vm.onHint()

        vm.playBestMove()

        assertThat(vm.uiState.value.hint).isNull()
    }

    @Test
    fun pauseBlocksMovesUntilResumed() {
        val vm = viewModel()
        vm.onPause()
        val before = vm.game

        vm.playBestMove()
        vm.onHint()

        assertThat(vm.game).isEqualTo(before)
        vm.onResume()
        vm.playBestMove()
        assertThat(vm.game.moves).isEqualTo(1)
    }

    @Test
    fun gameOverRecordsBestClearsSaveAndFlagsNewBest() {
        repository.apply { kotlinx.coroutines.runBlocking { saveInProgress(almostOverState(score = 250)) } }
        val vm = viewModel()

        vm.onPlace(slot = 0, origin = GridPoint(0, 1))

        val state = vm.uiState.value
        assertThat(vm.game.isGameOver).isTrue()
        assertThat(state.isNewBest).isTrue()
        assertThat(state.canUndo).isFalse()
        assertThat(repository.saved).isNull()
        assertThat(analytics.events.last().name).isEqualTo(AnalyticsEvents.GAME_OVER)
    }

    @Test
    fun gameOverBelowBestIsNotNewBest() {
        val repo = FakeClassicGameRepository(initialBest = 10_000)
        kotlinx.coroutines.runBlocking { repo.saveInProgress(almostOverState(score = 250)) }
        val vm = viewModel(repo = repo, seeds = { 1L })

        vm.onPlace(slot = 0, origin = GridPoint(0, 1))

        assertThat(vm.uiState.value.isNewBest).isFalse()
        assertThat(vm.uiState.value.displayBest).isEqualTo(10_000)
    }

    @Test
    fun gameOverRecordsProgressionAndShowsTheReward() {
        progression.nextReward = Reward(xp = 12, newAchievements = listOf("first_game"))
        runBlocking { repository.saveInProgress(almostOverState(score = 250)) }
        val vm = viewModel()

        vm.onPlace(slot = 0, origin = GridPoint(0, 1))

        assertThat(progression.classicGames).containsExactly(251)
        assertThat(vm.uiState.value.reward).isEqualTo(Reward(12, listOf("first_game")))
    }

    @Test
    fun abandonedGameIsRecordedButItsRewardIsNotShownOnTheNewGame() {
        progression.nextReward = Reward(xp = 5)
        val vm = viewModel()
        vm.playBestMove()

        vm.onRestart()

        assertThat(progression.classicGames).hasSize(1)
        assertThat(vm.uiState.value.reward).isNull()
    }

    @Test
    fun rewardedUndoIsOfferedOnlyWhenTheFreeUndoIsUsed() {
        val vm = viewModel()
        vm.playBestMove()
        assertThat(vm.uiState.value.rewardedUndoOffer).isFalse() // free undo still available

        vm.onUndo()
        vm.playBestMove()

        assertThat(vm.uiState.value.canUndo).isFalse()
        assertThat(vm.uiState.value.rewardedUndoOffer).isTrue()
    }

    @Test
    fun undoIsGrantedOnlyWhenTheAdRewardIsEarned() {
        val vm = viewModel()
        vm.playBestMove()
        vm.onUndo()
        vm.playBestMove()

        adsGateway.nextRewardEarned = false // player closed the ad early
        vm.onWatchAdForUndo(Activity())
        assertThat(vm.game.undosRemaining).isEqualTo(0)

        adsGateway.nextRewardEarned = true
        vm.onWatchAdForUndo(Activity())
        assertThat(vm.game.undosRemaining).isEqualTo(1)
        assertThat(vm.uiState.value.canUndo).isTrue()
        assertThat(repository.saved).isEqualTo(vm.game)
    }

    @Test
    fun rewardedHintsAreCappedPerGameAndResetOnRestart() {
        val vm = viewModel()
        repeat(3) { vm.onHint() }

        // Offered only while out of hints: watch, use the hint, repeat.
        repeat(5) {
            vm.onWatchAdForHint(Activity())
            vm.onHint()
        }

        assertThat(adsGateway.rewardedShows).isEqualTo(2)
        assertThat(vm.game.hintsRemaining).isEqualTo(0)
        assertThat(vm.uiState.value.rewardedHintOffer).isFalse()

        vm.onRestart()
        assertThat(vm.uiState.value.adHintsLeft).isEqualTo(2)
    }

    @Test
    fun noAdOffersWithoutConsentOrALoadedAd() {
        adsGateway.canRequest.value = false
        val vm = viewModel()
        repeat(3) { vm.onHint() }

        assertThat(vm.uiState.value.rewardedHintOffer).isFalse()
        vm.onWatchAdForHint(Activity())
        assertThat(adsGateway.rewardedShows).isEqualTo(0)
    }

    @Test
    fun leavingAFinishedGameIsABreakThatRunsTheActionAfterwards() {
        val vm = viewModel()
        var continued = 0

        vm.onBreak(Activity()) { continued++ }

        assertThat(continued).isEqualTo(1)
    }

    @Test
    fun placingPlaysASoundWhenSoundIsOn() {
        val vm = viewModel()

        vm.playBestMove()

        assertThat(sounds.played).hasSize(1)
    }

    @Test
    fun noSoundWhenSoundIsOff() {
        runBlocking { settings.setSoundEnabled(false) }
        val vm = viewModel()

        vm.playBestMove()

        assertThat(sounds.played).isEmpty()
        assertThat(vm.uiState.value.settings.soundEnabled).isFalse()
    }

    @Test
    fun gameOverPlaysGameOverSound() {
        runBlocking { repository.saveInProgress(almostOverState(score = 0)) }
        val vm = viewModel()

        vm.onPlace(slot = 0, origin = GridPoint(0, 1))

        assertThat(sounds.played).containsExactly(Sound.GAME_OVER)
    }

    @Test
    fun restartKeepsSettings() {
        runBlocking { settings.setReducedMotion(true) }
        val vm = viewModel()

        vm.onRestart()

        assertThat(vm.uiState.value.settings).isEqualTo(UserSettings(reducedMotion = true))
    }

    @Test
    fun restartStartsFreshGameAndKeepsAbandonedScoreForBest() {
        val vm = viewModel()
        repeat(3) { vm.playBestMove() }
        val abandonedScore = vm.game.score

        vm.onRestart()

        assertThat(vm.game.moves).isEqualTo(0)
        assertThat(vm.game.seed).isEqualTo(101L)
        assertThat(vm.uiState.value.displayBest).isEqualTo(abandonedScore)
        assertThat(repository.saved).isNull()
    }
}
