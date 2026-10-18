package com.puzzleblockblast.game.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Tests the repository's mapping and defaults against an in-memory DataStore. (File-backed DataStore
 * cannot overwrite its file on a Windows JVM host; real file I/O belongs in on-device tests.)
 */
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

    private class InMemoryDataStore<T>(initial: T) : DataStore<T> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<T> = state

        override suspend fun updateData(transform: suspend (T) -> T): T =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }
}
