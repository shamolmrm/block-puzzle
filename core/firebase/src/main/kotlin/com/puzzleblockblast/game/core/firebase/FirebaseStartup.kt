package com.puzzleblockblast.game.core.firebase

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.puzzleblockblast.game.core.data.di.ApplicationScope
import com.puzzleblockblast.game.core.data.settings.SettingsRepository
import com.puzzleblockblast.game.core.data.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Called once from Application.onCreate. Does nothing Firebase-related when it isn't configured. */
@Singleton
class FirebaseStartup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        if (!FirebaseStatus.isConfigured(context)) return
        // App Check: Play Integrity in release builds, the debug provider in debug builds.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())

        // Collection starts disabled (manifest) and follows the player's privacy settings.
        val analytics = FirebaseAnalytics.getInstance(context)
        val crashlytics = FirebaseCrashlytics.getInstance()
        scope.launch {
            settings.settings.collect { s ->
                analytics.setAnalyticsCollectionEnabled(s.shareUsageData)
                // Google Consent Mode v2: analytics is never used for ads, so ad signals are always
                // denied; analytics storage follows the player's "Share usage statistics" choice.
                analytics.setConsent(
                    mapOf(
                        FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                            if (s.shareUsageData) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
                    ),
                )
                crashlytics.isCrashlyticsCollectionEnabled = s.shareCrashReports
            }
        }
        // Bring progress from other devices (or a previous install's backup) on every start.
        syncScheduler.request()
    }
}
