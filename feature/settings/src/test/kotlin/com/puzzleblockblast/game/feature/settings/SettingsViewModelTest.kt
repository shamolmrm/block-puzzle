package com.puzzleblockblast.game.feature.settings

import com.google.common.truth.Truth.assertThat
import com.puzzleblockblast.game.core.analytics.AnalyticsEvents
import com.puzzleblockblast.game.core.data.sync.SyncResult
import com.puzzleblockblast.game.core.testing.FakeAnalyticsTracker
import com.puzzleblockblast.game.core.testing.FakeCloudBackup
import com.puzzleblockblast.game.core.testing.FakeSettingsRepository
import com.puzzleblockblast.game.core.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val analytics = FakeAnalyticsTracker()
    private val backup = FakeCloudBackup()

    /** Keeps the WhileSubscribed state flows active, then lets all work finish before asserting. */
    private fun TestScope.subscribed(vm: SettingsViewModel): SettingsViewModel {
        backgroundScope.launch { vm.settings.collect {} }
        backgroundScope.launch { vm.backupState.collect {} }
        return vm
    }

    @Test
    fun toggleUpdatesStateAndLogsSettingsChanged() = runTest {
        val vm = subscribed(SettingsViewModel(repository, analytics, backup))

        vm.setToggle(SettingToggle.SOUND, false)
        advanceUntilIdle()

        assertThat(vm.settings.value.soundEnabled).isFalse()
        val event = analytics.events.single()
        assertThat(event.name).isEqualTo(AnalyticsEvents.SETTINGS_CHANGED)
        assertThat(event.params).containsExactly("setting", "sound", "enabled", false)
    }

    @Test
    fun optingOutOfDataSharingIsNotReported() = runTest {
        val vm = subscribed(SettingsViewModel(repository, analytics, backup))

        vm.setToggle(SettingToggle.SHARE_USAGE, false)
        vm.setToggle(SettingToggle.SHARE_CRASHES, false)
        advanceUntilIdle()

        assertThat(vm.settings.value.shareUsageData).isFalse()
        assertThat(vm.settings.value.shareCrashReports).isFalse()
        assertThat(analytics.events).isEmpty()
    }

    @Test
    fun backUpNowSyncsAndShowsTheTime() = runTest {
        val vm = subscribed(SettingsViewModel(repository, analytics, backup))
        assertThat(vm.backupState.value.lastSyncMillis).isNull()

        vm.backUpNow()
        advanceUntilIdle()

        val state = vm.backupState.value
        assertThat(state.lastSyncMillis).isEqualTo(1_000L)
        assertThat(state.syncing).isFalse()
        assertThat(state.lastAttemptFailed).isFalse()
        assertThat(backup.syncCount).isEqualTo(1)
    }

    @Test
    fun failedBackupIsReported() = runTest {
        backup.result = SyncResult.Failed(RuntimeException("offline"))
        val vm = subscribed(SettingsViewModel(repository, analytics, backup))

        vm.backUpNow()
        advanceUntilIdle()

        assertThat(vm.backupState.value.lastAttemptFailed).isTrue()
    }

    @Test
    fun backupDoesNothingWhenUnavailable() = runTest {
        val unavailable = FakeCloudBackup(isAvailable = false)
        val vm = subscribed(SettingsViewModel(repository, analytics, unavailable))

        vm.backUpNow()
        advanceUntilIdle()

        assertThat(unavailable.syncCount).isEqualTo(0)
        assertThat(vm.backupState.value.available).isFalse()
    }
}
