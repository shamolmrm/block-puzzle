package com.puzzleblockblast.game.core.testing

import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state

    override suspend fun setSoundEnabled(enabled: Boolean) = state.update { it.copy(soundEnabled = enabled) }
    override suspend fun setMusicEnabled(enabled: Boolean) = state.update { it.copy(musicEnabled = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) = state.update { it.copy(hapticsEnabled = enabled) }
    override suspend fun setReducedMotion(enabled: Boolean) = state.update { it.copy(reducedMotion = enabled) }
}
