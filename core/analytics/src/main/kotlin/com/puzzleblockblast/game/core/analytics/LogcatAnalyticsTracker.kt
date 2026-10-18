package com.puzzleblockblast.game.core.analytics

import android.util.Log
import javax.inject.Inject

/** Phase 1 implementation: writes events to Logcat only. Nothing leaves the device. */
internal class LogcatAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun log(event: AnalyticsEvent) {
        Log.d(TAG, "${event.name} ${event.params}")
    }

    private companion object {
        const val TAG = "PbbAnalytics"
    }
}
