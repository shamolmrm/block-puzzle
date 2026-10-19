package com.puzzleblockblast.game.feature.game.tutorial

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.audio.SoundEffects
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.engine.board.Board
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.ClassicGame
import com.puzzleblockblast.game.core.engine.classic.GameState
import com.puzzleblockblast.game.core.engine.classic.GameStatus
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.MoveResult
import com.puzzleblockblast.game.core.engine.classic.Piece
import com.puzzleblockblast.game.core.engine.random.Pcg32
import com.puzzleblockblast.game.core.engine.shape.ShapeCatalog
import com.puzzleblockblast.game.core.model.UserSettings
import com.puzzleblockblast.game.feature.game.GameMessage
import com.puzzleblockblast.game.feature.game.GameUiState
import com.puzzleblockblast.game.feature.game.R
import com.puzzleblockblast.game.feature.game.soundFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One scripted lesson: a prepared board, a single piece and the one move that completes it. */
internal data class TutorialStep(val board: Board, val piece: Piece, val target: GridPoint, @param:StringRes val text: Int)

internal object TutorialScript {
    /** A few leftover blocks in the corner keep each lesson from emptying the board (no board-clear bonus). */
    private val leftovers = setOf(8 to 0, 9 to 0, 9 to 1)

    val steps: List<TutorialStep> = listOf(
        TutorialStep(boardWith { r, c -> r == 5 && c < 7 }, piece("i3_0", 4), GridPoint(5, 7), R.string.tutorial_step_row),
        TutorialStep(boardWith { r, c -> c == 4 && r < 7 }, piece("i3_1", 5), GridPoint(7, 4), R.string.tutorial_step_column),
        TutorialStep(
            boardWith { r, c -> (r == 6 || c == 5) && !(r == 6 && c == 5) },
            piece("dot_0", 2),
            GridPoint(6, 5),
            R.string.tutorial_step_double,
        ),
    )

    private fun piece(id: String, tile: Int) = Piece(requireNotNull(ShapeCatalog.Classic[id]), tile)

    private fun boardWith(filled: (row: Int, col: Int) -> Boolean) = Board.fromTiles(
        10,
        10,
        IntArray(100) { i ->
            val (r, c) = i / 10 to i % 10
            if (filled(r, c) || (r to c) in leftovers) 1 + (i * 3) % 7 else Board.EMPTY
        },
    )
}

data class TutorialUiState(
    val stepIndex: Int,
    val stepCount: Int,
    @param:StringRes val instruction: Int,
    val game: GameUiState,
    val finished: Boolean = false,
)

/**
 * Guided first game. Each step accepts only its target move, played through the real engine so
 * the clear and the scoring look exactly like Classic. Finishing or skipping marks it completed.
 */
@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val classic: ClassicGame,
    private val settingsRepository: SettingsRepository,
    private val analytics: AnalyticsTracker,
    private val sounds: SoundEffects,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(stateFor(0, UserSettings()))
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    private var advancing = false
    private var completed = false

    init {
        analytics.log(AnalyticsEvent(AnalyticsEvents.TUTORIAL_STARTED))
        viewModelScope.launch {
            settingsRepository.settings.collect { s -> _uiState.update { it.copy(game = it.game.copy(settings = s)) } }
        }
    }

    fun onPlace(slot: Int, origin: GridPoint) {
        val current = _uiState.value
        if (current.finished || advancing) return
        val step = TutorialScript.steps[current.stepIndex]
        if (slot != 0 || origin != step.target) {
            _uiState.update { it.copy(game = it.game.copy(message = GameMessage.TUTORIAL_TRY_TARGET)) }
            return
        }
        val state = current.game.game ?: return
        val result = classic.place(state, Move(slot, origin)) as? MoveResult.Accepted ?: return
        if (current.game.settings.soundEnabled) sounds.play(soundFor(result.outcome))

        // Hide the pieces the engine dealt after the tray emptied: the next lesson brings its own.
        val shown = result.state.copy(tray = List(3) { null })
        _uiState.update {
            it.copy(
                game = it.game.copy(
                    game = shown,
                    slotsThatFit = List(3) { false },
                    hint = null,
                    lastOutcome = result.outcome,
                    message = null,
                ),
            )
        }
        advancing = true
        viewModelScope.launch {
            delay(STEP_PAUSE_MS)
            advancing = false
            val next = current.stepIndex + 1
            _uiState.update {
                if (next < TutorialScript.steps.size) {
                    stateFor(next, it.game.settings, score = shown.score)
                } else {
                    it.copy(finished = true)
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(game = it.game.copy(message = null)) }
    }

    /** Called once when the player finishes or skips; safe to call again. */
    fun onComplete(skipped: Boolean) {
        if (completed) return
        completed = true
        analytics.log(AnalyticsEvent(AnalyticsEvents.TUTORIAL_COMPLETED, mapOf(AnalyticsEvents.Params.SKIPPED to skipped)))
        // Application scope: the screen is closing, but the flag must still be written.
        appScope.launch { settingsRepository.setTutorialCompleted() }
    }

    /** [score] carries over between lessons so the player sees their points add up. */
    private fun stateFor(index: Int, settings: UserSettings, score: Int = 0): TutorialUiState {
        val step = TutorialScript.steps[index]
        val game = GameState(
            seed = 0L,
            board = step.board,
            tray = listOf(step.piece, null, null),
            score = score,
            combo = 0,
            moves = index,
            linesCleared = 0,
            rng = Pcg32.seeded(index.toLong()).snapshot(),
            status = GameStatus.PLAYING,
            undosRemaining = 0,
            hintsRemaining = 0,
            previous = null,
        )
        return TutorialUiState(
            stepIndex = index,
            stepCount = TutorialScript.steps.size,
            instruction = step.text,
            game = GameUiState(
                isLoading = false,
                game = game,
                slotsThatFit = listOf(true, false, false),
                hint = Move(0, step.target),
                settings = settings,
            ),
        )
    }

    private companion object {
        const val STEP_PAUSE_MS = 1_100L
    }
}
