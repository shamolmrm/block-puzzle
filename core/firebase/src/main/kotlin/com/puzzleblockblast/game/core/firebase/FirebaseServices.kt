package com.puzzleblockblast.game.core.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.puzzleblockblast.game.core.analytics.AnalyticsEvent
import com.puzzleblockblast.game.core.analytics.AnalyticsTracker
import com.puzzleblockblast.game.core.analytics.CrashReporter
import com.puzzleblockblast.game.core.data.config.AppConfig
import com.puzzleblockblast.game.core.data.config.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Firebase is configured when the google-services plugin generated config and FirebaseApp started. */
object FirebaseStatus {
    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
}

internal class FirebaseAnalyticsTracker(private val analytics: FirebaseAnalytics) : AnalyticsTracker {
    override fun log(event: AnalyticsEvent) {
        val bundle = Bundle()
        event.params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putLong(key, if (value) 1 else 0)
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(event.name, bundle)
    }
}

internal class FirebaseCrashReporter(private val crashlytics: FirebaseCrashlytics) : CrashReporter {
    override fun recordNonFatal(error: Throwable, context: String) {
        crashlytics.log(context)
        crashlytics.recordException(error)
    }
}

/**
 * Remote config with bundled defaults. Values activated by the previous run are used for this run;
 * newly fetched values activate on the next start, so rules never change mid-game.
 */
internal class FirebaseConfigRepository(private val remote: FirebaseRemoteConfig) : ConfigRepository {
    private val state = MutableStateFlow(read())
    override val config: StateFlow<AppConfig> = state.asStateFlow()

    init {
        remote.setDefaultsAsync(AppConfig.DEFAULTS)
        // Fetch for next time; failures (offline) are fine, defaults and cached values apply.
        remote.fetch().addOnSuccessListener { remote.activate() }
    }

    private fun read(): AppConfig = AppConfig.from(
        dailyEnabled = remote.getValue(AppConfig.KEY_DAILY_ENABLED).takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }?.asBoolean(),
        classicHints = remote.getValue(AppConfig.KEY_CLASSIC_HINTS).takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }?.asLong(),
    )
}
