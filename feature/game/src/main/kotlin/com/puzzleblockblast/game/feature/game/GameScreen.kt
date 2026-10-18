package com.puzzleblockblast.game.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.feature.game.board.BoardCanvas

@Composable
fun GameRoute(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameScreen(state = state, onBack = onBack)
}

@Composable
fun GameScreen(
    state: GameUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.game_back)) }
                Text(
                    "${stringResource(R.string.game_score)} ${state.score}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${stringResource(R.string.game_best)} ${state.bestScore}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            BoardCanvas(board = state.board, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
            Text(stringResource(R.string.game_placeholder), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun GameScreenPreview() {
    PbbTheme { GameScreen(state = GameUiState(), onBack = {}) }
}
