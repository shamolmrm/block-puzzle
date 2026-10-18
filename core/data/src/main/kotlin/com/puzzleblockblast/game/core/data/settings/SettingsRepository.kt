package com.puzzleblockblast.game.core.data.settings

import com.puzzleblockblast.game.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
}
