package com.puzzleblockblast.game.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.data.InMemoryDataStore
import com.puzzleblockblast.game.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class DataStoreSettingsRepositoryTest {

    @Test
    fun firstLaunchUsesDefaults() = runTest {
        val repo = DataStoreSettingsRepository(InMemoryDataStore(emptyPreferences()))

        assertThat(repo.settings.first()).isEqualTo(UserSettings())
    }

    @Test
    fun togglesArePersisted() = runTest {
        val repo = DataStoreSettingsRepository(InMemoryDataStore(emptyPreferences()))

        repo.setSoundEnabled(false)
        repo.setHapticsEnabled(false)
        repo.setReducedMotion(true)

        assertThat(repo.settings.first()).isEqualTo(
            UserSettings(soundEnabled = false, musicEnabled = true, hapticsEnabled = false, reducedMotion = true),
        )
    }

    @Test
    fun unreadableFileFallsBackToDefaults() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = error("unused")
        }

        assertThat(DataStoreSettingsRepository(broken).settings.first()).isEqualTo(UserSettings())
    }
}
