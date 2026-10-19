package com.puzzleblockblast.game.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.data.progress.ProgressionRepository
import com.puzzleblockblast.game.core.data.time.WallClock
import com.puzzleblockblast.game.core.model.progress.PlayerLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeProfile(val level: PlayerLevel, val dailyDoneToday: Boolean, val streak: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(
    progression: ProgressionRepository,
    private val clock: WallClock,
) : ViewModel() {

    val profile: StateFlow<HomeProfile?> = clock.todayEpochDay().let { today ->
        combine(progression.snapshot(today), progression.dailyResults) { snapshot, daily ->
            HomeProfile(
                level = snapshot.playerLevel,
                dailyDoneToday = daily[today]?.isCompleted == true,
                streak = snapshot.currentStreak,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Today's Daily Challenge, read when the player taps it (the date may have changed since launch). */
    fun todayEpochDay(): Long = clock.todayEpochDay()
}
