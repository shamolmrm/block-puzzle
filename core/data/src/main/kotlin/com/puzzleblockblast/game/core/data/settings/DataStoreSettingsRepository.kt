package com.puzzleblockblast.game.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.puzzleblockblast.game.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    @param:SettingsDataStore private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        // A corrupt/unreadable file must never crash the game: fall back to defaults.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val defaults = UserSettings()
            UserSettings(
                soundEnabled = prefs[Keys.SOUND] ?: defaults.soundEnabled,
                musicEnabled = prefs[Keys.MUSIC] ?: defaults.musicEnabled,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
                reducedMotion = prefs[Keys.REDUCED_MOTION] ?: defaults.reducedMotion,
            )
        }
        .distinctUntilChanged()

    override suspend fun setSoundEnabled(enabled: Boolean) = set(Keys.SOUND, enabled)
    override suspend fun setMusicEnabled(enabled: Boolean) = set(Keys.MUSIC, enabled)
    override suspend fun setHapticsEnabled(enabled: Boolean) = set(Keys.HAPTICS, enabled)
    override suspend fun setReducedMotion(enabled: Boolean) = set(Keys.REDUCED_MOTION, enabled)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val MUSIC = booleanPreferencesKey("music_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }
}
