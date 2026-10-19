package com.puzzleblockblast.game.feature.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.puzzleblockblast.game.core.monetization.ads.AdsGateway
import com.puzzleblockblast.game.core.monetization.billing.BillingGateway
import com.puzzleblockblast.game.core.monetization.billing.Entitlements
import com.puzzleblockblast.game.core.monetization.billing.PurchaseState
import com.puzzleblockblast.game.core.monetization.billing.StoreOffer
import androidx.lifecycle.viewModelScope
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.analytics.logSettingChanged
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.data.sync.CloudBackup
import com.puzzleblockblast.game.core.data.sync.SyncResult
import com.puzzleblockblast.game.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingToggle(val analyticsName: String, val isPrivacy: Boolean = false) {
    SOUND("sound"),
    MUSIC("music"),
    HAPTICS("haptics"),
    REDUCED_MOTION("reduced_motion"),
    SHARE_USAGE("share_usage", isPrivacy = true),
    SHARE_CRASHES("share_crashes", isPrivacy = true),
}

data class BackupUiState(
    /** False when cloud backup isn't set up in this build (no Firebase config). */
    val available: Boolean = false,
    val lastSyncMillis: Long? = null,
    val syncing: Boolean = false,
    val lastAttemptFailed: Boolean = false,
)

data class StoreUiState(
    /** Null when the purchase can't be offered (not on the store yet, offline, no verification server). */
    val offer: StoreOffer? = null,
    /** Server-verified only. */
    val adsRemoved: Boolean = false,
    val purchase: PurchaseState = PurchaseState.Idle,
    /** Consent rules (e.g. EU) require a way to change ad privacy choices. */
    val privacyOptionsRequired: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val analytics: AnalyticsTracker,
    private val backup: CloudBackup,
    private val billing: BillingGateway,
    entitlements: Entitlements,
    private val ads: AdsGateway,
) : ViewModel() {

    val storeState: StateFlow<StoreUiState> = combine(
        billing.removeAdsOffer,
        entitlements.adsRemoved,
        billing.purchaseState,
        ads.privacyOptionsRequired,
    ) { offer, removed, purchase, privacy -> StoreUiState(offer, removed, purchase, privacy) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreUiState())

    fun buyRemoveAds(activity: Activity) {
        val state = storeState.value
        if (state.offer == null || state.adsRemoved) return
        billing.launchRemoveAdsPurchase(activity)
    }

    fun restorePurchases() = billing.restorePurchases()

    fun showAdPrivacyOptions(activity: Activity) = ads.showPrivacyOptions(activity)

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val syncState = MutableStateFlow(BackupUiState(available = backup.isAvailable))

    val backupState: StateFlow<BackupUiState> = combine(syncState, backup.lastSyncMillis) { state, last ->
        state.copy(lastSyncMillis = last)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState(available = backup.isAvailable))

    fun setToggle(toggle: SettingToggle, enabled: Boolean) {
        viewModelScope.launch {
            when (toggle) {
                SettingToggle.SOUND -> repository.setSoundEnabled(enabled)
                SettingToggle.MUSIC -> repository.setMusicEnabled(enabled)
                SettingToggle.HAPTICS -> repository.setHapticsEnabled(enabled)
                SettingToggle.REDUCED_MOTION -> repository.setReducedMotion(enabled)
                SettingToggle.SHARE_USAGE -> repository.setShareUsageData(enabled)
                SettingToggle.SHARE_CRASHES -> repository.setShareCrashReports(enabled)
            }
            // Opting out of data sharing is not itself reported.
            if (!toggle.isPrivacy) analytics.logSettingChanged(toggle.analyticsName, enabled)
        }
    }

    fun backUpNow() {
        if (!backup.isAvailable || syncState.value.syncing) return
        syncState.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = backup.sync()
            syncState.update { it.copy(syncing = false, lastAttemptFailed = result !is SyncResult.Synced) }
        }
    }
}
