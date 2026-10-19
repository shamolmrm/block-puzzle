package com.puzzleblockblast.game.core.analytics

import android.util.Log
import javax.inject.Inject

/**
 * Reports problems the app recovered from (corrupt saves, rejected content…). Crashes themselves
 * are captured automatically by the Crashlytics implementation. Never pass personal data.
 */
interface CrashReporter {
    fun recordNonFatal(error: Throwable, context: String)
}

/** Used when Firebase isn't configured. */
class LogcatCrashReporter @Inject constructor() : CrashReporter {
    override fun recordNonFatal(error: Throwable, context: String) {
        Log.w("PbbCrash", context, error)
    }
}
