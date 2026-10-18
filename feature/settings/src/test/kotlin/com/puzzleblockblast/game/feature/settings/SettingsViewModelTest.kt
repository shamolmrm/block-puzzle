package com.puzzleblockblast.game.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.testing.FakeSettingsRepository
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val analytics = FakeAnalyticsTracker()
    private val viewModel = SettingsViewModel(repository, analytics)

    @Test
    fun toggleUpdatesStateAndLogsSettingsChanged() = runTest {
        viewModel.settings.test {
            assertThat(awaitItem().soundEnabled).isTrue()

            viewModel.setToggle(SettingToggle.SOUND, false)

            assertThat(awaitItem().soundEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        val event = analytics.events.single()
        assertThat(event.name).isEqualTo(AnalyticsEvents.SETTINGS_CHANGED)
        assertThat(event.params).containsExactly("setting", "sound", "enabled", false)
    }
}
