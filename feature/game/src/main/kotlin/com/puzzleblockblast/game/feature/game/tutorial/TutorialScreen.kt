package com.puzzleblockblast.game.feature.game.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.engine.board.GridPoint
import com.puzzleblockblast.game.feature.game.GameScreen
import com.puzzleblockblast.game.feature.game.OverlayScrim
import com.puzzleblockblast.game.feature.game.R

@Composable
fun TutorialRoute(
    onDone: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TutorialScreen(
        state = state,
        onPlace = viewModel::onPlace,
        onMessageShown = viewModel::onMessageShown,
        onSkip = {
            viewModel.onComplete(skipped = true)
            onDone()
        },
        onPlay = {
            viewModel.onComplete(skipped = false)
            onDone()
        },
    )
}

@Composable
fun TutorialScreen(
    state: TutorialUiState,
    onPlace: (slot: Int, origin: GridPoint) -> Unit,
    onMessageShown: () -> Unit,
    onSkip: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        GameScreen(
            state = state.game,
            onPlace = onPlace,
            onUndo = {},
            onHint = {},
            onPause = {},
            onResume = {},
            onRestart = {},
            onMessageShown = onMessageShown,
            onHome = onSkip,
            showControls = false,
            header = {
                TutorialHeader(
                    step = state.stepIndex + 1,
                    count = state.stepCount,
                    instruction = stringResource(state.instruction),
                    onSkip = onSkip,
                )
            },
        )
        if (state.finished) TutorialDoneOverlay(onPlay)
    }
}

@Composable
private fun TutorialHeader(step: Int, count: Int, instruction: String, onSkip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.tutorial_step_of, step, count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                instruction,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.tutorial_skip))
        }
    }
}

@Composable
private fun TutorialDoneOverlay(onPlay: () -> Unit) {
    OverlayScrim {
        Text(
            stringResource(R.string.tutorial_done_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.tutorial_done_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.tutorial_play))
        }
    }
}
