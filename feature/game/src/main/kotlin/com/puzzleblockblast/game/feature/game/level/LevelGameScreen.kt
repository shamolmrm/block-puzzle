package com.puzzleblockblast.game.feature.game.level

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.data.progress.Reward
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.designsystem.component.RewardSummary
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.core.engine.level.LevelResult
import com.puzzleblockblast.game.core.engine.level.Objective
import com.puzzleblockblast.game.core.engine.level.ObjectiveProgress
import com.puzzleblockblast.game.feature.game.GameBanner
import com.puzzleblockblast.game.feature.game.GameScreen
import com.puzzleblockblast.game.feature.game.OverlayScrim
import com.puzzleblockblast.game.feature.game.PauseButton
import com.puzzleblockblast.game.feature.game.R

@Composable
fun LevelGameRoute(
    onLevels: () -> Unit,
    onNextLevel: (levelId: String) -> Unit,
    viewModel: LevelGameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onPause() }
    LevelGameScreen(
        state = state,
        onPlace = viewModel::onPlace,
        onUndo = viewModel::onUndo,
        onHint = viewModel::onHint,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onRestart = viewModel::onRestart,
        onMessageShown = viewModel::onMessageShown,
        onLevels = onLevels,
        onNextLevel = onNextLevel,
        onWatchAdForHint = { activity?.let(viewModel::onWatchAdForHint) },
        // Leaving a finished level is a natural break: an interstitial may show (paced, consented).
        throughBreak = { action -> if (activity != null) viewModel.onBreak(activity, action) else action() },
        banner = { visible ->
            val allowed by viewModel.bannersAllowed.collectAsStateWithLifecycle()
            if (allowed) GameBanner(visible = visible, create = viewModel::createBanner)
        },
    )
}

@Composable
fun LevelGameScreen(
    state: LevelUiState,
    onPlace: (slot: Int, origin: GridPoint) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMessageShown: () -> Unit,
    onLevels: () -> Unit,
    onNextLevel: (levelId: String) -> Unit,
    modifier: Modifier = Modifier,
    onWatchAdForHint: () -> Unit = {},
    /** Runs a result-card action, possibly after an interstitial. */
    throughBreak: (() -> Unit) -> Unit = { it() },
    /** Ad banner above the game; told to hide while a result card covers the screen. */
    banner: (@Composable (visible: Boolean) -> Unit)? = null,
) {
    if (state.notFound) {
        LevelUnavailable(onLevels, modifier)
        return
    }
    Box(modifier) {
        GameScreen(
            state = state.game,
            onPlace = onPlace,
            onUndo = onUndo,
            onHint = onHint,
            onPause = onPause,
            onResume = onResume,
            onRestart = onRestart,
            onMessageShown = onMessageShown,
            onHome = onLevels,
            showGameOver = false,
            header = { LevelHeader(state, onPause) },
            onWatchAdForHint = onWatchAdForHint,
            banner = if (banner != null) {
                { banner(state.result == LevelResult.IN_PROGRESS) }
            } else {
                null
            },
        )
        when (state.result) {
            LevelResult.WON -> LevelCompleteOverlay(
                stars = state.stars,
                score = state.game.game?.score ?: 0,
                moves = state.game.game?.moves ?: 0,
                nextLevelId = state.nextLevelId,
                isDaily = state.isDaily,
                reward = state.reward,
                onNext = { id -> throughBreak { onNextLevel(id) } },
                onReplay = { throughBreak(onRestart) },
                onLevels = { throughBreak(onLevels) },
            )
            LevelResult.OUT_OF_MOVES, LevelResult.NO_MOVES -> LevelFailedOverlay(
                outOfMoves = state.result == LevelResult.OUT_OF_MOVES,
                onRetry = { throughBreak(onRestart) },
                onLevels = { throughBreak(onLevels) },
            )
            LevelResult.IN_PROGRESS -> Unit
        }
    }
}

@Composable
private fun LevelHeader(state: LevelUiState, onPause: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PauseButton(enabled = state.game.isPlaying, onPause = onPause)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (state.dailyEpochDay != null) {
                    stringResource(R.string.daily_title, formatEpochDay(state.dailyEpochDay))
                } else {
                    stringResource(R.string.level_title, state.level?.index ?: 0)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                state.objectives.forEach { ObjectiveChip(it) }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                "${state.movesLeft}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (state.movesLeft <= 3) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                pluralStringResource(R.plurals.level_moves_left, state.movesLeft),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ObjectiveChip(progress: ObjectiveProgress) {
    val shown = minOf(progress.current, progress.objective.target)
    val label = when (progress.objective) {
        is Objective.ScoreAtLeast -> stringResource(R.string.level_goal_score, shown, progress.objective.target)
        is Objective.ClearLines -> stringResource(R.string.level_goal_lines, shown, progress.objective.target)
        is Objective.ClearGems -> stringResource(R.string.level_goal_gems, shown, progress.objective.target)
    }
    val done = progress.isMet
    Text(
        text = if (done) "✓ $label" else label,
        style = MaterialTheme.typography.labelLarge,
        color = if (done) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(
                if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun StarRow(stars: Int) {
    val description = stringResource(R.string.level_stars_description, stars)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(3) { i ->
            Text(
                "★",
                fontSize = 44.sp,
                color = if (i < stars) Color(0xFFFFC145) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    stars: Int,
    score: Int,
    moves: Int,
    nextLevelId: String?,
    isDaily: Boolean,
    reward: Reward?,
    onNext: (String) -> Unit,
    onReplay: () -> Unit,
    onLevels: () -> Unit,
) {
    OverlayScrim {
        Text(
            stringResource(R.string.level_complete),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        StarRow(stars)
        Text(
            pluralStringResource(R.plurals.level_result_summary, moves, score, moves),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (reward != null) RewardSummary(reward.xp, reward.newAchievements)
        if (isDaily) {
            Text(stringResource(R.string.daily_come_back), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        } else if (nextLevelId != null) {
            Button(onClick = { onNext(nextLevelId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.level_next))
            }
        } else {
            Text(stringResource(R.string.level_all_done), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        OutlinedButton(onClick = onReplay, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.level_replay))
        }
        OutlinedButton(onClick = onLevels, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.level_back_to_levels))
        }
    }
}

@Composable
private fun LevelFailedOverlay(outOfMoves: Boolean, onRetry: () -> Unit, onLevels: () -> Unit) {
    OverlayScrim {
        Text(
            stringResource(if (outOfMoves) R.string.level_failed_moves else R.string.level_failed_space),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.level_failed_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.level_retry))
        }
        OutlinedButton(onClick = onLevels, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.level_back_to_levels))
        }
    }
}

@Composable
private fun LevelUnavailable(onLevels: () -> Unit, modifier: Modifier = Modifier) {
    PbbBackground(modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.level_unavailable), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Button(onClick = onLevels) { Text(stringResource(R.string.level_back_to_levels)) }
        }
    }
}

/** Formats an epoch day (a calendar date, no time zone) in the user's locale, e.g. "Oct 19, 2026". */
internal fun formatEpochDay(epochDay: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochDay * WallClock.MILLIS_PER_DAY))