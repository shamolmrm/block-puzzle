package com.puzzleblockblast.game.feature.settings

import android.text.format.DateUtils
import androidx.activity.compose.LocalActivity
import com.puzzleblockblast.game.core.monetization.billing.PurchaseState
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puzzleblockblast.game.core.designsystem.component.PbbBackground
import com.puzzleblockblast.game.core.designsystem.theme.PbbTheme
import com.puzzleblockblast.game.core.model.UserSettings

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backup by viewModel.backupState.collectAsStateWithLifecycle()
    val store by viewModel.storeState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    SettingsScreen(
        settings = settings,
        backup = backup,
        store = store,
        onToggle = viewModel::setToggle,
        onBackUpNow = viewModel::backUpNow,
        onBuyRemoveAds = { activity?.let(viewModel::buyRemoveAds) },
        onRestorePurchases = viewModel::restorePurchases,
        onAdPrivacyOptions = { activity?.let(viewModel::showAdPrivacyOptions) },
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onToggle: (SettingToggle, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backup: BackupUiState = BackupUiState(),
    onBackUpNow: () -> Unit = {},
    store: StoreUiState = StoreUiState(),
    onBuyRemoveAds: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onAdPrivacyOptions: () -> Unit = {},
) {
    PbbBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
            )
            ToggleRow(R.string.settings_sound, settings.soundEnabled) { onToggle(SettingToggle.SOUND, it) }
            // No music ships yet, so its toggle stays hidden rather than doing nothing.
            ToggleRow(R.string.settings_haptics, settings.hapticsEnabled) { onToggle(SettingToggle.HAPTICS, it) }
            ToggleRow(R.string.settings_reduced_motion, settings.reducedMotion) {
                onToggle(SettingToggle.REDUCED_MOTION, it)
            }

            SectionTitle(R.string.settings_privacy)
            ToggleRow(R.string.settings_share_usage, settings.shareUsageData, R.string.settings_share_usage_detail) {
                onToggle(SettingToggle.SHARE_USAGE, it)
            }
            ToggleRow(R.string.settings_share_crashes, settings.shareCrashReports, R.string.settings_share_crashes_detail) {
                onToggle(SettingToggle.SHARE_CRASHES, it)
            }
            if (store.privacyOptionsRequired) {
                TextButton(onClick = onAdPrivacyOptions, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.settings_ad_privacy))
                }
            }
            // Play's User Data policy requires the privacy policy to be reachable from inside the app.
            val uriHandler = LocalUriHandler.current
            TextButton(onClick = { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.settings_privacy_policy))
            }

            // Hidden when nothing is sold (this release has no "Remove ads"): no dead "store unavailable" row.
            if (store.offer != null || store.adsRemoved || store.purchase != PurchaseState.Idle) {
                SectionTitle(R.string.settings_store)
                StoreRow(store, onBuyRemoveAds, onRestorePurchases)
            }

            SectionTitle(R.string.settings_backup)
            BackupRow(backup, onBackUpNow)
        }
    }
}

/** Hosted on the project's Firebase Hosting site (firebase/public). */
internal const val PRIVACY_POLICY_URL = "https://blockfit-puzzle.web.app/privacy.html"

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp, start = 8.dp).semantics { heading() },
    )
}

/** Whole row is the touch target (≥ 56dp) and is announced as a single switch. */
@Composable
private fun ToggleRow(
    @StringRes label: Int,
    checked: Boolean,
    @StringRes detail: Int? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(stringResource(detail), style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun BackupRow(backup: BackupUiState, onBackUpNow: () -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        val status = when {
            !backup.available -> stringResource(R.string.settings_backup_unavailable)
            backup.syncing -> stringResource(R.string.settings_backup_syncing)
            backup.lastAttemptFailed -> stringResource(R.string.settings_backup_failed)
            backup.lastSyncMillis == null -> stringResource(R.string.settings_backup_never)
            else -> stringResource(
                R.string.settings_backup_last,
                DateUtils.getRelativeTimeSpanString(backup.lastSyncMillis).toString(),
            )
        }
        Text(status, style = MaterialTheme.typography.bodyMedium)
        if (backup.available) {
            FilledTonalButton(onClick = onBackUpNow, enabled = !backup.syncing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.settings_backup_now))
            }
        }
    }
}

@Composable
private fun StoreRow(store: StoreUiState, onBuy: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        val offer = store.offer
        when {
            store.adsRemoved -> Text(stringResource(R.string.settings_ads_removed), style = MaterialTheme.typography.bodyLarge)
            offer != null -> {
                Text(stringResource(R.string.settings_remove_ads_detail), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onBuy,
                    enabled = store.purchase !is PurchaseState.Verifying,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.settings_remove_ads_buy, offer.formattedPrice))
                }
            }
            else -> Text(stringResource(R.string.settings_store_unavailable), style = MaterialTheme.typography.bodyMedium)
        }
        when (val purchase = store.purchase) {
            PurchaseState.Verifying -> Text(stringResource(R.string.settings_purchase_verifying), style = MaterialTheme.typography.bodySmall)
            PurchaseState.Pending -> Text(stringResource(R.string.settings_purchase_pending), style = MaterialTheme.typography.bodySmall)
            is PurchaseState.Failed -> Text(stringResource(R.string.settings_purchase_failed, purchase.reason), style = MaterialTheme.typography.bodySmall)
            PurchaseState.Idle, PurchaseState.Verified -> Unit
        }
        if (!store.adsRemoved) {
            TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.settings_restore_purchases))
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    PbbTheme { SettingsScreen(settings = UserSettings(), onToggle = { _, _ -> }, onBack = {}) }
}
