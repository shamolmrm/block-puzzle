package com.puzzleblockblast.game.core.model

/** Locally persisted player preferences. Defaults are the first-launch values. */
data class UserSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    /** Anonymous usage analytics (Firebase Analytics when configured). */
    val shareUsageData: Boolean = true,
    /** Crash reports (Crashlytics when configured). */
    val shareCrashReports: Boolean = true,
)
