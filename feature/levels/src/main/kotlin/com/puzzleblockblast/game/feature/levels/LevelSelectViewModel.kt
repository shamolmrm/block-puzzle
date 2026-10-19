package com.puzzleblockblast.game.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.data.level.LevelProgressRepository
import com.puzzleblockblast.game.core.data.level.LevelRepository
import com.puzzleblockblast.game.core.data.level.isUnlocked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LevelTile(val id: String, val index: Int, val stars: Int, val unlocked: Boolean)

data class LevelSelectUiState(
    val isLoading: Boolean = true,
    val tiles: List<LevelTile> = emptyList(),
    val totalStars: Int = 0,
) {
    val maxStars: Int get() = tiles.size * 3

    /** The first unlocked level without stars: where the player should go next. */
    val nextUp: LevelTile? get() = tiles.firstOrNull { it.unlocked && it.stars == 0 }
}

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    levels: LevelRepository,
    progress: LevelProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<LevelSelectUiState> =
        combine(flow { emit(levels.levels()) }, progress.progress) { all, done ->
            val tiles = all.map { level ->
                LevelTile(
                    id = level.id,
                    index = level.index,
                    stars = done[level.id]?.stars ?: 0,
                    unlocked = isUnlocked(all, level.index, done),
                )
            }
            LevelSelectUiState(isLoading = false, tiles = tiles, totalStars = tiles.sumOf { it.stars })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LevelSelectUiState())
}
