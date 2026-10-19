package com.puzzleblockblast.game.feature.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme

@Composable
fun LevelSelectRoute(
    onOpenLevel: (levelId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LevelSelectScreen(state = state, onOpenLevel = onOpenLevel, onBack = onBack)
}

@Composable
fun LevelSelectScreen(
    state: LevelSelectUiState,
    onOpenLevel: (levelId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PbbBackground(modifier) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.levels_back)) }
                Text(
                    stringResource(R.string.levels_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    stringResource(R.string.levels_total_stars, state.totalStars, state.maxStars),
                    style = MaterialTheme.typography.titleMedium,
                    color = StarGold,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.tiles.isEmpty() -> Text(
                    stringResource(R.string.levels_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.widthIn(max = 560.dp).padding(bottom = 16.dp),
                ) {
                    items(state.tiles, key = { it.id }) { tile ->
                        LevelTileButton(tile, isNext = tile.id == state.nextUp?.id, onClick = { onOpenLevel(tile.id) })
                    }
                }
            }
        }
    }
}

private val StarGold = Color(0xFFFFC145)

@Composable
private fun LevelTileButton(tile: LevelTile, isNext: Boolean, onClick: () -> Unit) {
    val colors = PbbTheme.boardColors
    val description = when {
        !tile.unlocked -> stringResource(R.string.levels_tile_locked, tile.index)
        else -> stringResource(R.string.levels_tile, tile.index, tile.stars)
    }
    val shape = RoundedCornerShape(16.dp)
    val background = when {
        !tile.unlocked -> MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
        tile.stars > 0 -> colors.tile(tile.index % 7 + 1)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .then(if (isNext) Modifier.border(3.dp, StarGold, shape) else Modifier)
            .then(if (tile.unlocked) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (tile.unlocked) onClick { onClick(); true } else disabled()
            },
    ) {
        Text(
            if (tile.unlocked) "${tile.index}" else "🔒",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = if (tile.stars > 0) Color.White else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (tile.unlocked) 1f else 0.5f),
        )
        if (tile.unlocked) {
            Text(
                "★".repeat(tile.stars) + "☆".repeat(3 - tile.stars),
                fontSize = 12.sp,
                color = if (tile.stars > 0) Color.White else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
            )
        }
    }
}
