package com.puzzleblockblast.game.feature.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground

@Composable
fun EventRoute(
    onOpenLevel: (runLevelId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EventScreen(state = state, onOpenLevel = onOpenLevel, onBack = onBack)
}

@Composable
fun EventScreen(
    state: EventUiState,
    onOpenLevel: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PbbBackground(modifier) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.levels_back)) }
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.title == null -> Text(
                    stringResource(R.string.event_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
                else -> {
                    EventHeader(state)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.widthIn(max = 560.dp).padding(vertical = 16.dp),
                    ) {
                        items(state.tiles, key = { it.id }) { tile ->
                            LevelTileButton(tile, isNext = tile.id == state.nextUp?.id, onClick = { onOpenLevel(tile.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventHeader(state: EventUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.event_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            state.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.semantics { heading() },
        )
        Text(pluralStringResource(R.plurals.event_days_left, state.daysLeft, state.daysLeft), style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { if (state.tiles.isEmpty()) 0f else state.completed.toFloat() / state.tiles.size },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.tertiary,
            drawStopIndicator = {},
        )
        Text(
            if (state.isFinished) stringResource(R.string.event_finished) else stringResource(R.string.event_progress, state.completed, state.tiles.size),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
