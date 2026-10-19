package com.puzzleblockblast.game.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.model.progress.PlayerSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    progression: ProgressionRepository,
    clock: WallClock,
) : ViewModel() {
    /** Null until loaded. */
    val snapshot: StateFlow<PlayerSnapshot?> = progression.snapshot(clock.todayEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
