package com.puzzleblockblast.game.feature.game

import androidx.lifecycle.ViewModel
import com.puzzleblockblast.game.core.engine.board.Board
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GameUiState(
    val board: Board = Board.empty(),
    val score: Int = 0,
    val bestScore: Int = 0,
)

/** Phase 1 skeleton: exposes an empty board. Move handling, tray and persistence arrive in Phase 2. */
@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
}
