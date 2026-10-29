package com.puzzleblockblast.game.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.data.level.EventRepository
import com.puzzleblockblast.game.core.data.level.LevelProgressRepository
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.engine.level.EventSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EventUiState(
    val isLoading: Boolean = true,
    /** Null once loaded means no event content is available. */
    val title: String? = null,
    val daysLeft: Int = 0,
    /** Tile ids are this week's run ids (e.g. `E01L03@2915`): progress resets every run. */
    val tiles: List<LevelTile> = emptyList(),
) {
    val completed: Int get() = tiles.count { it.stars > 0 }
    val isFinished: Boolean get() = tiles.isNotEmpty() && completed == tiles.size
    val nextUp: LevelTile? get() = tiles.firstOrNull { it.unlocked && it.stars == 0 }
}

@HiltViewModel
class EventViewModel @Inject constructor(
    events: EventRepository,
    progress: LevelProgressRepository,
    clock: WallClock,
) : ViewModel() {

    val uiState: StateFlow<EventUiState> = combine(flow { emit(events.events()) }, progress.progress) { all, done ->
        val today = clock.todayEpochDay()
        val active = EventSchedule.active(all, today) ?: return@combine EventUiState(isLoading = false)
        var previousDone = true
        val tiles = active.event.levels.map { level ->
            val runId = active.runLevelId(level)
            val stars = done[runId]?.stars ?: 0
            // Levels open in order within the run.
            LevelTile(id = runId, index = level.index, stars = stars, unlocked = previousDone).also { previousDone = stars > 0 }
        }
        EventUiState(isLoading = false, title = active.event.title, daysLeft = active.daysLeft(today), tiles = tiles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventUiState())
}
