package com.puzzleblockblast.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppState(val settings: UserSettings, val tutorialCompleted: Boolean)

/** App-wide state the UI shell needs before showing anything. Null until loaded (splash stays up). */
@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val appState: StateFlow<AppState?> =
        combine(settingsRepository.settings, settingsRepository.tutorialCompleted, ::AppState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
