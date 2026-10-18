package com.puzzleblockblast.game.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.analytics.logSettingChanged
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingToggle(val analyticsName: String) {
    SOUND("sound"),
    MUSIC("music"),
    HAPTICS("haptics"),
    REDUCED_MOTION("reduced_motion"),
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setToggle(toggle: SettingToggle, enabled: Boolean) {
        viewModelScope.launch {
            when (toggle) {
                SettingToggle.SOUND -> repository.setSoundEnabled(enabled)
                SettingToggle.MUSIC -> repository.setMusicEnabled(enabled)
                SettingToggle.HAPTICS -> repository.setHapticsEnabled(enabled)
                SettingToggle.REDUCED_MOTION -> repository.setReducedMotion(enabled)
            }
            analytics.logSettingChanged(toggle.analyticsName, enabled)
        }
    }
}
