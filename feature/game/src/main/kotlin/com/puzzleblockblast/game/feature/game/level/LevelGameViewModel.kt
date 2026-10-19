package com.puzzleblockblast.game.feature.game.level

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.audio.Sound
import com.puzzleblockblast.game.core.audio.SoundEffects
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.level.LevelProgressRepository
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.progress.Reward
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.classic.HintResult
import com.puzzleblockblast.game.core.engine.classic.Move
import com.puzzleblockblast.game.core.engine.classic.MoveOutcome
import com.puzzleblockblast.game.core.engine.level.DailyChallenge
import com.puzzleblockblast.game.core.engine.level.LevelDefinition
import com.puzzleblockblast.game.core.engine.level.LevelGame
import com.puzzleblockblast.game.core.engine.level.LevelMoveResult
import com.puzzleblockblast.game.core.engine.level.LevelResult
import com.puzzleblockblast.game.core.engine.level.LevelState
import com.puzzleblockblast.game.core.engine.level.ObjectiveProgress
import com.puzzleblockblast.game.core.model.GameMode
import com.puzzleblockblast.game.core.monetization.AdsController
import com.puzzleblockblast.game.feature.game.GameMessage
import com.puzzleblockblast.game.feature.game.GameUiState
import com.puzzleblockblast.game.feature.game.soundFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelUiState(
    val game: GameUiState = GameUiState(),
    val level: LevelDefinition? = null,
    /** The requested level isn't in the content (e.g. a stale link): show an error, not a crash. */
    val notFound: Boolean = false,
    /** Set for a Daily Challenge: its epoch day. */
    val dailyEpochDay: Long? = null,
    val result: LevelResult = LevelResult.IN_PROGRESS,
    val objectives: List<ObjectiveProgress> = emptyList(),
    val movesLeft: Int = 0,
    val stars: Int = 0,
    val nextLevelId: String? = null,
    /** XP and achievements earned by the finished attempt, once recorded. */
    val reward: Reward? = null,
) {
    val isDaily: Boolean get() = dailyEpochDay != null
}

/**
 * Plays one pack level or a Daily Challenge (level id `D<epoch day>`, generated on the device). An
 * unfinished attempt is not saved: levels are short, and leaving restarts it. Results are written
 * on the application scope so leaving the screen can't drop them.
 */
@HiltViewModel
class LevelGameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val levels: LevelRepository,
    private val progress: LevelProgressRepository,
    private val progression: ProgressionRepository,
    private val analytics: AnalyticsTracker,
    private val sounds: SoundEffects,
    private val clock: WallClock,
    settingsRepository: SettingsRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val ads: AdsController,
) : ViewModel() {

    private val levelId: String = requireNotNull(savedStateHandle[LEVEL_ID_KEY]) { "levelId missing" }
    private val dailyDay: Long? = DailyChallenge.epochDayOf(levelId)

    private val _uiState = MutableStateFlow(LevelUiState(dailyEpochDay = dailyDay))
    val uiState: StateFlow<LevelUiState> = _uiState.asStateFlow()

    private var engine: LevelGame? = null
    private var current: LevelState? = null
    private var bestCombo = 0

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s -> _uiState.update { it.copy(game = it.game.copy(settings = s)) } }
        }
        viewModelScope.launch {
            ads.rewardedAvailable.collect { ready -> _uiState.update { it.copy(game = it.game.copy(rewardedReady = ready)) } }
        }
        viewModelScope.launch {
            val (level, next) = if (dailyDay != null) {
                DailyChallenge.levelFor(dailyDay) to null
            } else {
                val all = levels.levels()
                val index = all.indexOfFirst { it.id == levelId }
                all.getOrNull(index) to all.getOrNull(index + 1)?.id
            }
            if (level == null) {
                _uiState.update { it.copy(notFound = true, game = it.game.copy(isLoading = false)) }
                return@launch
            }
            engine = LevelGame(level)
            _uiState.update { it.copy(level = level, nextLevelId = next) }
            startAttempt()
        }
    }

    fun onPlace(slot: Int, origin: GridPoint) {
        val game = engine ?: return
        val state = current ?: return
        if (!_uiState.value.game.isPlaying) return
        val result = game.place(state, Move(slot, origin)) as? LevelMoveResult.Accepted ?: return
        val next = result.state
        bestCombo = maxOf(bestCombo, result.outcome.score.combo)

        if (_uiState.value.game.settings.soundEnabled) {
            sounds.play(
                when (next.result) {
                    LevelResult.WON -> Sound.BOARD_CLEAR
                    LevelResult.OUT_OF_MOVES, LevelResult.NO_MOVES -> Sound.GAME_OVER
                    LevelResult.IN_PROGRESS -> soundFor(result.outcome)
                },
            )
        }
        publish(next, result.outcome)
        if (next.isFinished) recordEnd(game, next)
    }

    fun onUndo() {
        val game = engine ?: return
        if (!_uiState.value.game.canUndo) return
        val undone = game.undo(current ?: return) ?: return
        publish(undone, outcome = null)
        analytics.log(AnalyticsEvent(AnalyticsEvents.UNDO_USED, mapOf(AnalyticsEvents.Params.SOURCE to "free")))
    }

    fun onHint() {
        val game = engine ?: return
        val state = current ?: return
        if (!_uiState.value.game.isPlaying) return
        when (val result = game.requestHint(state)) {
            is HintResult.Shown -> {
                publish(state.copy(game = result.state), outcome = _uiState.value.game.lastOutcome)
                _uiState.update { it.copy(game = it.game.copy(hint = result.move)) }
                analytics.log(AnalyticsEvent(AnalyticsEvents.HINT_USED, mapOf(AnalyticsEvents.Params.SOURCE to "free")))
            }
            HintResult.NoHintsRemaining -> setMessage(GameMessage.NO_HINTS_LEFT)
            HintResult.NoValidMove -> setMessage(GameMessage.NO_VALID_MOVE)
        }
    }

    fun onPause() {
        _uiState.update { if (it.game.isPlaying) it.copy(game = it.game.copy(isPaused = true)) else it }
    }

    fun onResume() {
        _uiState.update { it.copy(game = it.game.copy(isPaused = false)) }
    }

    /** Starts a fresh attempt at the same level (also used for "Retry" and "Replay"). */
    fun onRestart() {
        if (engine == null) return
        _uiState.update { it.copy(game = it.game.copy(isPaused = false)) }
        startAttempt()
    }

    fun onMessageShown() = setMessage(null)

    /** Rewarded hint (pack levels only; the Daily stays identical for everyone). Granted only on a confirmed reward. */
    fun onWatchAdForHint(activity: Activity) {
        val game = engine ?: return
        if (!_uiState.value.game.rewardedHintOffer || adInProgress) return
        adInProgress = true
        viewModelScope.launch {
            try {
                if (!ads.watchRewarded(activity, "level_hint")) return@launch
                val granted = game.grantHint(current ?: return@launch) ?: return@launch
                current = granted
                _uiState.update { it.copy(game = it.game.copy(game = granted.game, adHintsLeft = it.game.adHintsLeft - 1)) }
            } finally {
                adInProgress = false
            }
        }
    }

    private var adInProgress = false

    /** A natural break (leaving a finished level): an interstitial may show, then [then] runs. */
    fun onBreak(activity: Activity, then: () -> Unit) {
        viewModelScope.launch {
            ads.atBreak(activity)
            then()
        }
    }

    private fun startAttempt() {
        val game = engine ?: return
        bestCombo = 0
        _uiState.update {
            it.copy(
                reward = null,
                game = it.game.copy(adHintsLeft = if (dailyDay != null) 0 else ads.policy.rewardedHintsPerGame, adUndosLeft = 0),
            )
        }
        publish(game.start(), outcome = null)
        appScope.launch {
            if (dailyDay != null) progression.recordDailyAttempt(dailyDay) else progress.recordAttempt(levelId)
        }
        logLevel(AnalyticsEvents.LEVEL_STARTED)
    }

    private fun recordEnd(game: LevelGame, state: LevelState) {
        val stars = game.stars(state)
        val score = state.game.score
        val lines = state.game.linesCleared
        val combo = bestCombo
        val at = clock.nowMillis()
        val today = clock.todayEpochDay()
        appScope.launch {
            val reward = progression.recordLevelEnd(levelId, stars, score, lines, combo, at, today)
            // Only attach the reward if the player hasn't already started another attempt.
            _uiState.update { if (it.result == state.result && it.game.game == state.game) it.copy(reward = reward) else it }
        }
        when {
            state.result != LevelResult.WON -> logLevel(
                AnalyticsEvents.LEVEL_FAILED,
                AnalyticsEvents.Params.REASON to state.result.name.lowercase(),
            )
            dailyDay != null -> logLevel(AnalyticsEvents.DAILY_CHALLENGE_COMPLETED, AnalyticsEvents.Params.STARS to stars)
            else -> logLevel(
                AnalyticsEvents.LEVEL_COMPLETED,
                AnalyticsEvents.Params.STARS to stars,
                AnalyticsEvents.Params.MOVES to state.game.moves,
            )
        }
    }

    private fun publish(state: LevelState, outcome: MoveOutcome?) {
        val game = engine ?: return
        current = state
        _uiState.update {
            it.copy(
                game = it.game.copy(
                    isLoading = false,
                    game = state.game,
                    slotsThatFit = game.slotsThatFit(state),
                    hint = null,
                    message = null,
                    lastOutcome = outcome,
                    isEnded = state.isFinished,
                ),
                result = state.result,
                objectives = game.progress(state.game),
                movesLeft = game.movesLeft(state),
                stars = game.stars(state),
            )
        }
    }

    private fun setMessage(message: GameMessage?) {
        _uiState.update { it.copy(game = it.game.copy(message = message)) }
    }

    private fun logLevel(name: String, vararg extra: Pair<String, Any>) {
        val mode = if (dailyDay != null) GameMode.DAILY else GameMode.LEVEL
        analytics.log(
            AnalyticsEvent(
                name,
                mapOf(AnalyticsEvents.Params.MODE to mode.analyticsName, AnalyticsEvents.Params.LEVEL_ID to levelId) + extra,
            ),
        )
    }

    companion object {
        /** Matches the `levelId` property of the navigation route. */
        const val LEVEL_ID_KEY = "levelId"
    }
}
