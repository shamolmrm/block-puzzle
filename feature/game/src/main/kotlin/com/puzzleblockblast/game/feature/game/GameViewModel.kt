package com.puzzleblockblast.game.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.audio.SoundEffects
import com.puzzleblockblast.game.core.data.classic.ClassicGameRepository
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.progress.Reward
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.HintResult
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.MoveOutcome
import com.puzzleblockblast.game.core.engine.classic.MoveResult
import com.puzzleblockblast.game.core.model.GameMode
import com.puzzleblockblast.game.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GameMessage { NO_HINTS_LEFT, NO_VALID_MOVE, TUTORIAL_TRY_TARGET }

/** The sound that best describes a move: the biggest event wins. */
internal fun soundFor(outcome: MoveOutcome): Sound = when {
    outcome.gameOver -> Sound.GAME_OVER
    outcome.score.boardClearBonus > 0 -> Sound.BOARD_CLEAR
    outcome.score.combo >= 2 -> Sound.COMBO
    outcome.clearedLines.count >= 2 -> Sound.CLEAR_MULTI
    outcome.clearedLines.count == 1 -> Sound.CLEAR
    else -> Sound.PLACE
}

data class GameUiState(
    val isLoading: Boolean = true,
    val game: GameState? = null,
    /** Per tray slot: whether that piece fits anywhere. Pieces that don't are drawn dimmed. */
    val slotsThatFit: List<Boolean> = emptyList(),
    /** Stored best score (from before any in-progress save raised it). */
    val bestScore: Int = 0,
    val isPaused: Boolean = false,
    val hint: Move? = null,
    val lastOutcome: MoveOutcome? = null,
    val isNewBest: Boolean = false,
    val message: GameMessage? = null,
    val settings: UserSettings = UserSettings(),
    /** Set by modes with their own end conditions (a won or failed level): blocks all input. */
    val isEnded: Boolean = false,
    /** XP and achievements earned by the finished game, once recorded. */
    val reward: Reward? = null,
) {
    val isPlaying: Boolean get() = game != null && !game.isGameOver && !isPaused && !isEnded
    val canUndo: Boolean get() = isPlaying && game?.canUndo == true
    val canHint: Boolean get() = isPlaying && (game?.hintsRemaining ?: 0) > 0
    val displayBest: Int get() = maxOf(bestScore, game?.score ?: 0)
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val classic: ClassicGame,
    private val repository: ClassicGameRepository,
    private val analytics: AnalyticsTracker,
    private val seeds: SeedSource,
    @param:ApplicationScope private val appScope: CoroutineScope,
    settingsRepository: SettingsRepository,
    private val sounds: SoundEffects,
    private val progression: ProgressionRepository,
    private val clock: WallClock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /** Best score when the current game started; decides "new best" at game over. */
    private var bestAtStart = 0

    /** Highest combo in the current game (from this session; a resumed game starts at its current combo). */
    private var bestCombo = 0

    /**
     * Writes run in order on the application scope, so a save requested just before the player
     * leaves the screen still completes. Unlimited (not conflated) so a finish is never dropped.
     */
    private val writes = Channel<Write>(Channel.UNLIMITED)

    private sealed interface Write {
        data class Save(val state: GameState) : Write
        /** [seed] identifies the game, so its reward is only shown while that game is on screen. */
        data class Finish(val score: Int, val lines: Int, val bestCombo: Int, val seed: Long) : Write
    }

    init {
        appScope.launch {
            for (write in writes) {
                when (write) {
                    is Write.Save -> repository.saveInProgress(write.state)
                    is Write.Finish -> {
                        repository.finishGame(write.score)
                        val reward = progression.recordClassicGame(write.score, write.lines, write.bestCombo, clock.nowMillis())
                        _uiState.update { if (it.game?.seed == write.seed && it.game.isGameOver) it.copy(reward = reward) else it }
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s -> _uiState.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            val best = repository.bestScore.first()
            val restored = repository.loadInProgress()
            bestAtStart = best
            val state = restored ?: classic.newGame(seeds.nextSeed())
            bestCombo = state.combo
            _uiState.update { it.copy(isLoading = false, bestScore = best).withGame(state) }
            logGameStarted(resumed = restored != null)

            repository.bestScore.collect { stored -> _uiState.update { it.copy(bestScore = stored) } }
        }
    }

    fun onPlace(slot: Int, origin: GridPoint) {
        val current = _uiState.value
        val state = current.game ?: return
        if (!current.isPlaying) return
        val result = classic.place(state, Move(slot, origin)) as? MoveResult.Accepted ?: return

        val next = result.state
        bestCombo = maxOf(bestCombo, result.outcome.score.combo)
        if (current.settings.soundEnabled) sounds.play(soundFor(result.outcome))
        if (next.isGameOver) {
            _uiState.update {
                it.withGame(next).copy(lastOutcome = result.outcome, isNewBest = next.score > bestAtStart)
            }
            writes.trySend(Write.Finish(next.score, next.linesCleared, bestCombo, next.seed))
            analytics.log(
                AnalyticsEvent(
                    AnalyticsEvents.GAME_OVER,
                    mapOf(
                        AnalyticsEvents.Params.MODE to GameMode.CLASSIC.analyticsName,
                        AnalyticsEvents.Params.SCORE_BUCKET to AnalyticsEvents.scoreBucket(next.score),
                        AnalyticsEvents.Params.MOVES to next.moves,
                    ),
                ),
            )
        } else {
            _uiState.update { it.withGame(next).copy(lastOutcome = result.outcome) }
            writes.trySend(Write.Save(next))
        }
    }

    fun onUndo() {
        if (!_uiState.value.canUndo) return
        val undone = classic.undo(_uiState.value.game ?: return) ?: return
        _uiState.update { it.withGame(undone).copy(lastOutcome = null) }
        writes.trySend(Write.Save(undone))
        analytics.log(AnalyticsEvent(AnalyticsEvents.UNDO_USED, mapOf(AnalyticsEvents.Params.SOURCE to "free")))
    }

    fun onHint() {
        val current = _uiState.value
        val state = current.game ?: return
        if (!current.isPlaying) return
        when (val result = classic.requestHint(state)) {
            is HintResult.Shown -> {
                _uiState.update { it.withGame(result.state).copy(hint = result.move) }
                writes.trySend(Write.Save(result.state))
                analytics.log(AnalyticsEvent(AnalyticsEvents.HINT_USED, mapOf(AnalyticsEvents.Params.SOURCE to "free")))
            }
            HintResult.NoHintsRemaining -> _uiState.update { it.copy(message = GameMessage.NO_HINTS_LEFT) }
            HintResult.NoValidMove -> _uiState.update { it.copy(message = GameMessage.NO_VALID_MOVE) }
        }
    }

    fun onPause() {
        _uiState.update { if (it.game != null && !it.game.isGameOver) it.copy(isPaused = true) else it }
    }

    fun onResume() {
        _uiState.update { it.copy(isPaused = false) }
    }

    /** Abandons the current game (its score still counts towards the best) and starts a new one. */
    fun onRestart() {
        val current = _uiState.value
        val state = current.game ?: return
        if (!state.isGameOver) writes.trySend(Write.Finish(state.score, state.linesCleared, bestCombo, state.seed))
        bestCombo = 0
        bestAtStart = current.displayBest
        _uiState.update {
            GameUiState(isLoading = false, bestScore = current.displayBest, settings = it.settings)
                .withGame(classic.newGame(seeds.nextSeed()))
        }
        logGameStarted(resumed = false)
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        // Let queued writes finish on the application scope, then stop the writer.
        writes.close()
    }

    private fun GameUiState.withGame(state: GameState) =
        copy(game = state, slotsThatFit = classic.slotsThatFit(state), hint = null, message = null)

    private fun logGameStarted(resumed: Boolean) = analytics.log(
        AnalyticsEvent(
            AnalyticsEvents.GAME_STARTED,
            mapOf(AnalyticsEvents.Params.MODE to GameMode.CLASSIC.analyticsName, AnalyticsEvents.Params.RESUMED to resumed),
        ),
    )
}
