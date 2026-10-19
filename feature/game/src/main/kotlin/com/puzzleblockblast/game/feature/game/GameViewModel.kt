package com.puzzleblockblast.game.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.data.classic.ClassicGameRepository
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.HintResult
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.MoveOutcome
import com.puzzleblockblast.game.core.engine.classic.MoveResult
import com.puzzleblockblast.game.core.model.GameMode
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

enum class GameMessage { NO_HINTS_LEFT, NO_VALID_MOVE }

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
) {
    val isPlaying: Boolean get() = game != null && !game.isGameOver && !isPaused
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /** Best score when the current game started; decides "new best" at game over. */
    private var bestAtStart = 0

    /**
     * Writes run in order on the application scope, so a save requested just before the player
     * leaves the screen still completes. Unlimited (not conflated) so a finish is never dropped.
     */
    private val writes = Channel<Write>(Channel.UNLIMITED)

    private sealed interface Write {
        data class Save(val state: GameState) : Write
        data class Finish(val score: Int) : Write
    }

    init {
        appScope.launch {
            for (write in writes) {
                when (write) {
                    is Write.Save -> repository.saveInProgress(write.state)
                    is Write.Finish -> repository.finishGame(write.score)
                }
            }
        }
        viewModelScope.launch {
            val best = repository.bestScore.first()
            val restored = repository.loadInProgress()
            bestAtStart = best
            val state = restored ?: classic.newGame(seeds.nextSeed())
            _uiState.value = GameUiState(isLoading = false, bestScore = best).withGame(state)
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
        if (next.isGameOver) {
            _uiState.update {
                it.withGame(next).copy(lastOutcome = result.outcome, isNewBest = next.score > bestAtStart)
            }
            writes.trySend(Write.Finish(next.score))
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
        if (!state.isGameOver) writes.trySend(Write.Finish(state.score))
        bestAtStart = current.displayBest
        _uiState.update {
            GameUiState(isLoading = false, bestScore = current.displayBest).withGame(classic.newGame(seeds.nextSeed()))
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
